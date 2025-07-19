package com.example.health_assistant.features.discover.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.health_assistant.R
import com.example.health_assistant.databinding.FragmentBookmarksBinding
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment for displaying bookmarked content and reading history
 * Supports filtering by content type and category organization
 */
@AndroidEntryPoint
class BookmarksFragment : Fragment() {

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookmarksViewModel by viewModels()
    private lateinit var bookmarksAdapter: BookmarksAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupObservers()
        setupClickListeners()
        
        // Load initial bookmarks
        viewModel.loadBookmarks()
    }

    private fun setupUI() {
        setupRecyclerView()
        setupTabs()
        setupToolbar()
    }

    private fun setupRecyclerView() {
        bookmarksAdapter = BookmarksAdapter(
            onItemClick = { content -> navigateToContent(content) },
            onBookmarkClick = { content -> viewModel.removeBookmark(content) },
            onShareClick = { content -> shareContent(content) }
        )

        binding.bookmarksRecyclerView.apply {
            adapter = bookmarksAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupTabs() {
        binding.contentTypeTabs.apply {
            addTab(newTab().setText("All"))
            addTab(newTab().setText("Articles"))
            addTab(newTab().setText("News"))
            addTab(newTab().setText("Videos"))
            
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val contentType = when (tab?.position) {
                        0 -> null // All
                        1 -> "article"
                        2 -> "news"
                        3 -> "video"
                        else -> null
                    }
                    viewModel.filterByContentType(contentType)
                }
                
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            
            inflateMenu(R.menu.bookmarks_menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_clear_all -> {
                        viewModel.clearAllBookmarks()
                        true
                    }
                    R.id.action_sort -> {
                        showSortOptions()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bookmarksState.collect { state ->
                handleBookmarksState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.readingHistoryState.collect { state ->
                handleReadingHistoryState(state)
            }
        }
    }

    private fun setupClickListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshBookmarks()
        }

        binding.readingHistoryButton.setOnClickListener {
            viewModel.toggleReadingHistoryView()
        }
    }

    private fun handleBookmarksState(state: BookmarksViewModel.BookmarksState) {
        binding.swipeRefreshLayout.isRefreshing = false
        
        when (state) {
            is BookmarksViewModel.BookmarksState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.emptyStateLayout.visibility = View.GONE
                binding.bookmarksRecyclerView.visibility = View.GONE
            }
            
            is BookmarksViewModel.BookmarksState.Success -> {
                binding.progressBar.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.GONE
                binding.bookmarksRecyclerView.visibility = View.VISIBLE
                
                bookmarksAdapter.submitList(state.bookmarks)
                updateBookmarkCount(state.bookmarks.size)
            }
            
            is BookmarksViewModel.BookmarksState.Empty -> {
                binding.progressBar.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.bookmarksRecyclerView.visibility = View.GONE
                
                binding.emptyStateTitle.text = getString(R.string.no_bookmarks_title)
                binding.emptyStateMessage.text = getString(R.string.no_bookmarks_message)
            }
            
            is BookmarksViewModel.BookmarksState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.bookmarksRecyclerView.visibility = View.GONE
                
                binding.emptyStateTitle.text = getString(R.string.error_loading_bookmarks)
                binding.emptyStateMessage.text = state.message
            }
        }
    }

    private fun handleReadingHistoryState(state: BookmarksViewModel.ReadingHistoryState) {
        when (state) {
            is BookmarksViewModel.ReadingHistoryState.Success -> {
                // Update reading history indicators in the adapter
                bookmarksAdapter.updateReadingHistory(state.history)
            }
            else -> {
                // Handle other states if needed
            }
        }
    }

    private fun navigateToContent(content: DiscoverContent) {
        when (content) {
            is DiscoverContent.Article -> {
                findNavController().navigate(
                    BookmarksFragmentDirections.actionBookmarksFragmentToArticleReaderFragment(
                        content.id
                    )
                )
            }
            is DiscoverContent.News -> {
                findNavController().navigate(
                    BookmarksFragmentDirections.actionBookmarksFragmentToArticleReaderFragment(
                        content.id
                    )
                )
            }
            is DiscoverContent.Video -> {
                findNavController().navigate(
                    BookmarksFragmentDirections.actionBookmarksFragmentToVideoPlayerFragment(
                        content.id
                    )
                )
            }
        }
    }

    private fun shareContent(content: DiscoverContent) {
        val sharingBottomSheet = ContentSharingBottomSheet.newInstance(content)
        sharingBottomSheet.show(childFragmentManager, "ContentSharingBottomSheet")
    }

    private fun updateBookmarkCount(count: Int) {
        binding.bookmarkCountText.text = resources.getQuantityString(
            R.plurals.bookmark_count,
            count,
            count
        )
    }

    private fun showSortOptions() {
        val options = arrayOf(
            getString(R.string.sort_by_date_added),
            getString(R.string.sort_by_date_published),
            getString(R.string.sort_by_content_type),
            getString(R.string.sort_by_category)
        )

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.sort_bookmarks))
            .setItems(options) { _, which ->
                val sortOption = when (which) {
                    0 -> BookmarksViewModel.SortOption.DATE_ADDED
                    1 -> BookmarksViewModel.SortOption.DATE_PUBLISHED
                    2 -> BookmarksViewModel.SortOption.CONTENT_TYPE
                    3 -> BookmarksViewModel.SortOption.CATEGORY
                    else -> BookmarksViewModel.SortOption.DATE_ADDED
                }
                viewModel.sortBookmarks(sortOption)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}