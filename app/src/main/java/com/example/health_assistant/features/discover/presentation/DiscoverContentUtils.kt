package com.example.health_assistant.features.discover.presentation

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.content.ContextCompat
import com.example.health_assistant.R

/**
 * Utility class for Discover content presentation helpers
 * Includes text highlighting, formatting, and search result enhancements
 */
object DiscoverContentUtils {

    /**
     * Highlight search terms in text with background color and bold styling
     */
    fun highlightSearchTerms(
        text: String,
        searchQuery: String,
        context: android.content.Context
    ): SpannableString {
        val spannableString = SpannableString(text)
        
        if (searchQuery.isBlank()) {
            return spannableString
        }
        
        val searchTerms = searchQuery.lowercase().split("\\s+".toRegex())
        val textLowerCase = text.lowercase()
        
        val highlightColor = ContextCompat.getColor(context, R.color.search_highlight_background)
        val textColor = ContextCompat.getColor(context, R.color.search_highlight_text)
        
        searchTerms.forEach { term ->
            if (term.length >= 2) {
                var startIndex = 0
                while (startIndex < textLowerCase.length) {
                    val index = textLowerCase.indexOf(term, startIndex)
                    if (index == -1) break
                    
                    val endIndex = index + term.length
                    
                    // Apply background highlight
                    spannableString.setSpan(
                        BackgroundColorSpan(highlightColor),
                        index,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    
                    // Apply text color
                    spannableString.setSpan(
                        ForegroundColorSpan(textColor),
                        index,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    
                    // Apply bold styling
                    spannableString.setSpan(
                        StyleSpan(Typeface.BOLD),
                        index,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    
                    startIndex = endIndex
                }
            }
        }
        
        return spannableString
    }

    /**
     * Calculate relevance score for search result ranking
     */
    fun calculateRelevanceScore(
        title: String,
        summary: String,
        category: String,
        searchQuery: String,
        credibilityScore: Int = 0
    ): Double {
        if (searchQuery.isBlank()) return 0.0
        
        var score = 0.0
        val queryTerms = searchQuery.lowercase().split("\\s+".toRegex())
        val titleLower = title.lowercase()
        val summaryLower = summary.lowercase()
        val categoryLower = category.lowercase()

        queryTerms.forEach { term ->
            if (term.length >= 2) {
                // Title matches get highest score
                when {
                    titleLower.startsWith(term) -> score += 10.0
                    titleLower.contains(term) -> score += 5.0
                }
                
                // Summary matches get medium score
                if (summaryLower.contains(term)) {
                    score += 2.0
                }
                
                // Category matches get lower score
                if (categoryLower.contains(term)) {
                    score += 1.0
                }
                
                // Exact phrase matches get bonus
                if (titleLower.contains(searchQuery.lowercase())) {
                    score += 15.0
                }
            }
        }

        // Boost score based on credibility
        if (credibilityScore > 0) {
            score *= (1.0 + credibilityScore * 0.1)
        }

        return score
    }

    /**
     * Format content category for display
     */
    fun formatCategory(category: String): String {
        return category.replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase() else it.toString() 
                }
            }
    }

    /**
     * Get category color resource based on category type
     */
    fun getCategoryColor(category: String, context: android.content.Context): Int {
        return when (category.lowercase()) {
            "nutrition" -> ContextCompat.getColor(context, R.color.category_nutrition)
            "fitness" -> ContextCompat.getColor(context, R.color.category_fitness)
            "mental-health", "mental_health" -> ContextCompat.getColor(context, R.color.category_mental_health)
            "preventive-care", "preventive_care" -> ContextCompat.getColor(context, R.color.category_preventive_care)
            "chronic-conditions", "chronic_conditions" -> ContextCompat.getColor(context, R.color.category_chronic_conditions)
            else -> ContextCompat.getColor(context, R.color.category_default)
        }
    }

    /**
     * Truncate text with ellipsis for search results
     */
    fun truncateText(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) {
            text
        } else {
            "${text.substring(0, maxLength - 3)}..."
        }
    }

    /**
     * Extract snippet around search term for preview
     */
    fun extractSearchSnippet(
        text: String,
        searchQuery: String,
        snippetLength: Int = 150
    ): String {
        if (searchQuery.isBlank()) {
            return truncateText(text, snippetLength)
        }
        
        val textLower = text.lowercase()
        val queryLower = searchQuery.lowercase()
        val firstTerm = queryLower.split("\\s+".toRegex()).firstOrNull() ?: ""
        
        if (firstTerm.length < 2) {
            return truncateText(text, snippetLength)
        }
        
        val index = textLower.indexOf(firstTerm)
        if (index == -1) {
            return truncateText(text, snippetLength)
        }
        
        // Calculate snippet boundaries
        val halfSnippet = snippetLength / 2
        val start = maxOf(0, index - halfSnippet)
        val end = minOf(text.length, index + firstTerm.length + halfSnippet)
        
        var snippet = text.substring(start, end)
        
        // Add ellipsis if truncated
        if (start > 0) snippet = "...$snippet"
        if (end < text.length) snippet = "$snippet..."
        
        return snippet
    }

    /**
     * Format credibility score for display
     */
    fun formatCredibilityScore(score: Int): String {
        return when {
            score >= 5 -> "Highly Verified"
            score >= 4 -> "Verified"
            score >= 3 -> "Credible"
            score >= 2 -> "Moderate"
            else -> "Unverified"
        }
    }

    /**
     * Get credibility color based on score
     */
    fun getCredibilityColor(score: Int, context: android.content.Context): Int {
        return when {
            score >= 4 -> ContextCompat.getColor(context, R.color.credibility_high)
            score >= 3 -> ContextCompat.getColor(context, R.color.credibility_medium)
            score >= 2 -> ContextCompat.getColor(context, R.color.credibility_low)
            else -> ContextCompat.getColor(context, R.color.credibility_unverified)
        }
    }
}