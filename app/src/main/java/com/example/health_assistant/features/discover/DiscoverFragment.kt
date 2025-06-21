package com.example.health_assistant.features.discover

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.health_assistant.databinding.FragmentDiscoverBinding
import com.example.health_assistant.features.discover.adapters.FeaturedTopicsAdapter
import com.example.health_assistant.features.discover.adapters.QuickActionsAdapter
import com.example.health_assistant.features.discover.adapters.RecentSearchesAdapter
import com.example.health_assistant.features.discover.model.HealthTopic
import com.example.health_assistant.features.discover.model.QuickAction
import kotlinx.coroutines.launch

class DiscoverFragment : Fragment() {
    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    // Initialize ViewModel using ViewModelProvider
    private lateinit var viewModel: DiscoverViewModel

    // Initialize adapters
    private val quickActionsAdapter by lazy {
        QuickActionsAdapter { action -> handleQuickActionClick(action) }
    }

    private val featuredTopicsAdapter by lazy {
        FeaturedTopicsAdapter { topic -> handleTopicClick(topic) }
    }

    private val recentSearchesAdapter by lazy {
        RecentSearchesAdapter { query -> handleRecentSearchClick(query) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)

        // Initialize the ViewModel using ViewModelProvider (no DI)
        viewModel = ViewModelProvider(this)[DiscoverViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI elements and set up any event listeners
        setupUI()

        // Observe ViewModel state changes
        observeViewModelState()
    }

    private fun observeViewModelState() {
        // Use Kotlin coroutines to collect StateFlow updates
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe search query changes
                launch {
                    viewModel.searchQuery.collect { query ->
                        Log.d(TAG, "Search query updated: $query")
                        binding.searchEditText.setText(query)

                        // Update the visibility of sections based on search state
                        updateContentVisibilityForSearchState(query.isNotEmpty())
                    }
                }

                // Observe search results
                launch {
                    viewModel.searchResults.collect { results ->
                        Log.d(TAG, "Search results updated: ${results.size} items")
                        // If implementing search results list in future, would update adapter here
                    }
                }

                // Observe featured topics
                launch {
                    viewModel.featuredTopics.collect { topics ->
                        Log.d(TAG, "Featured topics updated: ${topics.size} items")
                        featuredTopicsAdapter.submitList(topics)
                        updateFeaturedTopicsVisibility(topics)
                    }
                }

                // Observe quick actions
                launch {
                    viewModel.quickActions.collect { actions ->
                        Log.d(TAG, "Quick actions updated: ${actions.size} items")
                        quickActionsAdapter.submitList(actions)
                        updateQuickActionsVisibility(actions)
                    }
                }

                // Observe recent searches
                launch {
                    viewModel.recentSearches.collect { searches ->
                        Log.d(TAG, "Recent searches updated: ${searches.size} items")
                        recentSearchesAdapter.submitList(searches)
                        updateRecentSearchesVisibility(searches)
                    }
                }

                // Observe loading state
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        Log.d(TAG, "Loading state updated: $isLoading")
                        updateLoadingState(isLoading)
                    }
                }

                // Observe error state
                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            Log.e(TAG, "Error state: $it")
                            // Could show a Snackbar with the error message
                            // Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupUI() {
        // Set up search functionality
        setupSearchBar()

        // Set up AI interaction components
        setupAIComponents()

