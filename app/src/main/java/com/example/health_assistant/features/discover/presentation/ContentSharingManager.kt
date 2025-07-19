package com.example.health_assistant.features.discover.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.health_assistant.R
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manager class for handling content sharing functionality
 * Supports various sharing methods including social media, email, and clipboard
 */
class ContentSharingManager(private val context: Context) {

    private val deepLinkManager = DeepLinkManager(context)

    companion object {
        private const val APP_NAME = "Health Assistant"
        private const val APP_URL = "https://healthassistant.app"
    }

    /**
     * Share content using Android's native share intent
     */
    fun shareContent(content: DiscoverContent) {
        val shareText = formatContentForSharing(content)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, getShareSubject(content))
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share via")
        if (shareIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooserIntent)
        } else {
            Toast.makeText(context, "No sharing apps available", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share content specifically for social media platforms
     */
    fun shareToSocialMedia(content: DiscoverContent, platform: SocialPlatform) {
        val shareText = formatContentForSocialMedia(content, platform)
        val intent = when (platform) {
            SocialPlatform.TWITTER -> createTwitterIntent(shareText)
            SocialPlatform.FACEBOOK -> createFacebookIntent(shareText)
            SocialPlatform.LINKEDIN -> createLinkedInIntent(shareText)
            SocialPlatform.WHATSAPP -> createWhatsAppIntent(shareText)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to generic sharing if specific app not available
            shareContent(content)
        }
    }

    /**
     * Share content via email with formatted content and citations
     */
    fun shareViaEmail(content: DiscoverContent, recipientEmail: String? = null) {
        val emailSubject = getEmailSubject(content)
        val emailBody = formatContentForEmail(content)
        
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, emailSubject)
            putExtra(Intent.EXTRA_TEXT, emailBody)
            recipientEmail?.let { 
                putExtra(Intent.EXTRA_EMAIL, arrayOf(it))
            }
        }

        if (emailIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(Intent.createChooser(emailIntent, "Send email"))
        } else {
            Toast.makeText(context, "No email app available", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copy content to clipboard with proper formatting
     */
    fun copyToClipboard(content: DiscoverContent, copyType: ClipboardCopyType = ClipboardCopyType.FULL_CONTENT) {
        val clipboardManager = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        val clipText = when (copyType) {
            ClipboardCopyType.TITLE_ONLY -> content.title
            ClipboardCopyType.SUMMARY_ONLY -> content.getContentSummary()
            ClipboardCopyType.QUOTE -> formatContentAsQuote(content)
            ClipboardCopyType.CITATION -> formatContentCitation(content)
            ClipboardCopyType.FULL_CONTENT -> formatContentForSharing(content)
        }

        val clipData = ClipData.newPlainText("Health Content", clipText)
        clipboardManager?.setPrimaryClip(clipData)
        
        val message = when (copyType) {
            ClipboardCopyType.TITLE_ONLY -> "Title copied to clipboard"
            ClipboardCopyType.SUMMARY_ONLY -> "Summary copied to clipboard"
            ClipboardCopyType.QUOTE -> "Quote copied to clipboard"
            ClipboardCopyType.CITATION -> "Citation copied to clipboard"
            ClipboardCopyType.FULL_CONTENT -> "Content copied to clipboard"
        }
        
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Create a shareable deep link for the content
     */
    fun createShareableLink(content: DiscoverContent): String {
        return deepLinkManager.createUniversalLink(content)
    }

    /**
     * Format content for general sharing
     */
    private fun formatContentForSharing(content: DiscoverContent): String {
        return when (content) {
            is DiscoverContent.Article -> formatArticleForSharing(content)
            is DiscoverContent.News -> formatNewsForSharing(content)
            is DiscoverContent.Video -> formatVideoForSharing(content)
        }
    }

    /**
     * Format article content for sharing
     */
    private fun formatArticleForSharing(article: DiscoverContent.Article): String {
        return buildString {
            appendLine("📄 ${article.title}")
            appendLine()
            appendLine("${article.summary}")
            appendLine()
            appendLine("👨‍⚕️ By ${article.authorName}")
            if (article.authorCredentials.isNotBlank()) {
                appendLine("   ${article.authorCredentials}")
            }
            appendLine("⏱️ ${article.readingTimeMinutes} min read")
            appendLine("🏷️ ${formatCategory(article.category)}")
            appendLine()
            appendLine("Read more: ${createShareableLink(article)}")
            appendLine()
            appendLine("Shared via $APP_NAME")
            appendLine("$APP_URL")
        }
    }

    /**
     * Format news content for sharing
     */
    private fun formatNewsForSharing(news: DiscoverContent.News): String {
        return buildString {
            if (news.isBreakingNews) {
                appendLine("🚨 BREAKING NEWS")
            }
            appendLine("📰 ${news.title}")
            appendLine()
            appendLine("${news.summary}")
            appendLine()
            appendLine("📰 Source: ${news.sourcePublication}")
            appendLine("📅 ${formatDate(news.publishedDate)}")
            appendLine("🏷️ ${formatCategory(news.category)}")
            appendLine()
            appendLine("Read more: ${createShareableLink(news)}")
            appendLine()
            appendLine("Shared via $APP_NAME")
            appendLine("$APP_URL")
        }
    }

    /**
     * Format video content for sharing
     */
    private fun formatVideoForSharing(video: DiscoverContent.Video): String {
        return buildString {
            appendLine("🎥 ${video.title}")
            appendLine()
            appendLine("${video.description}")
            appendLine()
            appendLine("👨‍⚕️ Expert: ${video.expertName}")
            if (video.expertCredentials.isNotBlank()) {
                appendLine("   ${video.expertCredentials}")
            }
            appendLine("⏱️ Duration: ${formatDuration(video.durationSeconds)}")
            appendLine("📊 Level: ${formatCategory(video.difficultyLevel)}")
            appendLine("🏷️ ${formatCategory(video.category)}")
            appendLine()
            appendLine("Watch: ${createShareableLink(video)}")
            appendLine()
            appendLine("Shared via $APP_NAME")
            appendLine("$APP_URL")
        }
    }

    /**
     * Format content for social media platforms
     */
    private fun formatContentForSocialMedia(content: DiscoverContent, platform: SocialPlatform): String {
        val maxLength = when (platform) {
            SocialPlatform.TWITTER -> 280
            SocialPlatform.FACEBOOK -> 500
            SocialPlatform.LINKEDIN -> 700
            SocialPlatform.WHATSAPP -> 1000
        }

        val baseText = when (content) {
            is DiscoverContent.Article -> {
                "📄 ${content.title}\n\n${content.summary}\n\n👨‍⚕️ ${content.authorName} • ${content.readingTimeMinutes} min read"
            }
            is DiscoverContent.News -> {
                val breaking = if (content.isBreakingNews) "🚨 " else ""
                "${breaking}📰 ${content.title}\n\n${content.summary}\n\n📰 ${content.sourcePublication}"
            }
            is DiscoverContent.Video -> {
                "🎥 ${content.title}\n\n${content.description}\n\n👨‍⚕️ ${content.expertName} • ${formatDuration(content.durationSeconds)}"
            }
        }

        val link = createShareableLink(content)
        val attribution = "\n\nvia $APP_NAME"
        val fullText = "$baseText$attribution\n$link"

        return if (fullText.length <= maxLength) {
            fullText
        } else {
            val availableLength = maxLength - attribution.length - link.length - 10 // buffer
            val truncatedBase = if (baseText.length > availableLength) {
                "${baseText.take(availableLength - 3)}..."
            } else {
                baseText
            }
            "$truncatedBase$attribution\n$link"
        }
    }

    /**
     * Format content for email sharing
     */
    private fun formatContentForEmail(content: DiscoverContent): String {
        return buildString {
            appendLine("Hello,")
            appendLine()
            appendLine("I wanted to share this health content with you:")
            appendLine()
            appendLine("=".repeat(50))
            appendLine()
            
            when (content) {
                is DiscoverContent.Article -> {
                    appendLine("HEALTH ARTICLE")
                    appendLine("Title: ${content.title}")
                    appendLine("Author: ${content.authorName}")
                    if (content.authorCredentials.isNotBlank()) {
                        appendLine("Credentials: ${content.authorCredentials}")
                    }
                    appendLine("Reading Time: ${content.readingTimeMinutes} minutes")
                    appendLine("Category: ${formatCategory(content.category)}")
                    appendLine("Credibility Score: ${content.credibilityScore}/5")
                    appendLine()
                    appendLine("SUMMARY:")
                    appendLine(content.summary)
                    appendLine()
                    if (content.sourceUrl.isNotBlank()) {
                        appendLine("Original Source: ${content.sourceUrl}")
                    }
                }
                is DiscoverContent.News -> {
                    if (content.isBreakingNews) {
                        appendLine("⚠️ BREAKING HEALTH NEWS")
                    } else {
                        appendLine("HEALTH NEWS")
                    }
                    appendLine("Headline: ${content.title}")
                    appendLine("Source: ${content.sourcePublication}")
                    appendLine("Published: ${formatDate(content.publishedDate)}")
                    appendLine("Category: ${formatCategory(content.category)}")
                    appendLine("Source Type: ${formatCredibility(content.sourceCredibility)}")
                    appendLine()
                    appendLine("SUMMARY:")
                    appendLine(content.summary)
                    appendLine()
                    if (content.externalUrl.isNotBlank()) {
                        appendLine("Original Article: ${content.externalUrl}")
                    }
                }
                is DiscoverContent.Video -> {
                    appendLine("EDUCATIONAL VIDEO")
                    appendLine("Title: ${content.title}")
                    appendLine("Expert: ${content.expertName}")
                    if (content.expertCredentials.isNotBlank()) {
                        appendLine("Credentials: ${content.expertCredentials}")
                    }
                    appendLine("Duration: ${formatDuration(content.durationSeconds)}")
                    appendLine("Difficulty: ${formatCategory(content.difficultyLevel)}")
                    appendLine("Category: ${formatCategory(content.category)}")
                    appendLine()
                    appendLine("DESCRIPTION:")
                    appendLine(content.description)
                    appendLine()
                }
            }
            
            appendLine()
            appendLine("=".repeat(50))
            appendLine()
            appendLine("You can view this content in the Health Assistant app:")
            appendLine(createShareableLink(content))
            appendLine()
            appendLine("Download Health Assistant: $APP_URL")
            appendLine()
            appendLine("Best regards,")
            appendLine("Shared via Health Assistant")
        }
    }

    /**
     * Format content as a quote for clipboard
     */
    private fun formatContentAsQuote(content: DiscoverContent): String {
        return when (content) {
            is DiscoverContent.Article -> {
                "\"${content.summary}\"\n\n— ${content.authorName}, ${content.authorCredentials}"
            }
            is DiscoverContent.News -> {
                "\"${content.summary}\"\n\n— ${content.sourcePublication}, ${formatDate(content.publishedDate)}"
            }
            is DiscoverContent.Video -> {
                "\"${content.description}\"\n\n— ${content.expertName}, ${content.expertCredentials}"
            }
        }
    }

    /**
     * Format content citation for academic/professional use
     */
    private fun formatContentCitation(content: DiscoverContent): String {
        return when (content) {
            is DiscoverContent.Article -> {
                "${content.authorName}. \"${content.title}.\" Health Assistant. ${formatDate(content.publishedDate)}. ${createShareableLink(content)}"
            }
            is DiscoverContent.News -> {
                "\"${content.title}.\" ${content.sourcePublication}. ${formatDate(content.publishedDate)}. ${createShareableLink(content)}"
            }
            is DiscoverContent.Video -> {
                "${content.expertName}. \"${content.title}.\" Health Assistant Video. ${formatDate(content.publishedDate)}. ${createShareableLink(content)}"
            }
        }
    }

    /**
     * Create Twitter-specific intent
     */
    private fun createTwitterIntent(text: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.twitter.android")
        }
    }

    /**
     * Create Facebook-specific intent
     */
    private fun createFacebookIntent(text: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.facebook.katana")
        }
    }

    /**
     * Create LinkedIn-specific intent
     */
    private fun createLinkedInIntent(text: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.linkedin.android")
        }
    }

    /**
     * Create WhatsApp-specific intent
     */
    private fun createWhatsAppIntent(text: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }
    }

    /**
     * Get share subject for content
     */
    private fun getShareSubject(content: DiscoverContent): String {
        return when (content) {
            is DiscoverContent.Article -> "Health Article: ${content.title}"
            is DiscoverContent.News -> "Health News: ${content.title}"
            is DiscoverContent.Video -> "Health Video: ${content.title}"
        }
    }

    /**
     * Get email subject for content
     */
    private fun getEmailSubject(content: DiscoverContent): String {
        return when (content) {
            is DiscoverContent.Article -> "Sharing Health Article: ${content.title}"
            is DiscoverContent.News -> "Sharing Health News: ${content.title}"
            is DiscoverContent.Video -> "Sharing Educational Video: ${content.title}"
        }
    }

    /**
     * Format category for display
     */
    private fun formatCategory(category: String): String {
        return category.split("-", "_")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    /**
     * Format credibility for display
     */
    private fun formatCredibility(credibility: String): String {
        return when (credibility) {
            "peer-reviewed" -> "Peer-Reviewed Study"
            "medical-journal" -> "Medical Journal"
            "health-organization" -> "Health Organization"
            else -> credibility.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Format duration from seconds to readable format
     */
    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return when {
            minutes == 0 -> "${remainingSeconds}s"
            remainingSeconds == 0 -> "${minutes}m"
            else -> "${minutes}m ${remainingSeconds}s"
        }
    }

    /**
     * Format date for display
     */
    private fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Enum for social media platforms
     */
    enum class SocialPlatform {
        TWITTER, FACEBOOK, LINKEDIN, WHATSAPP
    }

    /**
     * Enum for clipboard copy types
     */
    enum class ClipboardCopyType {
        TITLE_ONLY, SUMMARY_ONLY, QUOTE, CITATION, FULL_CONTENT
    }
}