package de.viktor.doorlyapp.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PageViewModel : ViewModel() {

    private val _isRunning = MutableLiveData<Boolean>()
    val isRunning: LiveData<Boolean> = Transformations.map(_isRunning) {it}

    fun setIndex(isRunning: Boolean) {
        _isRunning.value = isRunning
    }
}