        // Set up RecyclerViews with adapters
        setupQuickActionsRecyclerView()
        setupFeaturedTopicsRecyclerView()
        setupRecentSearchesRecyclerView()
    }

    private fun setupSearchBar() {
        // Set up search bar with action listeners
        binding.searchEditText.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = textView.text.toString()
                handleSearchQuery(query)
                return@setOnEditorActionListener true
            }
            false
        }

        // Clear text button already handled by TextInputLayout's endIconMode="clear_text"
        binding.searchInputLayout.setEndIconOnClickListener {
            binding.searchEditText.text?.clear()
            handleSearchQuery("")  // Clear the search query in ViewModel
        }
    }

    private fun handleSearchQuery(query: String) {
        Log.d(TAG, "Search initiated with query: $query")
        viewModel.setSearchQuery(query)
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun setupAIComponents() {
        // Set up AI chat chip
        binding.aiChatChip.setOnClickListener {
            // Will implement AI chat functionality in a future step
            Log.d(TAG, "AI chat chip clicked")
        }

        // Set up AI chat FAB
        binding.aiChatFab.setOnClickListener {
            // Will implement AI chat functionality in a future step
            Log.d(TAG, "AI chat FAB clicked")
        }
    }

    private fun setupQuickActionsRecyclerView() {
        // Set up the RecyclerView with adapter
        binding.quickActionsRecyclerView.apply {
            setHasFixedSize(true)
            adapter = quickActionsAdapter
        }
    }

    private fun setupFeaturedTopicsRecyclerView() {
        // Set up the RecyclerView with adapter
        binding.featuredTopicsRecyclerView.apply {
            setHasFixedSize(true)
            adapter = featuredTopicsAdapter

            // Add padding decoration for better spacing
            val padding = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4
            addItemDecoration(object : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: android.graphics.Rect, view: View, parent: androidx.recyclerview.widget.RecyclerView, state: androidx.recyclerview.widget.RecyclerView.State) {
                    // Add padding to all items except the first one
                    if (parent.getChildAdapterPosition(view) != 0) {
                        outRect.left = padding
                    }
                }
            })
        }
    }

    private fun setupRecentSearchesRecyclerView() {
        // Set up the RecyclerView with adapter
        binding.recentSearchesRecyclerView.apply {
            setHasFixedSize(true)
            adapter = recentSearchesAdapter
        }

        // Set up clear searches button
        binding.clearSearchesButton.setOnClickListener {
            Log.d(TAG, "Clear searches button clicked")
            viewModel.clearRecentSearches()
        }
    }

    // Click handlers for list items

    private fun handleQuickActionClick(action: QuickAction) {
        Log.d(TAG, "Quick action clicked: ${action.title}")
        // Stub for quick action handling - will implement in a future step
        // Could navigate to feature or show appropriate UI based on action.id
    }

    private fun handleTopicClick(topic: HealthTopic) {
        Log.d(TAG, "Health topic clicked: ${topic.title}")
        // Stub for topic details - will implement in a future step
        // Could navigate to topic details screen with topic.id as parameter
    }

    private fun handleRecentSearchClick(query: String) {
        Log.d(TAG, "Recent search clicked: $query")
        // Set search query and perform search
        binding.searchEditText.setText(query)
        viewModel.setSearchQuery(query)
    }

    // UI state management methods

    private fun updateContentVisibilityForSearchState(isSearchActive: Boolean) {
        // When search is active, hide the regular content and show search results
        binding.quickActionsTitle.isVisible = !isSearchActive
        binding.quickActionsRecyclerView.isVisible = !isSearchActive
        binding.featuredTopicsTitle.isVisible = !isSearchActive
        binding.featuredTopicsRecyclerView.isVisible = !isSearchActive

        // Always show recent searches section since it's related to search
    }

    private fun updateQuickActionsVisibility(actions: List<QuickAction>) {
        val isVisible = actions.isNotEmpty() && binding.searchEditText.text.isNullOrEmpty()
        binding.quickActionsTitle.isVisible = isVisible
        binding.quickActionsRecyclerView.isVisible = isVisible
    }

    private fun updateFeaturedTopicsVisibility(topics: List<HealthTopic>) {
        val isVisible = topics.isNotEmpty() && binding.searchEditText.text.isNullOrEmpty()
        binding.featuredTopicsTitle.isVisible = isVisible
        binding.featuredTopicsRecyclerView.isVisible = isVisible
    }

    private fun updateRecentSearchesVisibility(searches: List<String>) {
        val isVisible = searches.isNotEmpty()
        binding.recentSearchesTitle.isVisible = isVisible
        binding.recentSearchesRecyclerView.isVisible = isVisible
        binding.clearSearchesButton.isVisible = isVisible
    }

    private fun updateLoadingState(isLoading: Boolean) {
        // You could add a progress indicator to your layout and control its visibility here
        // binding.progressIndicator.isVisible = isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "DiscoverFragment"
    }
}