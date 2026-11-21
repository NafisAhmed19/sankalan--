package com.example.core.cloud

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Firestore repository backed strictly by the named applet database (`R.string.firestore_database_id`).
 * Manages authenticated user Gmail profiles (`/users/{uid}`) and community Settings feedback (`/feedback`).
 */
class CloudAccountAndFeedbackRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  fun observeUserProfile(userId: String): Flow<UserProfile?> {
    val activeUid = auth.currentUser?.uid
    if (userId.isBlank() || userId.contains('/') || activeUid == null || activeUid != userId) {
      return flowOf(null)
    }
    return db.collection(COLLECTION_USERS)
      .document(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
  }

  /**
   * Ensures the authenticated user's profile exists in Firestore `/users/{uid}` upon sign-in or account creation.
   */
  suspend fun ensureUserProfileExists(displayNameOverride: String? = null): Result<UserProfile> {
    var docPath = COLLECTION_USERS
    return try {
      val user = auth.currentUser ?: error("User must be signed in with Google before creating profile.")
      val uid = user.uid
      docPath = "$COLLECTION_USERS/$uid"
      val email = (user.email?.trim()?.takeIf { it.length in 3..160 }) ?: "user-$uid@gmail.com".take(160)
      val fallbackName = email.substringBefore('@').ifBlank { "Sankalan User" }
      val candidateName =
        (displayNameOverride?.trim()?.takeIf { it.isNotEmpty() }
          ?: user.displayName?.trim()?.takeIf { it.isNotEmpty() }
          ?: fallbackName).take(60)
      val photo = user.photoUrl?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.take(500)

      val docRef = db.collection(COLLECTION_USERS).document(uid)
      val existingSnap = docRef.get().await()
      if (!existingSnap.exists()) {
        val createPayload =
          buildMap<String, Any> {
            put("userId", uid)
            put("email", email)
            put("displayName", candidateName)
            if (photo != null) put("photoUrl", photo)
            put("authProvider", "google")
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
          }
        docRef.set(createPayload).await()
        Result.success(
          UserProfile(
            userId = uid,
            email = email,
            displayName = candidateName,
            photoUrl = photo,
            authProvider = "google",
          )
        )
      } else {
        val existingProfile =
          existingSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        val resolvedName =
          displayNameOverride?.trim()?.takeIf { it.isNotEmpty() }?.take(60)
            ?: existingProfile?.displayName?.takeIf { it.isNotBlank() }
            ?: candidateName
        val updatePayload =
          buildMap<String, Any> {
            put("email", email)
            put("displayName", resolvedName)
            if (photo != null) put("photoUrl", photo)
            put("updatedAt", FieldValue.serverTimestamp())
          }
        docRef.update(updatePayload).await()
        Result.success(
          (existingProfile ?: UserProfile(userId = uid)).copy(
            email = email,
            displayName = resolvedName,
            photoUrl = photo ?: existingProfile?.photoUrl,
          )
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateDisplayName(newDisplayName: String): Result<Unit> {
    var docPath = COLLECTION_USERS
    return try {
      val uid = requireUserId()
      val trimmed = newDisplayName.trim()
      require(trimmed.length in 1..60) { "Display name must be 1 to 60 characters." }
      docPath = "$COLLECTION_USERS/$uid"
      db.collection(COLLECTION_USERS)
        .document(uid)
        .update(
          mapOf(
            "displayName" to trimmed,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  /**
   * Streams community feedback items sorted newest-first client-side so no composite index is required,
   * while matching `allow list: if isSignedIn() && existing().status == 'published'` 1:1.
   */
  fun observePublishedFeedback(): Flow<List<FeedbackEntry>> {
    if (auth.currentUser == null) return flowOf(emptyList())
    return db.collection(COLLECTION_FEEDBACK)
      .whereEqualTo("status", STATUS_PUBLISHED)
      .limit(60)
      .snapshots()
      .map { querySnapshot ->
        querySnapshot
          .toObjects(FeedbackEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
  }

  suspend fun submitFeedback(
    authorName: String,
    message: String,
    category: FeedbackCategory,
    rating: Int,
  ): Result<Unit> {
    var docPath = COLLECTION_FEEDBACK
    return try {
      val uid = requireUserId()
      val safeAuthor =
        authorName.trim()
          .ifBlank { auth.currentUser?.displayName?.trim().orEmpty() }
          .ifBlank { auth.currentUser?.email?.substringBefore('@')?.trim().orEmpty() }
          .ifBlank { "Sankalan Reader" }
          .take(60)
      val safeMessage = message.trim()
      require(safeMessage.length in 1..500) { "Feedback message must be 1 to 500 characters." }
      val safeRating = rating.coerceIn(1, 5)

      val docRef = db.collection(COLLECTION_FEEDBACK).document()
      docPath = docRef.path
      val payload =
        mapOf(
          "userId" to uid,
          "authorName" to safeAuthor,
          "message" to safeMessage,
          "category" to category.id,
          "rating" to safeRating,
          "status" to STATUS_PUBLISHED,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteFeedback(feedbackId: String): Result<Unit> {
    var docPath = COLLECTION_FEEDBACK
    return try {
      requireUserId()
      val cleanId = feedbackId.trim()
      require(cleanId.isNotBlank() && !cleanId.contains('/')) { "Invalid feedback ID." }
      docPath = "$COLLECTION_FEEDBACK/$cleanId"
      db.collection(COLLECTION_FEEDBACK).document(cleanId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  companion object {
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_FEEDBACK = "feedback"
    private const val STATUS_PUBLISHED = "published"
  }
}
