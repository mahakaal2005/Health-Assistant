package com.example.health_assistant.features.discover.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.health_assistant.databinding.FragmentContentListBinding
import com.example.health_assistant.features.discover.domain.model.ContentType
import com.example.health_assistant.features.discover.domain.model.HealthContent
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment that displays a full list of content for a specific type (Articles, News, or Videos)
 */
@AndroidEntryPoint
class ContentListFragment : Fragment() {
    private var _binding: FragmentContentListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SimpleDiscoverViewModel by viewModels()
    private val args: ContentListFragmentArgs by navArgs()
    
    private lateinit var contentAdapter: ContentListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupAdapter()
        setupClickListeners()
        setupObservers()
    }

    private fun setupToolbar() {
        val title = when (args.contentType) {
            "ARTICLE" -> "Health Articles"
            "NEWS" -> "Health News"
            "VIDEO" -> "Health Videos"
            else -> "Health Content"
        }
        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupAdapter() {
        contentAdapter = ContentListAdapter { content ->
            openContentInBrowser(content)
        }
        
        binding.recyclerViewContent.apply {
            adapter = contentAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
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
            val contentList = when (args.contentType) {
                "ARTICLE" -> sections.articles
                "NEWS" -> sections.news
                "VIDEO" -> sections.videos
                else -> emptyList()
            }
            
            displayContent(contentList)
            hideError()
            showContent()
        }
        
        // Handle error state
        uiState.error?.let { error ->
            if (uiState.sections == null) {
                showError(error)
                hideContent()
            } else {
                showErrorSnackbar(error)
            }
        }
    }

    private fun displayContent(contentList: List<HealthContent>) {
        contentAdapter.submitList(contentList)
        
        // Show/hide empty state
        if (contentList.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.recyclerViewContent.visibility = View.GONE
            
            val emptyMessage = when (args.contentType) {
                "ARTICLE" -> "No articles available"
                "NEWS" -> "No news available"
                "VIDEO" -> "No videos available"
                else -> "No content available"
            }
            binding.textEmptyMessage.text = emptyMessage
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.recyclerViewContent.visibility = View.VISIBLE
        }
    }

    private fun showLoading() {
        binding.layoutLoading.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.layoutLoading.visibility = View.GONE
    }

    private fun showContent() {
        binding.recyclerViewContent.visibility = View.VISIBLE
    }

    private fun hideContent() {
        binding.recyclerViewContent.visibility = View.GONE
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

    private fun openContentInBrowser(content: HealthContent) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content.sourceUrl))
            startActivity(intent)
            viewModel.onContentClick(content)
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Unable to open content", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}