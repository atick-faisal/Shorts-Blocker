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

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import timber.log.Timber

object AccessibilityServiceManager {

    /**
     * Check if the accessibility service is enabled for this app.
     *
     * @param context Application context
     * @param serviceName Fully qualified service name (e.g., "dev.atick.shorts/.services.ShortsAccessibilityService")
     * @return true if accessibility service is enabled, false otherwise
     */
    fun isAccessibilityServiceEnabled(context: Context, serviceName: String): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )

        Timber.d("Checking accessibility service: $serviceName")
        Timber.v("Enabled services: $enabledServices")

        if (enabledServices.isNullOrEmpty()) {
            Timber.d("No accessibility services enabled")
            return false
        }

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(serviceName, ignoreCase = true)) {
                Timber.i("Accessibility service is enabled: $serviceName")
                return true
            }
        }

        Timber.d("Accessibility service not found in enabled services")
        return false
    }

    /**
     * Open the accessibility settings page.
     *
     * @param context Context to start the activity
     */
    fun openAccessibilitySettings(context: Context) {
        Timber.i("Opening accessibility settings")
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
            Timber.d("Accessibility settings opened successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to open accessibility settings")
        }
    }
}
