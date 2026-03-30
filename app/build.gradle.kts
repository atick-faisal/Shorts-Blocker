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

@file:Suppress("UnstableApiUsage")


import com.android.build.api.dsl.ApplicationExtension
import java.io.FileInputStream
import java.util.Properties

val keystorePropertiesFile: File = rootProject.file("keystore.properties")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.gms)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.dokka)
}

extensions.configure<ApplicationExtension> {
    // ... Application Version ...
    val majorUpdateVersion = 1
    val minorUpdateVersion = 0
    val patchVersion = 7

    val mVersionCode = majorUpdateVersion.times(10_000)
        .plus(minorUpdateVersion.times(100))
        .plus(patchVersion)

    val mVersionName = "$majorUpdateVersion.$minorUpdateVersion.$patchVersion"

    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36
        versionCode = mVersionCode
        versionName = mVersionName
        applicationId = "dev.atick.shorts"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                println(
                    "keystore.properties file not found. Using debug key. Read more here: " +
                            "https://atick.dev/Jetpack-Android-Starter/github",
                )
                signingConfigs.getByName("debug")

            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    namespace = "dev.atick.shorts"
}

// TODO: AGP 9 Migration - Custom Output Filename
// FIXME: Implement proper AGP 9 approach for custom APK naming
// Previous behavior: Jetpack_release_v{version}_{timestamp}.apk
// Current: Using default AGP naming scheme
//
// AGP 9 removed direct outputFile manipulation. Recommended approaches:
// 1. Use variant.artifacts.use() with SingleArtifact.APK
// 2. Customize via tasks.named<PackageApplication>("package{Variant}")
//
// References:
// - https://github.com/android/gradle-recipes (variantOutput recipe)
// - https://developer.android.com/build/extend-agp
// Tracking: GitHub Issue #579
androidComponents {
    onVariants { variant ->
        // Placeholder for future custom filename logic
        variant.outputs.forEach { output ->
            output.versionName.set("${variant.outputs.first().versionName.getOrElse("1.0.0")}")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.dataStore.core)
    implementation(libs.androidx.dataStore.preferences)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    dokkaPlugin(libs.dokka.android.plugin)
    dokkaPlugin(libs.dokka.mermaid.plugin)

    implementation(libs.timber.logging)

    implementation(libs.google.oss.licenses)
}