package com.example.health_assistant.features.discover

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.health_assistant.R
import com.example.health_assistant.databinding.FragmentDiscoverBinding
import com.example.health_assistant.features.discover.adapters.FeaturedTopicsAdapter
import com.example.health_assistant.features.discover.adapters.QuickActionsAdapter
import com.example.health_assistant.features.discover.adapters.RecentSearchesAdapter
import com.example.health_assistant.features.discover.adapters.SearchResultsAdapter
import com.example.health_assistant.features.discover.model.HealthTopic
import com.example.health_assistant.features.discover.model.QuickAction
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.snackbar.Snackbar
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

    private val searchResultsAdapter by lazy {
        SearchResultsAdapter { topic -> handleTopicClick(topic) }
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

        // Apply premium theme effects
        applyPremiumThemeEffects()

        // Initialize UI elements and set up any event listeners
        setupUI()

        // Observe ViewModel state changes
        observeViewModelState()
    }

    /**
     * Apply premium visual effects to match the HomeFragment theme
     */
    private fun applyPremiumThemeEffects() {
        // Animate decorative shapes
        animateDecorativeShapes()

        // Apply elevation overlay to FAB for better visibility on light/dark themes
        binding.aiChatFab.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(),
            R.color.colorPrimaryGradientStart
        )

        // Apply custom elevation to cards for consistent appearance
        binding.searchCard.cardElevation = resources.getDimension(R.dimen.card_elevation_small)
    }

    /**
     * Animate decorative shapes for a subtle premium effect
     */
    private fun animateDecorativeShapes() {
        // Subtle rotation animation for top decorative shape
        binding.decorativeShape1?.let { shape ->
            ObjectAnimator.ofFloat(shape, "rotation", 0f, 360f).apply {
                duration = 120000 // 2 minutes full rotation
                repeatCount = ObjectAnimator.INFINITE
                interpolator = DecelerateInterpolator()
                start()
            }
        }

        // Subtle scale animation for bottom decorative shape
        binding.decorativeShape2?.let { shape ->
            ObjectAnimator.ofFloat(shape, "scaleX", 0.95f, 1.05f).apply {
                duration = 8000 // 8 seconds per cycle
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                interpolator = DecelerateInterpolator()
                start()
            }

            ObjectAnimator.ofFloat(shape, "scaleY", 0.95f, 1.05f).apply {
                duration = 8000 // 8 seconds per cycle
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                interpolator = DecelerateInterpolator()
                start()
            }
        }
    }

    private fun observeViewModelState() {
        // Use Kotlin coroutines to collect StateFlow updates
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe search query changes
                launch {
                    viewModel.searchQuery.collect { query ->
                        Log.d(TAG, "Search query updated: $query")
                        if (binding.searchEditText.text.toString() != query) {
                            binding.searchEditText.setText(query)
                        }

                        // Update the search state UI
                        updateSearchStateUI(query, viewModel.searchResults.value)
                    }
                }

                // Observe search results
                launch {
                    viewModel.searchResults.collect { results ->
                        Log.d(TAG, "Search results updated: ${results.size} items")
                        searchResultsAdapter.submitList(results)

                        // Update the search state UI
                        updateSearchStateUI(viewModel.searchQuery.value, results)
                    }
                }

                // Observe featured topics
                launch {
                    viewModel.featuredTopics.collect { topics ->
                        Log.d(TAG, "Featured topics updated: ${topics.size} items")
                        featuredTopicsAdapter.submitList(topics)
                    }
                }

                // Observe quick actions
                launch {
                    viewModel.quickActions.collect { actions ->
                        Log.d(TAG, "Quick actions updated: ${actions.size} items")
                        quickActionsAdapter.submitList(actions)
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
                            showErrorMessage(it)
                            viewModel.clearError()
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
        setupSearchResultsRecyclerView()
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

            // Add subtle animation feedback for premium feel
            it.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
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

    private fun setupSearchResultsRecyclerView() {
        // Set up the RecyclerView with adapter for search results
        binding.searchResultsRecyclerView.apply {
            setHasFixedSize(true)
            adapter = searchResultsAdapter
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

        // Add ripple effect for premium feel
        Snackbar.make(
            binding.root,
            "Opening ${action.title}...",
            Snackbar.LENGTH_SHORT
        ).apply {
            setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.colorPrimaryGradientEnd))
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            show()
        }
    }

    private fun handleTopicClick(topic: HealthTopic) {
        Log.d(TAG, "Health topic clicked: ${topic.title}")
        // Stub for topic details - will implement in a future step
        // Could navigate to topic details screen with topic.id as parameter

        // Add ripple effect for premium feel
        Snackbar.make(
            binding.root,
            "Opening \"${topic.title}\"",
            Snackbar.LENGTH_SHORT
        ).apply {
            setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.colorPrimaryGradientEnd))
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            show()
        }
    }

    private fun handleRecentSearchClick(query: String) {
        Log.d(TAG, "Recent search clicked: $query")
        // Set search query and perform search
        binding.searchEditText.setText(query)
        viewModel.setSearchQuery(query)
    }

    // UI state management methods

    private fun updateSearchStateUI(query: String, results: List<HealthTopic>) {
        val isSearchActive = query.isNotEmpty()

        // Handle browse content visibility (quick actions and featured topics)
        binding.browseContentGroup.isVisible = !isSearchActive

        if (isSearchActive) {
            // If search is active, show search results or empty state
            val hasResults = results.isNotEmpty()

            binding.searchResultsTitle.isVisible = hasResults
            binding.searchResultsRecyclerView.isVisible = hasResults
            binding.emptyResultsCard.isVisible = !hasResults

            // Update empty state message with the query
            if (!hasResults) {
                binding.emptySearchMessage.text = "No results found for \"$query\". Try another search term or browse our featured topics below."
            }
        } else {
            // If search is not active, hide search results and empty state
            binding.searchResultsTitle.isVisible = false
            binding.searchResultsRecyclerView.isVisible = false
            binding.emptyResultsCard.isVisible = false
        }

        // Recent searches visibility is now independent of search state
        // It's controlled separately in updateRecentSearchesVisibility()
    }

    private fun updateRecentSearchesVisibility(searches: List<String>) {
        val isVisible = searches.isNotEmpty()
        binding.recentSearchesCard.isVisible = isVisible
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.progressIndicator.isVisible = isLoading
    }

    private fun showErrorMessage(errorMessage: String) {
        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG)
            .apply {
                setBackgroundTint(ContextCompat.getColor(context, R.color.healthPoor))
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "DiscoverFragment"
    }
}