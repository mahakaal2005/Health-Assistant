package com.example.health_assistant.features.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.health_assistant.R
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.databinding.FragmentDiscoverBinding
import com.example.health_assistant.features.discover.presentation.DiscoverContentAdapter
import com.example.health_assistant.features.discover.presentation.DiscoverViewModel
import com.example.health_assistant.features.discover.presentation.DiscoverUiState
import com.example.health_assistant.features.discover.domain.DiscoverFeedData
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.model.HealthContentCategory
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DiscoverFragment : Fragment() {
    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DiscoverViewModel by viewModels()
    private lateinit var contentAdapter: DiscoverContentAdapter
    private lateinit var searchAdapter: DiscoverContentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupMenu()
        setupRecyclerView()
        setupSearchView()
        setupCategoryFilters()
        setupSwipeRefresh()
        setupClickListeners()
        setupObservers()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.discover_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_filter -> {
                        // TODO: Show filter dialog
                        true
                    }
                    R.id.action_bookmarks -> {
                        findNavController().navigate(
                            DiscoverFragmentDirections.actionDiscoverFragmentToBookmarksFragment()
                        )
                        true
                    }
                    R.id.action_refresh -> {
                        viewModel.refreshContent()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        // Main content adapter
        contentAdapter = DiscoverContentAdapter(
            onArticleClick = { article -> 
                navigateToArticleReader(article.id)
            },
            onNewsClick = { news -> 
                // TODO: Navigate to news reader
            },
            onVideoClick = { video -> 
                // TODO: Navigate to video player
            },
            onBookmarkClick = { content -> 
                viewModel.toggleBookmark(content)
            },
            onShareClick = { content -> 
                shareContent(content)
            }
        )

        binding.recyclerViewContent.apply {
            adapter = contentAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        // Search results adapter
        searchAdapter = DiscoverContentAdapter(
            onArticleClick = { article -> 
                navigateToArticleReader(article.id)
            },
            onNewsClick = { news -> 
                // TODO: Navigate to news reader
            },
            onVideoClick = { video -> 
                // TODO: Navigate to video player
            },
            onBookmarkClick = { content -> 
                viewModel.toggleBookmark(content)
            },
            onShareClick = { content -> 
                shareContent(content)
            }
        )

        binding.recyclerViewSearchResults.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearchView() {
        binding.searchBar.setOnClickListener {
            binding.searchView.show()
        }

        // Set up real-time search with debouncing
        binding.searchView.editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString() ?: ""
                viewModel.updateSearchQuery(query)
            }
        })

        binding.searchView.editText.setOnEditorActionListener { _, _, _ ->
            val query = binding.searchView.text.toString()
            if (query.isNotBlank()) {
                viewModel.searchContent(query)
                binding.searchView.hide()
            }
            true
        }

        // Handle search view close
        binding.searchView.addTransitionListener { _, _, newState ->
            if (newState == com.google.android.material.search.SearchView.TransitionState.HIDDEN) {
                viewModel.clearSearch()
            }
        }
    }

    private fun setupCategoryFilters() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            val selectedCategory = when {
                checkedIds.isEmpty() || binding.chipAll.isChecked -> null
                binding.chipNutrition.isChecked -> HealthContentCategory.NUTRITION
                binding.chipFitness.isChecked -> HealthContentCategory.FITNESS
                binding.chipMentalHealth.isChecked -> HealthContentCategory.MENTAL_HEALTH
                binding.chipPreventiveCare.isChecked -> HealthContentCategory.PREVENTIVE_CARE
                binding.chipChronicConditions.isChecked -> HealthContentCategory.CHRONIC_CONDITIONS
                else -> null
            }
            
            // Update visual feedback for active filters
            updateCategoryFilterVisualFeedback(selectedCategory)
            
            // Load content for selected category
            viewModel.loadContentByCategory(selectedCategory)
        }
    }

    private fun updateCategoryFilterVisualFeedback(selectedCategory: HealthContentCategory?) {
        // Reset all chips to default state
        val chips = listOf(
            binding.chipAll,
            binding.chipNutrition,
            binding.chipFitness,
            binding.chipMentalHealth,
            binding.chipPreventiveCare,
            binding.chipChronicConditions
        )
        
        chips.forEach { chip ->
            chip.setChipBackgroundColorResource(R.color.surface_secondary)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
        }
        
        // Highlight the selected chip
        val activeChip = when (selectedCategory) {
            null -> binding.chipAll
            HealthContentCategory.NUTRITION -> binding.chipNutrition
            HealthContentCategory.FITNESS -> binding.chipFitness
            HealthContentCategory.MENTAL_HEALTH -> binding.chipMentalHealth
            HealthContentCategory.PREVENTIVE_CARE -> binding.chipPreventiveCare
            HealthContentCategory.CHRONIC_CONDITIONS -> binding.chipChronicConditions
            else -> binding.chipAll
        }
        
        activeChip.setChipBackgroundColorResource(R.color.colorPrimary)
        activeChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshContent()
        }
    }

    private fun setupClickListeners() {
        binding.buttonRetry.setOnClickListener {
            viewModel.refreshContent()
        }

        binding.buttonRetryError.setOnClickListener {
            viewModel.refreshContent()
        }
    }

    private fun setupObservers() {
        // Test sample data (remove this in production)
        testSampleData()
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe UI state
                launch {
                    viewModel.uiState.collect { uiState ->
                        handleUiState(uiState)
                    }
                }
                
                // Observe content feed
                launch {
                    viewModel.contentFeed.collect { result ->
                        handleContentFeed(result)
                    }
                }
                
                // Observe search results
                launch {
                    viewModel.searchResults.collect { result ->
                        handleSearchResults(result)
                    }
                }
                
                // Observe search state
                launch {
                    viewModel.isSearchActive.collect { isActive ->
                        handleSearchActiveState(isActive)
                    }
                }
                
                // Observe search suggestions
                launch {
                    viewModel.searchSuggestions.collect { suggestions ->
                        handleSearchSuggestions(suggestions)
                    }
                }
                
                // Observe search loading state
                launch {
                    viewModel.isSearching.collect { isSearching ->
                        handleSearchLoadingState(isSearching)
                    }
                }
            }
        }
    }

    private fun handleUiState(uiState: DiscoverUiState) {
        // Handle loading state
        binding.swipeRefreshLayout.isRefreshing = uiState.isRefreshing
        
        // Show/hide loading indicator
        binding.layoutLoading.visibility = if (uiState.isLoading && !uiState.isRefreshing) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        // Handle error state
        uiState.error?.let { error ->
            showError(error)
            viewModel.clearError()
        }
        
        // Handle message state
        uiState.message?.let { message ->
            showMessage(message)
            viewModel.clearMessage()
        }
        
        // Handle empty state
        if (uiState.isEmpty && !uiState.isLoading) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun handleContentFeed(result: Result<DiscoverFeedData>) {
        when (result) {
            is Result.Success -> {
                displayContentFeed(result.data)
            }
            is Result.Error -> {
                showError("Failed to load content: ${result.exception?.message ?: "Unknown error"}")
            }
            is Result.Loading -> {
                showLoading()
            }
        }
    }

    private fun handleSearchResults(result: Result<com.example.health_assistant.features.discover.domain.SearchResults>?) {
        result?.let {
            when (it) {
                is Result.Success -> {
                    displaySearchResults(it.data.results, it.data.query)
                }
                is Result.Error -> {
                    showError("Search failed: ${it.exception?.message ?: "Unknown error"}")
                }
                is Result.Loading -> {
                    showSearchLoading()
                }
            }
        }
    }

    private fun handleTrendingContent(result: Result<List<DiscoverContent>>) {
        when (result) {
            is Result.Success -> {
                displayTrendingContent(result.data)
            }
            is Result.Error -> {
                // Handle trending content error silently or show minimal error
            }
            is Result.Loading -> {
                // Handle trending loading state
            }
        }
    }

    private fun handleBookmarkedContent(result: Result<List<DiscoverContent>>) {
        when (result) {
            is Result.Success -> {
                displayBookmarkedContent(result.data)
            }
            is Result.Error -> {
                // Handle bookmarks error silently
            }
            is Result.Loading -> {
                // Handle bookmarks loading state
            }
        }
    }

    // UI Display Methods
    private fun displayContentFeed(feedData: DiscoverFeedData) {
        val allContent = mutableListOf<DiscoverContent>()
        allContent.addAll(feedData.articles)
        allContent.addAll(feedData.news)
        allContent.addAll(feedData.videos)
        
        // Sort by published date (newest first)
        val sortedContent = allContent.sortedByDescending { it.publishedDate }
        contentAdapter.submitList(sortedContent)
        
        // Hide error and empty states
        binding.layoutError.visibility = View.GONE
        hideEmptyState()
    }

    private fun displaySearchResults(results: List<DiscoverContent>, query: String) {
        searchAdapter.updateSearchQuery(query)
        searchAdapter.submitList(results)
    }

    private fun displayTrendingContent(content: List<DiscoverContent>) {
        // Trending content could be displayed in a separate section if needed
        // For now, it's integrated into the main feed
    }

    private fun displayBookmarkedContent(content: List<DiscoverContent>) {
        // Bookmarked content could be displayed in a separate view
        // For now, bookmark state is handled in the adapter
    }

    private fun showLoading() {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.layoutError.visibility = View.GONE
        hideEmptyState()
    }

    private fun showSearchLoading() {
        // Search loading is handled by the search view itself
    }

    private fun showError(message: String) {
        binding.layoutError.visibility = View.VISIBLE
        binding.textErrorMessage.text = message
        binding.layoutLoading.visibility = View.GONE
        hideEmptyState()
        
        // Also show snackbar for immediate feedback
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Retry") { viewModel.refreshContent() }
            .show()
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showEmptyState() {
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.layoutError.visibility = View.GONE
        binding.layoutLoading.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.layoutEmpty.visibility = View.GONE
    }

    // Search-specific UI handlers
    private fun handleSearchActiveState(isActive: Boolean) {
        // Show/hide search results vs main content
        if (isActive) {
            binding.recyclerViewContent.visibility = View.GONE
            binding.recyclerViewSearchResults.visibility = View.VISIBLE
        } else {
            binding.recyclerViewContent.visibility = View.VISIBLE
            binding.recyclerViewSearchResults.visibility = View.GONE
        }
    }

    private fun handleSearchSuggestions(suggestions: List<String>) {
        // For now, we'll handle suggestions through the search view's built-in functionality
        // In a more advanced implementation, we could show a dropdown with suggestions
        if (suggestions.isNotEmpty() && binding.searchView.isShowing) {
            // Could implement a suggestion dropdown here
        }
    }

    private fun handleSearchLoadingState(isSearching: Boolean) {
        // Show search loading indicator if needed
        // The search view handles its own loading state
    }

    private fun navigateToArticleReader(articleId: String) {
        val action = DiscoverFragmentDirections.actionDiscoverFragmentToArticleReaderFragment(articleId)
        findNavController().navigate(action)
    }

    private fun shareContent(content: DiscoverContent) {
        val sharingBottomSheet = com.example.health_assistant.features.discover.presentation.ContentSharingBottomSheet.newInstance(content)
        sharingBottomSheet.show(childFragmentManager, "ContentSharingBottomSheet")
    }

    private fun testSampleData() {
        // Simple test to verify data is working
        android.util.Log.d("DiscoverFragment", "Testing sample data availability...")
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.contentFeed.collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val totalItems = result.data.totalItems
                            android.util.Log.d("DiscoverFragment", "SUCCESS: Feed has $totalItems items")
                            android.util.Log.d("DiscoverFragment", "Articles: ${result.data.articles.size}")
                            android.util.Log.d("DiscoverFragment", "News: ${result.data.news.size}")
                            android.util.Log.d("DiscoverFragment", "Videos: ${result.data.videos.size}")
                        }
                        is Result.Error -> {
                            android.util.Log.e("DiscoverFragment", "ERROR: ${result.exception?.message}")
                        }
                        is Result.Loading -> {
                            android.util.Log.d("DiscoverFragment", "LOADING...")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DiscoverFragment", "Exception in test", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}