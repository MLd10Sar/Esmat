package com.example.roznamcha.ui.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.roznamcha.network.ActivationRequest
import com.example.roznamcha.network.ApiClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

// Helper class for handling one-time events like navigation or toasts
class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set
    fun getContentIfNotHandled(): T? = if (hasBeenHandled) null else {
        hasBeenHandled = true
        content
    }
}

class ActivationViewModel(application: Application) : AndroidViewModel(application) {

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Use the Event wrapper for one-time results
    private val _activationResultEvent = MutableLiveData<Event<Pair<Boolean, String?>>>()
    val activationResultEvent: LiveData<Event<Pair<Boolean, String?>>> = _activationResultEvent

    fun attemptActivation(enteredCode: String) {
        if (enteredCode.isBlank()) {
            _activationResultEvent.value = Event(Pair(false, "Code cannot be empty"))
            return
        }
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val request = ActivationRequest(activationCode = enteredCode)
                val response = ApiClient.apiService.verifyActivationCode(request)

                if (response.isSuccessful && response.body()?.status == "success") {
                    _activationResultEvent.postValue(Event(Pair(true, response.body()?.message)))
                } else {
                    val errorMsg = response.body()?.message ?: "Activation failed (Code: ${response.code()})"
                    _activationResultEvent.postValue(Event(Pair(false, errorMsg)))
                }
            } catch (e: IOException) { // No internet, DNS error
                Log.e("ActivationVM", "Network error", e)
                _activationResultEvent.postValue(Event(Pair(false, "Network error. Please check connection.")))
            } catch (e: HttpException) { // Server returned 4xx or 5xx
                Log.e("ActivationVM", "HTTP error", e)
                _activationResultEvent.postValue(Event(Pair(false, "Server error (Code: ${e.code()}).")))
            } catch (e: Exception) { // Other errors (e.g., JSON parsing)
                Log.e("ActivationVM", "Unexpected error", e)
                _activationResultEvent.postValue(Event(Pair(false, "An unexpected error occurred.")))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}