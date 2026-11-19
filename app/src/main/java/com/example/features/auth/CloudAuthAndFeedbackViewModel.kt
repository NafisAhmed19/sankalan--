package com.example.features.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.cloud.CloudAccountAndFeedbackRepository
import com.example.core.cloud.FeedbackCategory
import com.example.core.cloud.FeedbackEntry
import com.example.core.cloud.OperationType
import com.example.core.cloud.UserProfile
import com.example.core.cloud.handleFirestoreError
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CloudUiState<out T> {
  data object Loading : CloudUiState<Nothing>
  data class Success<T>(val data: T) : CloudUiState<T>
  data class Error(val message: String) : CloudUiState<Nothing>
}

class CloudAuthAndFeedbackViewModel(
  private val repository: CloudAccountAndFeedbackRepository,
  private val currentUserId: String,
  initialDisplayNameOverride: String? = null,
) : ViewModel() {

  val userProfileState: StateFlow<CloudUiState<UserProfile?>> =
    repository
      .observeUserProfile(currentUserId)
      .map<UserProfile?, CloudUiState<UserProfile?>> { CloudUiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        handleFirestoreError(rootError, OperationType.GET, "users/$currentUserId")
        Log.w(TAG, "Error observing user profile", rootError)
        emit(CloudUiState.Error(rootError.localizedMessage ?: "Failed to load profile"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CloudUiState.Loading,
      )

  val feedbackListState: StateFlow<CloudUiState<List<FeedbackEntry>>> =
    repository
      .observePublishedFeedback()
      .map<List<FeedbackEntry>, CloudUiState<List<FeedbackEntry>>> { CloudUiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        handleFirestoreError(rootError, OperationType.LIST, "feedback")
        Log.w(TAG, "Error observing community feedback", rootError)
        emit(CloudUiState.Error(rootError.localizedMessage ?: "Failed to observe feedback"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CloudUiState.Loading,
      )

  private val _isSavingProfile = MutableStateFlow(false)
  val isSavingProfile: StateFlow<Boolean> = _isSavingProfile.asStateFlow()

  private val _isSubmittingFeedback = MutableStateFlow(false)
  val isSubmittingFeedback: StateFlow<Boolean> = _isSubmittingFeedback.asStateFlow()

  private val _statusBanner = MutableStateFlow<Pair<Boolean, String>?>(null)
  val statusBanner: StateFlow<Pair<Boolean, String>?> = _statusBanner.asStateFlow()

  init {
    viewModelScope.launch {
      repository.ensureUserProfileExists(displayNameOverride = initialDisplayNameOverride)
    }
  }

  fun updateDisplayName(newName: String, successMsg: String, failureMsg: String) {
    if (_isSavingProfile.value) return
    viewModelScope.launch {
      _isSavingProfile.value = true
      val res = repository.updateDisplayName(newName)
      _isSavingProfile.value = false
      _statusBanner.value =
        if (res.isSuccess) true to successMsg
        else false to (res.exceptionOrNull()?.localizedMessage ?: failureMsg)
    }
  }

  fun submitFeedback(
    authorName: String,
    message: String,
    category: FeedbackCategory,
    rating: Int,
    successMsg: String,
    failureMsg: String,
    onSubmitted: () -> Unit = {},
  ) {
    if (_isSubmittingFeedback.value) return
    viewModelScope.launch {
      _isSubmittingFeedback.value = true
      val res = repository.submitFeedback(authorName, message, category, rating)
      _isSubmittingFeedback.value = false
      if (res.isSuccess) {
        _statusBanner.value = true to successMsg
        onSubmitted()
      } else {
        _statusBanner.value = false to (res.exceptionOrNull()?.localizedMessage ?: failureMsg)
      }
    }
  }

  fun deleteFeedback(feedbackId: String, failureMsg: String) {
    viewModelScope.launch {
      val res = repository.deleteFeedback(feedbackId)
      if (res.isFailure) {
        _statusBanner.value = false to (res.exceptionOrNull()?.localizedMessage ?: failureMsg)
      }
    }
  }

  fun clearBanner() {
    _statusBanner.value = null
  }

  private companion object {
    const val TAG = "CloudAuthFeedbackVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
