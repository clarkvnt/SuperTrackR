package com.fittracker.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fittracker.service.LocationTrackingService
import com.fittracker.service.TrackingStats
import com.fittracker.utils.LocationPermissionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.location.Location

class LocationTrackingViewModel(application: Application) : AndroidViewModel(application) {
    private var locationService: LocationTrackingService? = null
    private var bound = false

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _trackingStats = MutableStateFlow<TrackingStats?>(null)
    val trackingStats: StateFlow<TrackingStats?> = _trackingStats.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationTrackingService.LocalBinder
            locationService = binder.getService()
            bound = true
            observeServiceState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            bound = false
        }
    }

    init {
        bindLocationService()
    }

    private fun bindLocationService() {
        Intent(getApplication(), LocationTrackingService::class.java).also { intent ->
            getApplication<Application>().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            locationService?.isTracking?.collect { isTracking ->
                _isTracking.value = isTracking
            }
        }

        viewModelScope.launch {
            locationService?.locationUpdates?.collect { locations ->
                _currentLocation.value = locations.firstOrNull()
                updateTrackingStats()
            }
        }
    }

    private fun updateTrackingStats() {
        locationService?.let { service ->
            _trackingStats.value = service.getTrackingStats()
        }
    }

    fun startTracking() {
        if (!LocationPermissionHandler.hasLocationPermission(getApplication())) {
            return
        }

        Intent(getApplication(), LocationTrackingService::class.java).also { intent ->
            intent.action = LocationTrackingService.ACTION_START_TRACKING
            getApplication<Application>().startService(intent)
        }
    }

    fun stopTracking() {
        Intent(getApplication(), LocationTrackingService::class.java).also { intent ->
            intent.action = LocationTrackingService.ACTION_STOP_TRACKING
            getApplication<Application>().startService(intent)
        }
    }

    fun updateLocation(location: Location) {
        viewModelScope.launch {
            _currentLocation.value = location
        }
    }

    fun updateTrackingStats(stats: TrackingStats) {
        viewModelScope.launch {
            _trackingStats.value = stats
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (bound) {
            getApplication<Application>().unbindService(connection)
            bound = false
        }
    }
} 