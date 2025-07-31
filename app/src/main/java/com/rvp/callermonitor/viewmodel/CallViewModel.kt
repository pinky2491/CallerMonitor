package com.rvp.callermonitor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvp.callermonitor.model.CallerInfo
import com.rvp.callermonitor.model.UnknownCallLog
import com.rvp.callermonitor.repository.CallerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.rvp.callermonitor.R

/**
 * ViewModel for handling call events and exposing caller info.
 */
@HiltViewModel
class CallViewModel @Inject constructor(
    private val callerRepository: CallerRepository
) : ViewModel() {
    private val _unknownCaller = MutableLiveData<CallerInfo?>()
    val unknownCaller: LiveData<CallerInfo?> = _unknownCaller

    /**
     * Checks if the incoming number is in contacts and updates LiveData. Logs unknown calls.
     */
    fun onIncomingCall(phoneNumber: String) {
        Log.d("CallViewModel", "Processing incoming call: $phoneNumber")
        viewModelScope.launch(Dispatchers.IO) {
            val isInContacts = callerRepository.isNumberInContacts(phoneNumber)
            Log.d("CallViewModel", "Number $phoneNumber is in contacts: $isInContacts")
            
            if (!isInContacts) {
                Log.d("CallViewModel", "Setting unknown caller: $phoneNumber")
                _unknownCaller.postValue(CallerInfo(phoneNumber, false))
                val log = UnknownCallLog(phoneNumber = phoneNumber, timestamp = System.currentTimeMillis())
                callerRepository.insertUnknownCallLog(log)
                Log.d("CallViewModel", "Logged unknown call for: $phoneNumber")
            } else {
                _unknownCaller.postValue(null)
            }
        }
    }

    suspend fun insertUnknownCallLog(phoneNumber: String, timestamp: Long) {
        val log = com.rvp.callermonitor.model.UnknownCallLog(phoneNumber = phoneNumber, timestamp = timestamp)
        callerRepository.insertUnknownCallLog(log)
    }

    // Expose unknown call logs as StateFlow for UI
    private val _unknownCallLogs = MutableStateFlow<List<UnknownCallLog>>(emptyList())
    val unknownCallLogs: StateFlow<List<UnknownCallLog>> = _unknownCallLogs.asStateFlow()

    fun deleteUnknownCallLog(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            callerRepository.deleteUnknownCallLog(id)
        }
    }

    fun deleteAllUnknownCallLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            callerRepository.deleteAllUnknownCallLogs()
        }
    }

    fun blockNumberLocally(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            callerRepository.blockNumberLocally(phoneNumber)
        }
    }
    suspend fun isNumberBlockedLocally(phoneNumber: String): Boolean = callerRepository.isNumberBlockedLocally(phoneNumber)
    fun unblockNumberLocally(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            callerRepository.unblockNumberLocally(phoneNumber)
        }
    }

    suspend fun isNumberSpam(phoneNumber: String): Boolean = callerRepository.isNumberSpam(phoneNumber)
    fun reportNumberAsSpam(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            callerRepository.reportNumberAsSpam(phoneNumber)
        }
    }

    init {
        viewModelScope.launch {
            callerRepository.getUnknownCallLogs().collectLatest {
                Log.d("CallViewModel", "Unknown call logs updated: ${it.size} logs")
                _unknownCallLogs.value = it
            }
        }
    }
    
    // Import all unknown call logs from device call history
    suspend fun importAllUnknownCallLogs(): Int {
        Log.d("CallViewModel", "Starting import of all unknown call logs from device")
        return callerRepository.importAllUnknownCallLogs()
    }
} 