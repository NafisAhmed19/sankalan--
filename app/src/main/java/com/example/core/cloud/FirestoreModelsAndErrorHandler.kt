package com.example.core.cloud

import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write"),
}

fun handleFirestoreError(
  exception: Throwable,
  operationType: OperationType,
  path: String?,
): String {
  val causes = generateSequence(exception) { it.cause }
  val rootError =
    causes.firstOrNull { it is FirebaseFirestoreException }
      ?: causes.firstOrNull { it !is CancellationException }
      ?: exception

  val currentUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()

  val providerInfoList =
    currentUser?.providerData?.map { provider ->
      JSONObject().apply {
        put("providerId", provider.providerId)
        put("email", provider.email)
      }
    } ?: emptyList()

  val authInfoJson =
    JSONObject().apply {
      put("userId", currentUser?.uid)
      put("email", currentUser?.email)
      put("emailVerified", currentUser?.isEmailVerified)
      put("tenantId", currentUser?.tenantId)
      put("providerInfo", JSONArray(providerInfoList))
    }

  val errorInfoJson =
    JSONObject().apply {
      put("error", rootError.message ?: rootError.toString())
      put("operationType", operationType.value)
      put("path", path)
      put("authInfo", authInfoJson)
    }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", "Firestore Error: $jsonString")
  return jsonString
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
  val userId: String = "",
  val email: String = "",
  val displayName: String = "",
  val photoUrl: String? = null,
  val authProvider: String = "google",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class FeedbackCategory(val id: String) {
  GENERAL("general"),
  FEATURE("feature"),
  BUG("bug"),
  LOVE("love");

  companion object {
    fun fromId(raw: String?): FeedbackCategory =
      entries.firstOrNull { it.id == raw } ?: GENERAL
  }
}

@Keep
@IgnoreExtraProperties
data class FeedbackEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val authorName: String = "",
  val message: String = "",
  val category: String = FeedbackCategory.GENERAL.id,
  val rating: Long = 5L,
  val status: String = "published",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val safeCategory: FeedbackCategory
    get() = FeedbackCategory.fromId(category)

  val safeRatingInt: Int
    get() = rating.toInt().coerceIn(1, 5)
}
