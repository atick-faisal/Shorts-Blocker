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

package dev.atick.shorts.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.atick.shorts.models.TrackedPackage

@Composable
fun TrackedPackagesSection(
    packages: List<TrackedPackage>,
    onPackageToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
        ) {
            Text(
                text = "Tracked Apps",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            packages.forEachIndexed { index, pkg ->
                PackageToggleItem(
                    trackedPackage = pkg,
                    onToggle = { enabled ->
                        onPackageToggle(pkg.packageName, enabled)
                    },
                )

                if (index < packages.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackedPackagesSectionPreview() {
    MaterialTheme {
        TrackedPackagesSection(
            packages = listOf(
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
            onPackageToggle = { _, _ -> },
        )
    }
}
