package com.example.health_assistant.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.health_assistant.R
import com.example.health_assistant.databinding.DashboardFragmentBinding

class DashboardFragment : Fragment() {
    private var _binding: DashboardFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var dashboardAdapter: DashboardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DashboardFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDashboard()
    }

    private fun setupDashboard() {
        // Initialize adapter with dashboard sections
        dashboardAdapter = DashboardAdapter(generateDashboardSections()) { section, action ->
            // Handle section actions here
            handleDashboardAction(section, action)
        }

        // Set up RecyclerView
        binding.dashboardRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = dashboardAdapter
        }
    }

    private fun generateDashboardSections(): List<DashboardSection> {
        return listOf(
            DashboardSection(
                id = "health_check",
                title = "Health Check",
                description = "Start a health check, input symptoms and get analysis",
                iconRes = R.drawable.ic_health_check, // You'll need to create these icons
                accentColorRes = R.color.health_check_accent,
                actions = listOf(
                    DashboardAction("start_check", "Start Check"),
                    DashboardAction("audio_input", "Audio Input"),
                    DashboardAction("wellness", "Wellness Questions"),
                    DashboardAction("review", "Review & Confirm"),
                    DashboardAction("analysis", "Analysis & Result"),
                    DashboardAction("feedback", "Feedback"),
                    DashboardAction("history", "View History")
                )
            ),
            DashboardSection(
                id = "prescription_upload",
                title = "Prescription Upload",
                description = "Upload, scan and manage your prescriptions",
                iconRes = R.drawable.ic_prescription, // You'll need to create these icons
                accentColorRes = R.color.prescription_upload_accent,
                actions = listOf(
                    DashboardAction("upload", "Upload Photo"),
                    DashboardAction("scan", "Scan"),
                    DashboardAction("edit", "Edit/Sign"),
                    DashboardAction("records", "Medical Records"),
                    DashboardAction("manage", "View/Edit/Delete")
                )
            ),
            DashboardSection(
                id = "weekly_report",
                title = "Weekly Health Report",
                description = "Track your health trends and get AI-powered insights",
                iconRes = R.drawable.ic_report, // You'll need to create these icons
                accentColorRes = R.color.weekly_report_accent,
                actions = listOf(
                    DashboardAction("view_trends", "View Weekly Trends"),
                    DashboardAction("ai_summary", "AI Summary"),
                    DashboardAction("export_format", "Select Export Format"),
                    DashboardAction("export_share", "Export/Share")
                )
            ),
            DashboardSection(
                id = "emergency",
                title = "Emergency",
                description = "Quick access to emergency features",
                iconRes = R.drawable.ic_emergency, // You'll need to create these icons
                accentColorRes = R.color.emergency_accent,
                actions = listOf(
                    DashboardAction("panic", "Panic Button"),
                    DashboardAction("alert", "Send Alert"),
                    DashboardAction("contacts", "Emergency Contacts"),
                    DashboardAction("self_help", "Self-Help")
                )
            ),
            DashboardSection(
                id = "chatbot",
                title = "AI Health Assistant",
                description = "Chat with our AI for health advice and guidance",
                iconRes = R.drawable.ic_chatbot, // You'll need to create these icons
                accentColorRes = R.color.chatbot_accent,
                actions = listOf(
                    DashboardAction("open_chat", "Open AI Assistant"),
                    DashboardAction("mood", "Mood Check-in"),
                    DashboardAction("exercises", "Guided Exercise"),
                    DashboardAction("advice", "Get Advice"),
                    DashboardAction("past_chats", "View Past Chats")
                )
            ),
            DashboardSection(
                id = "ar_guide",
                title = "AR Health Guide",
                description = "Augmented reality guides for health information",
                iconRes = R.drawable.ic_ar_guide, // You'll need to create these icons
                accentColorRes = R.color.ar_guide_accent,
                actions = listOf(
                    DashboardAction("launch_ar", "Launch AR"),
                    DashboardAction("health_tips", "View Health Tips"),
                    DashboardAction("first_aid", "First Aid Guide"),
                    DashboardAction("fallback", "2D Guide")
                )
            ),
            DashboardSection(
                id = "community",
                title = "Community",
                description = "Connect with others on similar health journeys",
                iconRes = R.drawable.ic_community, // You'll need to create these icons
                accentColorRes = R.color.community_accent,
                actions = listOf(
                    DashboardAction("join_group", "Join Group"),
                    DashboardAction("check_in", "Post Check-in"),
                    DashboardAction("challenges", "Challenges"),
                    DashboardAction("privacy", "Privacy Controls")
                )
            ),
            DashboardSection(
                id = "settings",
                title = "Settings & Profile",
                description = "Manage your account and app preferences",
                iconRes = R.drawable.ic_settings, // You'll need to create these icons
                accentColorRes = R.color.settings_accent,
                actions = listOf(
                    DashboardAction("edit_profile", "Edit Info"),
                    DashboardAction("permissions", "Permissions"),
                    DashboardAction("notifications", "Notification Preferences"),
                    DashboardAction("language", "Language"),
                    DashboardAction("data_export", "Data Export/Delete"),
                    DashboardAction("support", "App Info/Support")
                )
            ),
            DashboardSection(
                id = "help_feedback",
                title = "Help & Feedback",
                description = "Get assistance and provide app feedback",
                iconRes = R.drawable.ic_help, // You'll need to create these icons
                accentColorRes = R.color.help_feedback_accent,
                actions = listOf(
                    DashboardAction("submit", "Submit Feedback/Bug"),
                    DashboardAction("faq", "FAQ"),
                    DashboardAction("contact", "Contact Support")
                )
            )
        )
    }

    private fun handleDashboardAction(section: DashboardSection, action: DashboardAction) {
        // Handle actions based on section ID and action ID
        // This would navigate to the appropriate feature/screen
        when (section.id) {
            "health_check" -> handleHealthCheckAction(action.id)
            "prescription_upload" -> handlePrescriptionAction(action.id)
            "weekly_report" -> handleReportAction(action.id)
            "emergency" -> handleEmergencyAction(action.id)
            "chatbot" -> handleChatbotAction(action.id)
            "ar_guide" -> handleARGuideAction(action.id)
            "community" -> handleCommunityAction(action.id)
            "settings" -> handleSettingsAction(action.id)
            "help_feedback" -> handleHelpFeedbackAction(action.id)
        }
    }

    // Action handler methods for each section
    private fun handleHealthCheckAction(actionId: String) {
        when (actionId) {
            "start_check" -> {
                // Navigate to start health check
            }
            "audio_input" -> {
                // Navigate to audio input
            }
            // Handle other actions...
        }
    }

    private fun handlePrescriptionAction(actionId: String) {
        // Handle prescription upload actions
    }

    private fun handleReportAction(actionId: String) {
        // Handle weekly report actions
    }

    private fun handleEmergencyAction(actionId: String) {
        // Handle emergency actions
    }

    private fun handleChatbotAction(actionId: String) {
        // Handle chatbot actions
    }

    private fun handleARGuideAction(actionId: String) {
        // Handle AR guide actions
    }

    private fun handleCommunityAction(actionId: String) {
        // Handle community actions
    }

    private fun handleSettingsAction(actionId: String) {
        // Handle settings actions
    }

    private fun handleHelpFeedbackAction(actionId: String) {
        // Handle help & feedback actions
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}