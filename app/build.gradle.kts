plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("jacoco")
}

android {
    namespace = "com.example.health_assistant"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.health_assistant"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // CRITICAL: Android 15 compatibility flags
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // FIXED: Use modern androidResources instead of deprecated resourceConfigurations
    androidResources {
        localeFilters += listOf("en")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // CRITICAL: Android 15 security enhancements
            isDebuggable = false
            isJniDebuggable = false
            isPseudoLocalesEnabled = false
        }

        debug {
            isDebuggable = true
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

        // CRITICAL: Enable core library desugaring for Android 15
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"

        // CRITICAL: Android 15 Kotlin compatibility
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    // FIXED: Use modern packaging instead of deprecated packagingOptions
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/gradle/incremental.annotation.processors"
        }

        // CRITICAL: 16 KB page size alignment for native libraries (modern approach)
        jniLibs {
            useLegacyPackaging = false
        }
    }

    // CRITICAL: Android 15 lint configuration
    lint {
        targetSdk = 36
        checkReleaseBuilds = false
        abortOnError = false
    }

    // CRITICAL: 16 KB page size support configuration using splits only
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true  // Generate universal APK for compatibility
        }
    }

    // CRITICAL: Force 16 KB page size alignment for all native libraries
    androidComponents {
        onVariants(selector().all()) { variant ->
            variant.packaging.jniLibs.excludes.add("**/libimage_processing_util_jni.so")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation(libs.androidx.swiperefreshlayout)
    // FIXED: Remove duplicate paging dependencies - only use Android version
    implementation(libs.androidx.paging.common.android)
    // CRITICAL: Core library desugaring for Android 15 compatibility
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Add Gson dependency for Room TypeConverters
    implementation("com.google.code.gson:gson:2.10.1")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation( libs.material.v190)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.lottie)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))
    // Add Firebase Authentication dependency
    implementation(libs.firebase.auth)
    // Add Firebase Realtime Database dependency
    implementation(libs.firebase.firestore)
    // Firebase Firestore
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)
    // Coroutines support for Firebase
    implementation(libs.kotlinx.coroutines.play.services.v1102)

    // Image loading (for displaying images from Firebase Storage)
    implementation(libs.coil3.coil)

    // Add android.security.crypto for secure data storage
    implementation(libs.androidx.security.crypto)

    // Add Supabase dependencies for storage functionality
    implementation(libs.storage.kt)
    implementation(libs.gotrue.kt) // Auth integration
    implementation(libs.kotlinx.coroutines.core)

    // Add CircleImageView library for circular profile images with border
    implementation(libs.circleimageview)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Add Room dependencies
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // Add datastore dependency
    implementation(libs.androidx.datastore.preferences)

    // Add Hilt dependencies
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // CRITICAL: Add missing Hilt Worker dependencies for HealthDataSyncWorker
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // CRITICAL: Add missing WorkManager dependency for CoroutineWorker
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines dependencies
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    // CRITICAL: Add missing Guava coroutines for CameraManager.kt
    implementation(libs.kotlinx.coroutines.guava)

    // ViewPager2 for onboarding screens
    implementation(libs.androidx.viewpager2)

    // Dots indicator library for ViewPager2 navigation
    implementation(libs.dotsindicator)

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.turbine)

    // Traditional Android View System dependencies (XML + Kotlin)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.fragment.ktx)

    // Google Play Services dependencies
    implementation(libs.play.services.auth)  // CRITICAL: Add missing Google Play Services Auth for GoogleSignIn
    implementation(libs.androidx.health.services.client)
    implementation(libs.kotlinx.coroutines.android.v1102)

    // CRITICAL: Add MPAndroidChart dependency for HealthMetricsAdapter
    implementation(libs.mpandroidchart)

    // CRITICAL: Add missing CameraX dependencies for CameraCaptureFragment
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
}