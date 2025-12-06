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
import dev.atick.shorts.utils.AccessibilityPermissionManager
import dev.atick.shorts.utils.UserPreferencesProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

data class PermissionState(
    val isGranted: Boolean = false,
    val isChecking: Boolean = false,
    val trackedPackages: List<TrackedPackage> = emptyList(),
)

class AccessibilityPermissionViewModel(
    application: Application,
) : AndroidViewModel(application) {

    companion object {
        private const val SERVICE_NAME: String =
            "dev.atick.shorts/dev.atick.shorts.services.ShortsAccessibilityService"
    }

    private val userPreferencesProvider = UserPreferencesProvider(application)

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    private var isMonitoring = false

    init {
        Timber.d("AccessibilityPermissionViewModel initialized")
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
                _permissionState.value = _permissionState.value.copy(trackedPackages = packages)
            }
        }
    }

    /**
     * Check accessibility permission status.
     * This is called automatically on lifecycle resume and periodically.
     */
    fun checkPermission() {
        viewModelScope.launch {
            Timber.d("Checking accessibility permission")
            _permissionState.value = _permissionState.value.copy(isChecking = true)

            val isGranted = AccessibilityPermissionManager.isAccessibilityServiceEnabled(
                getApplication(),
                SERVICE_NAME,
            )

            _permissionState.value = PermissionState(
                isGranted = isGranted,
                isChecking = false,
            )

            Timber.i("Permission check complete: isGranted=$isGranted")
        }
    }

    /**
     * Open Android accessibility settings page.
     * Automatically starts monitoring for permission changes.
     */
    fun openAccessibilitySettings() {
        Timber.d("Opening accessibility settings from ViewModel")
        AccessibilityPermissionManager.openAccessibilitySettings(getApplication())
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

                // Stop monitoring if permission is granted
                if (_permissionState.value.isGranted) {
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
        Timber.d("AccessibilityPermissionViewModel cleared")
    }
}