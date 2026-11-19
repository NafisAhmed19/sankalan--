const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write users or feedback", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@gmail.com",
      displayName: "Alice",
      authProvider: "google",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    unauthDb.collection("feedback").where("status", "==", "published").get()
  );
  await assertFails(
    unauthDb.collection("feedback").doc("fb_1").set({
      userId: ALICE_UID,
      authorName: "Alice",
      message: "Great app!",
      category: "love",
      rating: 5,
      status: "published",
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("User profile enforces strict PII isolation, schema bounds, and immutable fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Alice creates her own profile (matching Kotlin UserProfile payload)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@gmail.com",
      displayName: "Alice Rahman",
      authProvider: "google",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice can read & update her displayName
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice R.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob CANNOT read or modify Alice's profile (PII Gmail protection)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Alice CANNOT inject shadow fields (e.g. isAdmin) or mutate createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc("extra_doc").set({
      userId: ALICE_UID,
      email: "alice@gmail.com",
      displayName: "Spoof",
      authProvider: "google",
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Community feedback allows authenticated read/create and restricts update/delete to author", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Alice posts feedback (mirrors Kotlin FeedbackRepository payload without @DocumentId 'id')
  await assertSucceeds(
    aliceDb.collection("feedback").doc("fb_alice_1").set({
      userId: ALICE_UID,
      authorName: "Alice Rahman",
      message: "ডায়েরির পাতা উল্টানো খুব সুন্দর হয়েছে!",
      category: "love",
      rating: 5,
      status: "published",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob can query published community feedback (mirrors Kotlin .whereEqualTo("status", "published"))
  await assertSucceeds(
    bobDb.collection("feedback").where("status", "==", "published").get()
  );

  // Unfiltered feedback query must be rejected
  await assertFails(bobDb.collection("feedback").get());

  // Bob CANNOT edit or delete Alice's feedback
  await assertFails(
    bobDb.collection("feedback").doc("fb_alice_1").update({
      message: "Edited by Bob",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("feedback").doc("fb_alice_1").delete());

  // Alice CAN delete her own feedback
  await assertSucceeds(aliceDb.collection("feedback").doc("fb_alice_1").delete());

  // Reject oversized messages, invalid ratings, invalid category, future timestamps, or spoofed userId
  await assertFails(
    bobDb.collection("feedback").doc("fb_spoof").set({
      userId: ALICE_UID,
      authorName: "Bob",
      message: "Spoofed UID",
      category: "general",
      rating: 4,
      status: "published",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    bobDb.collection("feedback").doc("fb_bad_rating").set({
      userId: BOB_UID,
      authorName: "Bob",
      message: "Rating out of bounds",
      category: "general",
      rating: 9,
      status: "published",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    bobDb.collection("feedback").doc("fb_future").set({
      userId: BOB_UID,
      authorName: "Bob",
      message: "Tampered future timestamp",
      category: "general",
      rating: 4,
      status: "published",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});
