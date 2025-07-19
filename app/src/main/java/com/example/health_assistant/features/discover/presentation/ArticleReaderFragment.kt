package com.example.health_assistant.features.discover.presentation

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.health_assistant.R
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.databinding.FragmentArticleReaderBinding
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.model.ContentValidationResult
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ArticleReaderFragment : Fragment() {
    private var _binding: FragmentArticleReaderBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ArticleReaderViewModel by viewModels()
    private val args: ArticleReaderFragmentArgs by navArgs()
    
    private var currentArticle: DiscoverContent.Article? = null
    private var isBookmarked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArticleReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupClickListeners()
        setupScrollListener()
        setupObservers()
        
        // Load the article
        viewModel.loadArticle(args.articleId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupClickListeners() {
        binding.fabBookmark.setOnClickListener {
            currentArticle?.let { article ->
                viewModel.toggleBookmark(article.id)
            }
        }
        
        binding.fabShare.setOnClickListener {
            currentArticle?.let { article ->
                shareArticle(article)
            }
        }
        
        binding.buttonRetry.setOnClickListener {
            viewModel.loadArticle(args.articleId)
        }
    }

    private fun setupScrollListener() {
        binding.nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            // Calculate reading progress based on scroll position
            val contentHeight = binding.nestedScrollView.getChildAt(0).height
            val scrollViewHeight = binding.nestedScrollView.height
            val maxScroll = contentHeight - scrollViewHeight
            
            if (maxScroll > 0) {
                val progress = (scrollY.toFloat() / maxScroll.toFloat()).coerceIn(0f, 1f)
                binding.progressReading.progress = (progress * 100).toInt()
                
                // Update reading progress in the background
                currentArticle?.let { article ->
                    viewModel.updateReadingProgress(article.id, progress)
                }
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe article loading state
                launch {
                    viewModel.uiState.collect { uiState ->
                        handleUiState(uiState)
                    }
                }
                
                // Observe article data
                launch {
                    viewModel.article.collect { result ->
                        handleArticleResult(result)
                    }
                }
                
                // Observe bookmark state
                launch {
                    viewModel.isBookmarked.collect { bookmarked ->
                        updateBookmarkButton(bookmarked)
                    }
                }
                
                // Observe content validation
                launch {
                    viewModel.contentValidation.collect { result ->
                        handleContentValidation(result)
                    }
                }
            }
        }
    }

    private fun handleUiState(uiState: ArticleReaderUiState) {
        // Handle loading state
        binding.layoutLoading.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE
        binding.nestedScrollView.visibility = if (uiState.isLoading) View.GONE else View.VISIBLE
        
        // Handle error state
        if (uiState.error != null) {
            showError(uiState.error)
            binding.layoutError.visibility = View.VISIBLE
            binding.nestedScrollView.visibility = View.GONE
        } else {
            binding.layoutError.visibility = View.GONE
        }
        
        // Handle messages
        uiState.message?.let { message ->
            showMessage(message)
            viewModel.clearMessage()
        }
    }

    private fun handleArticleResult(result: Result<DiscoverContent.Article>) {
        when (result) {
            is Result.Success -> {
                currentArticle = result.data
                displayArticle(result.data)
                viewModel.validateContent(result.data.id)
            }
            is Result.Error -> {
                showError("Failed to load article: ${result.exception?.message ?: "Unknown error"}")
            }
            is Result.Loading -> {
                // Loading state is handled by uiState
            }
        }
    }

    private fun handleContentValidation(result: Result<ContentValidationResult>?) {
        result?.let {
            when (it) {
                is Result.Success -> {
                    displayContentWarnings(it.data)
                }
                is Result.Error -> {
                    // Validation errors are not critical, handle silently
                }
                is Result.Loading -> {
                    // Validation loading state
                }
            }
        }
    }

    private fun displayArticle(article: DiscoverContent.Article) {
        // Set article title
        binding.textArticleTitle.text = article.title
        
        // Set author information
        binding.textAuthorName.text = article.authorName
        binding.textAuthorCredentials.text = article.authorCredentials
        
        // Set publication date
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val publishedDate = Date(article.publishedDate)
        binding.textPublicationDate.text = getString(R.string.published_on, dateFormat.format(publishedDate))
        
        // Set reading time
        binding.textReadingTime.text = getString(R.string.min_read, article.readingTimeMinutes)
        
        // Set category
        binding.chipCategory.text = article.category.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
        
        // Set credibility score
        displayCredibilityScore(article.credibilityScore)
        
        // Set article summary
        binding.textArticleSummary.text = article.summary
        
        // Set article content
        binding.textArticleContent.text = article.content
        
        // Set source information
        binding.textSourceUrl.text = article.sourceUrl
        
        // Set last updated
        val lastUpdatedDate = Date(article.lastUpdated)
        binding.textLastUpdated.text = getString(R.string.last_updated, dateFormat.format(lastUpdatedDate))
        
        // Set reading progress
        binding.progressReading.progress = (article.readProgress * 100).toInt()
        
        // Load header image
        loadHeaderImage(article.imageUrl)
        
        // Display tags
        displayTags(article.tags)
        
        // Update bookmark state
        isBookmarked = article.isBookmarked
        updateBookmarkButton(isBookmarked)
    }

    private fun displayCredibilityScore(score: Int) {
        val (text, color) = when (score) {
            5 -> Pair("Peer-Reviewed", R.color.credibility_high)
            4 -> Pair("High Credibility", R.color.credibility_high)
            3 -> Pair("Verified Source", R.color.credibility_medium)
            2 -> Pair("General Source", R.color.credibility_low)
            else -> Pair("Unverified", R.color.credibility_unverified)
        }
        
        binding.textCredibilityScore.text = text
        binding.textCredibilityScore.setTextColor(ContextCompat.getColor(requireContext(), color))
        binding.layoutCredibilityScore.background?.setTint(
            ContextCompat.getColor(requireContext(), when (score) {
                5, 4 -> R.color.credibility_high
                3 -> R.color.credibility_medium
                2 -> R.color.credibility_low
                else -> R.color.credibility_unverified
            })
        )
    }

    private fun displayContentWarnings(validation: ContentValidationResult) {
        if (validation.warnings.isNotEmpty()) {
            binding.cardContentWarning.visibility = View.VISIBLE
            binding.textContentWarning.text = validation.warnings.joinToString("\n• ", "• ")
        } else {
            binding.cardContentWarning.visibility = View.GONE
        }
    }

    private fun loadHeaderImage(imageUrl: String?) {
        // For now, use a placeholder image. In a production app, you would use an image loading library like Glide or Coil
        binding.imageArticleHeader.setImageResource(R.drawable.ic_health_monitoring)
    }

    private fun displayTags(tags: List<String>) {
        binding.chipGroupTags.removeAllViews()
        
        tags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(R.color.surface_secondary)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
            }
            binding.chipGroupTags.addView(chip)
        }
    }

    private fun updateBookmarkButton(bookmarked: Boolean) {
        isBookmarked = bookmarked
        val iconRes = if (bookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark
        binding.fabBookmark.setImageResource(iconRes)
        
        val contentDescription = if (bookmarked) {
            "Remove bookmark"
        } else {
            getString(R.string.bookmark_article)
        }
        binding.fabBookmark.contentDescription = contentDescription
    }

    private fun shareArticle(article: DiscoverContent.Article) {
        val sharingBottomSheet = ContentSharingBottomSheet.newInstance(article)
        sharingBottomSheet.show(childFragmentManager, "ContentSharingBottomSheet")
    }

    private fun showError(message: String) {
        binding.textErrorMessage.text = message
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Retry") { 
                viewModel.loadArticle(args.articleId)
            }
            .show()
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        // Save reading progress when user leaves the screen
        currentArticle?.let { article ->
            val contentHeight = binding.nestedScrollView.getChildAt(0).height
            val scrollViewHeight = binding.nestedScrollView.height
            val maxScroll = contentHeight - scrollViewHeight
            val scrollY = binding.nestedScrollView.scrollY
            
            if (maxScroll > 0) {
                val progress = (scrollY.toFloat() / maxScroll.toFloat()).coerceIn(0f, 1f)
                viewModel.updateReadingProgress(article.id, progress)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}