/*
 * Copyright 2025 Atick Faisal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.atick.shorts.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.atick.shorts.models.TrackedPackage
import dev.atick.shorts.utils.AccessibilityServiceManager
import dev.atick.shorts.utils.UserPreferencesProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

data class ServiceState(
    val isGranted: Boolean = false,
    val isChecking: Boolean = false,
    val trackedPackages: List<TrackedPackage> = emptyList(),
)

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {

    companion object {
        private const val SERVICE_NAME: String =
            "dev.atick.shorts/dev.atick.shorts.services.ShortFormContentBlockerService"
    }

    private val userPreferencesProvider = UserPreferencesProvider(application)

    private val _serviceState = MutableStateFlow(ServiceState())
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    private var isMonitoring = false

    init {
        Timber.d("MainViewModel initialized")
        initializePreferences()
        checkPermission()
        observeTrackedPackages()
    }

    private fun initializePreferences() {
        viewModelScope.launch {
            userPreferencesProvider.initializeDefaultPackages()
        }
    }

    private fun observeTrackedPackages() {
        viewModelScope.launch {
            userPreferencesProvider.getTrackedPackagesWithStatus().collect { packages ->
                Timber.d("Tracked packages updated: ${packages.filter { it.isEnabled }.map { it.displayName }}")
                _serviceState.value = _serviceState.value.copy(trackedPackages = packages)
            }
        }
    }

    /**
     * Check accessibility service status.
     * This is called automatically on lifecycle resume and periodically.
     */
    fun checkPermission() {
        viewModelScope.launch {
            Timber.d("Checking accessibility service status")
            _serviceState.value = _serviceState.value.copy(isChecking = true)

            val isGranted = AccessibilityServiceManager.isAccessibilityServiceEnabled(
                getApplication(),
                SERVICE_NAME,
            )

            _serviceState.value = ServiceState(
                isGranted = isGranted,
                isChecking = false,
            )

            Timber.i("Service check complete: isGranted=$isGranted")
        }
    }

    /**
     * Open Android accessibility settings page.
     * Automatically starts monitoring for service status changes.
     */
    fun openAccessibilitySettings() {
        Timber.d("Opening accessibility settings")
        AccessibilityServiceManager.openAccessibilitySettings(getApplication())
        startMonitoring()
    }

    /**
     * Start monitoring permission changes when user goes to settings.
     * Checks every 1 second while monitoring is active.
     */
    private fun startMonitoring() {
        if (isMonitoring) {
            Timber.v("Already monitoring permission changes")
            return
        }

        isMonitoring = true
        Timber.d("Starting permission monitoring (1s interval)")

        viewModelScope.launch {
            while (isActive && isMonitoring) {
                delay(1000L)
                checkPermission()

                // Stop monitoring if service is enabled
                if (_serviceState.value.isGranted) {
                    Timber.i("Permission granted - stopping monitoring")
                    stopMonitoring()
                    break
                }
            }
        }
    }

    /**
     * Stop monitoring permission changes.
     */
    private fun stopMonitoring() {
        Timber.d("Stopping permission monitoring")
        isMonitoring = false
    }

    /**
     * Called when app resumes from background.
     * Performs a fresh permission check.
     */
    fun onResume() {
        Timber.d("ViewModel onResume - checking permission")
        checkPermission()
    }

    /**
     * Toggle tracking for a specific package.
     */
    fun togglePackageTracking(packageName: String, enabled: Boolean) {
        Timber.i("Toggling package tracking: $packageName -> $enabled")
        viewModelScope.launch {
            userPreferencesProvider.togglePackage(packageName, enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
        Timber.d("MainViewModel cleared")
    }
}
