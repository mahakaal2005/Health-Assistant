package com.example.health_assistant.dashboard

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

/**
 * Represents a section in the dashboard (e.g., Health Check, Prescription Upload)
 */
data class DashboardSection(
    val id: String,
    val title: String,
    val description: String,
    @DrawableRes val iconRes: Int,
    @ColorRes val accentColorRes: Int,
    val actions: List<DashboardAction>
)

/**
 * Represents an action within a dashboard section (e.g., "Start Check", "View History")
 */
data class DashboardAction(
    val id: String,
    val label: String
)