plugins {

    alias(libs.plugins.android.application)

    alias(libs.plugins.kotlin.android)

    id("com.google.gms.google-services")

    id("com.google.devtools.ksp")

    id("com.google.dagger.hilt.android")



}



android {

    namespace = "com.example.health_assistant"

    compileSdk = 35



    defaultConfig {

        applicationId = "com.example.health_assistant"

        minSdk = 24

        targetSdk = 35

        versionCode = 1

        versionName = "1.0"



        testInstrumentationRunner = "android.test.runner.AndroidJUnitRunner"

    }



    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(

                getDefaultProguardFile("proguard-android-optimize.txt"),

                "proguard-rules.pro"

            )

        }

    }

    compileOptions {

        sourceCompatibility = JavaVersion.VERSION_11

        targetCompatibility = JavaVersion.VERSION_11

    }

    kotlinOptions {

        jvmTarget = "11"

    }



    buildFeatures {

        viewBinding = true

        dataBinding =true

    }

}



dependencies {



    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.appcompat)

    implementation(libs.material)

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



    //

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)

    androidTestImplementation(libs.androidx.espresso.core)



    // Add Room dependencies

    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.room.runtime)

    implementation(libs.androidx.room.ktx)





    // Add datastore dependency

    implementation (libs.androidx.datastore.preferences)



    // Add Hilt dependencies

    implementation( libs.hilt.android)

    ksp (libs.hilt.compiler)



    // Coroutines dependencies

    implementation (libs.kotlinx.coroutines.android)

    implementation(libs.kotlinx.coroutines.play.services)



    // ViewPager2 for onboarding screens
    implementation(libs.androidx.viewpager2)

    // Dots indicator library for ViewPager2 navigation
    implementation(libs.dotsindicator)
}