package com.example.health_assistant

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * Application class for initializing Firebase when the app starts
 */
class HealthAssistantApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}