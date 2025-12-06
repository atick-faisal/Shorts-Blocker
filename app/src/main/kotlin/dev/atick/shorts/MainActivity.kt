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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.atick.shorts.ui.screens.MainScreen
import dev.atick.shorts.ui.screens.OnboardingScreen
import dev.atick.shorts.ui.screens.ProminentDisclosureScreen
import dev.atick.shorts.ui.theme.ShortsBlockerTheme
import dev.atick.shorts.ui.viewmodels.MainViewModel
import dev.atick.shorts.ui.viewmodels.OnboardingViewModel
import timber.log.Timber

/**
 * Main activity that hosts the app's UI.
 *
 * Displays the main screen where users can enable the accessibility service
 * and manage which apps to track for short-form content blocking.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate")

        setContent {
            ShortsBlockerTheme {
                val onboardingViewModel: OnboardingViewModel = viewModel()
                val mainViewModel: MainViewModel = viewModel()
                val context = LocalContext.current

                val onboardingState by onboardingViewModel.state.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when {
                        !onboardingState.isOnboardingCompleted -> {
                            OnboardingScreen(
                                onComplete = {
                                    onboardingViewModel.completeOnboarding()
                                },
                            )
                        }

                        onboardingState.showDisclosure -> {
                            ProminentDisclosureScreen(
                                onAgree = {
                                    mainViewModel.acceptDisclosure(context)
                                    onboardingViewModel.hideDisclosure()
                                },
                                onCancel = {
                                    onboardingViewModel.hideDisclosure()
                                },
                            )
                        }

                        else -> {
                            MainScreen(viewModel = mainViewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("MainActivity onResume")
    }
}
