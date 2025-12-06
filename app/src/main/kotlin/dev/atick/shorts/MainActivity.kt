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

package dev.atick.shorts

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.atick.shorts.ui.screens.MainScreenContent
import dev.atick.shorts.ui.theme.ShortsBlockerTheme
import dev.atick.shorts.ui.viewmodels.MainViewModel
import timber.log.Timber

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate")

        val enabledServices = Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )

        Timber.d("---------------- Enabled Services --------------")
        Timber.d(enabledServices)

        setContent {
            ShortsBlockerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScreenWithLifecycle()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("MainActivity onResume")
    }
}

/**
 * Wrapper that handles lifecycle events for the main screen.
 * This ensures service status is rechecked when the app resumes.
 */
@Composable
private fun MainScreenWithLifecycle() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: MainViewModel = viewModel()
    val serviceState by viewModel.serviceState.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Timber.d("Lifecycle ON_RESUME - checking service status")
                    viewModel.onResume()
                }

                Lifecycle.Event.ON_PAUSE -> {
                    Timber.d("Lifecycle ON_PAUSE")
                }

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            Timber.d("Removing lifecycle observer")
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    MainScreenContent(
        isPermissionGranted = serviceState.isGranted,
        trackedPackages = serviceState.trackedPackages,
        onOpenSettings = { viewModel.openAccessibilitySettings() },
        onPackageToggle = { packageName, enabled ->
            viewModel.togglePackageTracking(packageName, enabled)
        },
    )
}
