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

package dev.atick.shorts.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.atick.shorts.models.TrackedPackage
import dev.atick.shorts.ui.components.TrackedPackagesSection
import dev.atick.shorts.ui.viewmodels.MainViewModel
import timber.log.Timber

/**
 * Main screen with ViewModel integration.
 *
 * This is the primary entry point for the app UI. Observes lifecycle events
 * to check accessibility service status when the screen resumes.
 *
 * @param viewModel ViewModel managing service state and user preferences
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val serviceState by viewModel.serviceState.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Timber.d("Lifecycle ON_RESUME - checking service status")
                    viewModel.onResume(context)
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

    if (serviceState.showDisclosure) {
        ProminentDisclosureScreen(
            onAgree = { viewModel.acceptDisclosure(context) },
            onCancel = { viewModel.hideDisclosure() },
        )
    } else {
        MainScreenContent(
            isPermissionGranted = serviceState.isGranted,
            trackedPackages = serviceState.trackedPackages,
            onOpenSettings = { viewModel.showDisclosure() },
            onPackageToggle = { packageName, enabled ->
                viewModel.togglePackageTracking(packageName, enabled)
            },
        )
    }
}

/**
 * Pure presentation layer for main screen.
 *
 * Use this for testing or when you want to manage state externally.
 * Shows either service active status or setup instructions based on permission state.
 *
 * @param isPermissionGranted Whether accessibility service permission is granted
 * @param modifier Modifier for the root composable
 * @param trackedPackages List of packages with their enabled status
 * @param onOpenSettings Callback when user taps to manage accessibility permission
 * @param onPackageToggle Callback when user toggles package blocking (packageName, enabled)
 */
@Composable
fun MainScreenContent(
    isPermissionGranted: Boolean,
    modifier: Modifier = Modifier,
    trackedPackages: List<TrackedPackage> = emptyList(),
    onOpenSettings: () -> Unit = {},
    onPackageToggle: (String, Boolean) -> Unit = { _, _ -> },
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (isPermissionGranted) {
                ServiceActiveContent(
                    trackedPackages = trackedPackages,
                    onPackageToggle = onPackageToggle,
                    onManagePermission = onOpenSettings,
                )
            } else {
                SetupRequiredContent(
                    onOpenSettings = onOpenSettings,
                )
            }
        }
    }
}

/**
 * Displays the active service status with tracked packages.
 *
 * Shows an animated loading indicator with check icon and a list
 * of tracked apps that users can toggle on/off.
 *
 * @param trackedPackages List of packages with their enabled status
 * @param onPackageToggle Callback when user toggles package blocking
 * @param onManagePermission Callback when user wants to manage accessibility permission
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ServiceActiveContent(
    trackedPackages: List<TrackedPackage> = emptyList(),
    onPackageToggle: (String, Boolean) -> Unit = { _, _ -> },
    onManagePermission: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Material 3 Expressive LoadingIndicator with check icon
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Material 3 LoadingIndicator (morphing shapes animation)
                LoadingIndicator(
                    modifier = Modifier.size(200.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                )

                // Icon container in the center
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Active",
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Service Active",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Content blocker is running",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    InfoRow(
                        title = "Status",
                        value = "Enabled",
                        valueColor = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        title = "Tracked Apps",
                        value = trackedPackages.count { it.isEnabled }.toString(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TrackedPackagesSection(
                packages = trackedPackages,
                onPackageToggle = onPackageToggle,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onManagePermission,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Manage Accessibility Permission",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Displays setup instructions when accessibility service is not enabled.
 *
 * Shows step-by-step instructions and a button to open accessibility settings.
 *
 * @param onOpenSettings Callback when user taps to open accessibility settings
 */
@Composable
private fun SetupRequiredContent(
    onOpenSettings: () -> Unit,
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 4.dp,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Permission Required",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Setup Required",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enable accessibility service to block short-form content automatically",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    Text(
                        text = "How to enable:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    InstructionStep(
                        number = "1",
                        text = "Tap the button below to open settings",
                    )
                    InstructionStep(
                        number = "2",
                        text = "Find the app in the list",
                    )
                    InstructionStep(
                        number = "3",
                        text = "Toggle the switch to enable",
                    )
                    InstructionStep(
                        number = "4",
                        text = "Return to this app",
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Open Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Displays a title-value pair in the info card.
 *
 * @param title The label text
 * @param value The value text
 * @param valueColor Color for the value text
 */
@Composable
private fun InfoRow(
    title: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
        )
    }
}

/**
 * Displays a numbered instruction step.
 *
 * @param number Step number as a string
 * @param text Instruction text
 */
@Composable
private fun InstructionStep(
    number: String,
    text: String,
) {
    Column(
        modifier = Modifier.padding(vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 2.dp,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, name = "Service Active")
@Composable
private fun ServiceActivePreview() {
    MaterialTheme {
        MainScreenContent(
            isPermissionGranted = true,
            trackedPackages = listOf(
                TrackedPackage(
                    packageName = "com.google.android.youtube",
                    displayName = "YouTube",
                    description = "Block YouTube Shorts",
                    isEnabled = true,
                ),
                TrackedPackage(
                    packageName = "com.instagram.android",
                    displayName = "Instagram",
                    description = "Block Instagram Reels",
                    isEnabled = false,
                ),
            ),
        )
    }
}

@Preview(showBackground = true, name = "Setup Required")
@Composable
private fun SetupRequiredPreview() {
    MaterialTheme {
        MainScreenContent(
            isPermissionGranted = false,
        )
    }
}
