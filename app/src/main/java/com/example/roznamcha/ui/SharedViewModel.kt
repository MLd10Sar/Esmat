package com.example.roznamcha.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.roznamcha.utils.Event

class SharedViewModel : ViewModel() {

    private val _navigateToActivationEvent = MutableLiveData<Event<Unit>>()
    val navigateToActivationEvent: LiveData<Event<Unit>> = _navigateToActivationEvent

    fun requestNavigationToActivation() {
        _navigateToActivationEvent.value = Event(Unit)
    }
}