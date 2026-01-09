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

package dev.atick.shorts.utils

import dev.atick.shorts.models.TrackedPackage

/**
 * Constants for supported app packages and their default configurations.
 */
object PackageConstants {
    /** YouTube app package identifier */
    const val YOUTUBE_PACKAGE = "com.google.android.youtube"

    /** Instagram app package identifier */
    const val INSTAGRAM_PACKAGE = "com.instagram.android"

    /** List of all available packages that can be tracked */
    val AVAILABLE_PACKAGES = listOf(
        TrackedPackage(
            packageName = YOUTUBE_PACKAGE,
            displayName = "YouTube",
            description = "Block YouTube Shorts",
            isEnabled = false,
        ),
        TrackedPackage(
            packageName = INSTAGRAM_PACKAGE,
            displayName = "Instagram",
            description = "Block Instagram Reels",
            isEnabled = false,
        ),
    )

    /** List of package names that are enabled by default */
    val DEFAULT_ENABLED_PACKAGES = AVAILABLE_PACKAGES
        .filter { it.isEnabled }
        .map { it.packageName }
}
