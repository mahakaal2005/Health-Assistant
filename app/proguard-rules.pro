# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# CRITICAL: 16 KB page size compatibility rules for Android 15+
# Keep native methods to prevent obfuscation issues with aligned libraries
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep JNI native libraries aligned properly
-keep class **.R$* {
    *;
}

# Image processing libraries alignment (Coil, CameraX, etc.)
-keep class coil.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Keep Android components for proper functionality
-keep class androidx.** { *; }
-keep class com.google.android.material.** { *; }

# Firebase and Google Play Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Health Assistant app specific rules
-keep class com.example.health_assistant.** { *; }

# For release builds, allow some optimization while preserving functionality
-dontwarn java.lang.invoke.*
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**