package com.example.health_assistant.features.discover

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.health_assistant.databinding.FragmentDiscoverBinding
import com.example.health_assistant.features.discover.presentation.SimpleDiscoverViewModel
import com.example.health_assistant.features.discover.presentation.DiscoverUiState
import com.example.health_assistant.features.discover.presentation.VideosAdapter
import com.example.health_assistant.features.discover.presentation.NewsAdapter
import com.example.health_assistant.features.discover.presentation.ArticlesAdapter
import com.example.health_assistant.features.discover.domain.model.HealthContent
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DiscoverFragment : Fragment() {
    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SimpleDiscoverViewModel by viewModels()
    
    // Simple adapters for each section
    private lateinit var videosAdapter: VideosAdapter
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var articlesAdapter: ArticlesAdapter

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
        
        setupAdapters()
        setupClickListeners()
        setupObservers()
    }

    private fun setupAdapters() {
        // Videos adapter with click handling
        videosAdapter = VideosAdapter { content ->
            openContentInBrowser(content)
        }
        
        binding.recyclerViewVideos.apply {
            adapter = videosAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        // News adapter with click handling
        newsAdapter = NewsAdapter { content ->
            openContentInBrowser(content)
        }
        
        binding.recyclerViewNews.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        // Articles adapter with click handling
        articlesAdapter = ArticlesAdapter { content ->
            openContentInBrowser(content)
        }
        
        binding.recyclerViewArticles.apply {
            adapter = articlesAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupClickListeners() {
        // See All buttons - navigate to content list
        binding.buttonSeeAllVideos.setOnClickListener {
            navigateToContentList("VIDEO")
        }
        
        binding.buttonSeeAllNews.setOnClickListener {
            navigateToContentList("NEWS")
        }
        
        binding.buttonSeeAllArticles.setOnClickListener {
            navigateToContentList("ARTICLE")
        }

        // Pull to refresh functionality
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshContent()
        }

        // Retry button for error handling
        binding.buttonRetryError.setOnClickListener {
            viewModel.retry()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe UI state
                viewModel.uiState.collect { uiState ->
                    handleUiState(uiState)
                }
            }
        }
    }

    private fun handleUiState(uiState: DiscoverUiState) {
        // Handle loading state
        if (uiState.isLoading && uiState.sections == null) {
            showLoading()
            hideError()
            hideContent()
        } else {
            hideLoading()
        }
        
        // Handle refresh state
        binding.swipeRefreshLayout.isRefreshing = uiState.isRefreshing
        
        // Handle content
        uiState.sections?.let { sections ->
            displaySections(sections)
            hideError()
            showContent()
        }
        
        // Handle error state
        uiState.error?.let { error ->
            if (uiState.sections == null) {
                // Show full error screen if no content
                showError(error)
                hideContent()
            } else {
                // Show snackbar if we have content
                showErrorSnackbar(error)
            }
        }
    }

    private fun displaySections(sections: com.example.health_assistant.features.discover.domain.model.DiscoverSections) {
        // Update adapters with new data
        videosAdapter.submitList(sections.videos)
        newsAdapter.submitList(sections.news)
        articlesAdapter.submitList(sections.articles)
        
        // Show/hide empty states for each section
        binding.layoutVideosEmpty.visibility = if (sections.videos.isEmpty()) View.VISIBLE else View.GONE
        binding.layoutNewsEmpty.visibility = if (sections.news.isEmpty()) View.VISIBLE else View.GONE
        binding.layoutArticlesEmpty.visibility = if (sections.articles.isEmpty()) View.VISIBLE else View.GONE
        
        // Show/hide RecyclerViews based on content
        binding.recyclerViewVideos.visibility = if (sections.videos.isNotEmpty()) View.VISIBLE else View.GONE
        binding.recyclerViewNews.visibility = if (sections.news.isNotEmpty()) View.VISIBLE else View.GONE
        binding.recyclerViewArticles.visibility = if (sections.articles.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun showLoading() {
        binding.layoutLoading.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.layoutLoading.visibility = View.GONE
    }

    private fun showContent() {
        binding.layoutVideosSection.visibility = View.VISIBLE
        binding.layoutNewsSection.visibility = View.VISIBLE
        binding.layoutArticlesSection.visibility = View.VISIBLE
    }

    private fun hideContent() {
        binding.layoutVideosSection.visibility = View.GONE
        binding.layoutNewsSection.visibility = View.GONE
        binding.layoutArticlesSection.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.layoutError.visibility = View.VISIBLE
        binding.textErrorMessage.text = message
    }

    private fun hideError() {
        binding.layoutError.visibility = View.GONE
    }

    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Retry") { viewModel.retry() }
            .show()
    }



    /**
     * Open content in external browser or app
     */
    private fun openContentInBrowser(content: HealthContent) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content.sourceUrl))
            startActivity(intent)
            viewModel.onContentClick(content) // Log the interaction
        } catch (e: Exception) {
            showMessage("Unable to open content")
        }
    }

    private fun navigateToContentList(contentType: String) {
        try {
            val action = DiscoverFragmentDirections.actionDiscoverFragmentToContentListFragment(contentType)
            findNavController().navigate(action)
        } catch (e: Exception) {
            showMessage("Unable to open content list")
        }
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}