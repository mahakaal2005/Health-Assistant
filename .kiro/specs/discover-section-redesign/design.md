# Design Document

## Overview

The Discover Section Redesign simplifies the current complex mixed-content feed into three distinct, horizontally-scrollable sections: Videos, News, and Articles. This design reduces cognitive load, improves content discoverability, and maintains the existing MVVM architecture while significantly simplifying the UI components and data flow.

## Architecture

### Simplified Architecture
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  DiscoverFragment │────│ DiscoverViewModel │────│ DiscoverRepository │
│   (3 Sections)   │    │  (Simplified)     │    │  (Existing)       │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                         │
                        ┌────────────────────────────────┼────────────────────────────────┐
                        │                                │                                │
                ┌───────▼────────┐              ┌───────▼────────┐              ┌───────▼────────┐
                │  Room Database │              │ Firebase Store │              │ Content Cache  │
                │  (Offline)     │              │  (Remote)      │              │  (Images/Video)│
                └────────────────┘              └────────────────┘              └────────────────┘
```

### Simplified Data Flow
```
1. User opens Discover → Load 3 sections simultaneously
2. Each section → Load 5 items max for preview
3. User taps "See All" → Navigate to dedicated list screen
4. User taps content → Navigate to reader/player
5. Background sync → Update all sections
```

## Components and Interfaces

### 1. Simplified UI Structure

#### Main Discover Fragment Layout
```xml
<!-- Simplified fragment_discover.xml -->
<ScrollView>
    <LinearLayout orientation="vertical">
        
        <!-- Search Bar (Optional) -->
        <SearchView />
        
        <!-- Videos Section -->
        <LinearLayout>
            <TextView text="Videos" />
            <Button text="See All" />
        </LinearLayout>
        <RecyclerView horizontal="true" />
        
        <!-- News Section -->
        <LinearLayout>
            <TextView text="News" />
            <Button text="See All" />
        </LinearLayout>
        <RecyclerView horizontal="true" />
        
        <!-- Articles Section -->
        <LinearLayout>
            <TextView text="Articles" />
            <Button text="See All" />
        </LinearLayout>
        <RecyclerView horizontal="true" />
        
    </LinearLayout>
</ScrollView>
```

#### Section Item Layouts (Simplified)
```xml
<!-- Video Item (Horizontal Card) -->
<CardView width="280dp" height="200dp">
    <ImageView thumbnail />
    <TextView title maxLines="2" />
    <TextView duration />
    <ImageView playButton />
</CardView>

<!-- News Item (Horizontal Card) -->
<CardView width="300dp" height="180dp">
    <ImageView thumbnail />
    <TextView headline maxLines="2" />
    <TextView source />
    <TextView timeAgo />
</CardView>

<!-- Article Item (Horizontal Card) -->
<CardView width="280dp" height="160dp">
    <TextView title maxLines="2" />
    <TextView author />
    <TextView readingTime />
    <ProgressBar readingProgress />
</CardView>
```

### 2. Simplified Data Models

#### Unified Content Model (Simplified)
```kotlin
// Simplified content models - remove complex fields
data class VideoContent(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val duration: String, // Pre-formatted (e.g., "5:30")
    val expertName: String,
    val watchProgress: Float = 0f
)

data class NewsContent(
    val id: String,
    val headline: String,
    val imageUrl: String?,
    val source: String,
    val timeAgo: String, // Pre-formatted (e.g., "2 hours ago")
    val isBreaking: Boolean = false
)

data class ArticleContent(
    val id: String,
    val title: String,
    val author: String,
    val readingTime: String, // Pre-formatted (e.g., "5 min read")
    val readProgress: Float = 0f,
    val isBookmarked: Boolean = false
)

// Section data container
data class DiscoverSections(
    val videos: List<VideoContent>,
    val news: List<NewsContent>,
    val articles: List<ArticleContent>
)
```

### 3. Simplified ViewModel

#### DiscoverViewModel (Simplified)
```kotlin
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: DiscoverRepository
) : ViewModel() {
    
    // Single state for all sections
    private val _sectionsData = MutableStateFlow<Result<DiscoverSections>>(Result.Loading)
    val sectionsData: StateFlow<Result<DiscoverSections>> = _sectionsData.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    init {
        loadAllSections()
    }
    
    fun loadAllSections() {
        viewModelScope.launch {
            _sectionsData.value = Result.Loading
            
            try {
                // Load 5 items per section for preview
                val videos = repository.getVideos(limit = 5)
                val news = repository.getNews(limit = 5)
                val articles = repository.getArticles(limit = 5)
                
                val sections = DiscoverSections(
                    videos = videos.map { it.toVideoContent() },
                    news = news.map { it.toNewsContent() },
                    articles = articles.map { it.toArticleContent() }
                )
                
                _sectionsData.value = Result.Success(sections)
            } catch (e: Exception) {
                _sectionsData.value = Result.Error(e)
            }
        }
    }
    
    fun refreshContent() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadAllSections()
            _isRefreshing.value = false
        }
    }
    
    fun toggleBookmark(articleId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(articleId)
            loadAllSections() // Refresh to update bookmark state
        }
    }
}
```

### 4. Simplified Fragment Implementation

#### DiscoverFragment (Simplified)
```kotlin
@AndroidEntryPoint
class DiscoverFragment : Fragment() {
    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DiscoverViewModel by viewModels()
    
    // Simple adapters for each section
    private lateinit var videosAdapter: VideosAdapter
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var articlesAdapter: ArticlesAdapter
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAdapters()
        setupClickListeners()
        observeData()
    }
    
    private fun setupAdapters() {
        videosAdapter = VideosAdapter { video ->
            navigateToVideoPlayer(video.id)
        }
        
        newsAdapter = NewsAdapter { news ->
            navigateToNewsReader(news.id)
        }
        
        articlesAdapter = ArticlesAdapter(
            onArticleClick = { article -> navigateToArticleReader(article.id) },
            onBookmarkClick = { article -> viewModel.toggleBookmark(article.id) }
        )
        
        binding.recyclerViewVideos.adapter = videosAdapter
        binding.recyclerViewNews.adapter = newsAdapter
        binding.recyclerViewArticles.adapter = articlesAdapter
    }
    
    private fun setupClickListeners() {
        binding.buttonSeeAllVideos.setOnClickListener {
            navigateToVideosList()
        }
        
        binding.buttonSeeAllNews.setOnClickListener {
            navigateToNewsList()
        }
        
        binding.buttonSeeAllArticles.setOnClickListener {
            navigateToArticlesList()
        }
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshContent()
        }
    }
    
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sectionsData.collect { result ->
                when (result) {
                    is Result.Success -> {
                        displaySections(result.data)
                        hideLoading()
                    }
                    is Result.Error -> {
                        showError(result.exception?.message ?: "Failed to load content")
                        hideLoading()
                    }
                    is Result.Loading -> {
                        showLoading()
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isRefreshing.collect { isRefreshing ->
                binding.swipeRefreshLayout.isRefreshing = isRefreshing
            }
        }
    }
    
    private fun displaySections(sections: DiscoverSections) {
        videosAdapter.submitList(sections.videos)
        newsAdapter.submitList(sections.news)
        articlesAdapter.submitList(sections.articles)
        
        // Show/hide empty states
        binding.layoutVideosEmpty.visibility = if (sections.videos.isEmpty()) View.VISIBLE else View.GONE
        binding.layoutNewsEmpty.visibility = if (sections.news.isEmpty()) View.VISIBLE else View.GONE
        binding.layoutArticlesEmpty.visibility = if (sections.articles.isEmpty()) View.VISIBLE else View.GONE
    }
}
```

### 5. Simple Adapters

#### VideosAdapter (Simplified)
```kotlin
class VideosAdapter(
    private val onVideoClick: (VideoContent) -> Unit
) : ListAdapter<VideoContent, VideosAdapter.ViewHolder>(VideoDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVideoHorizontalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: ItemVideoHorizontalBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(video: VideoContent) {
            binding.apply {
                textVideoTitle.text = video.title
                textVideoDuration.text = video.duration
                textVideoExpert.text = video.expertName
                
                // Load thumbnail
                imageVideoThumbnail.load(video.thumbnailUrl)
                
                // Show progress if exists
                if (video.watchProgress > 0) {
                    progressWatching.visibility = View.VISIBLE
                    progressWatching.progress = (video.watchProgress * 100).toInt()
                } else {
                    progressWatching.visibility = View.GONE
                }
                
                root.setOnClickListener { onVideoClick(video) }
            }
        }
    }
}
```

## Data Models

### Simplified Repository Interface
```kotlin
interface DiscoverRepository {
    // Simplified methods - remove complex filtering and search
    suspend fun getVideos(limit: Int = 20): List<EducationalVideo>
    suspend fun getNews(limit: Int = 20): List<HealthNews>
    suspend fun getArticles(limit: Int = 20): List<HealthArticle>
    
    suspend fun toggleBookmark(contentId: String): Boolean
    suspend fun syncContent(): Result<Unit>
    
    // Navigation methods for "See All" screens
    suspend fun getAllVideos(): List<EducationalVideo>
    suspend fun getAllNews(): List<HealthNews>
    suspend fun getAllArticles(): List<HealthArticle>
}
```

### Content Mapping Extensions
```kotlin
// Simple mapping functions
fun EducationalVideo.toVideoContent() = VideoContent(
    id = id,
    title = title,
    thumbnailUrl = thumbnailUrl,
    duration = formatDuration(durationSeconds),
    expertName = expertName,
    watchProgress = watchProgress
)

fun HealthNews.toNewsContent() = NewsContent(
    id = id,
    headline = headline,
    imageUrl = imageUrl,
    source = sourcePublication,
    timeAgo = formatTimeAgo(publishedDate),
    isBreaking = isBreakingNews
)

fun HealthArticle.toArticleContent() = ArticleContent(
    id = id,
    title = title,
    author = authorName,
    readingTime = "${readingTimeMinutes} min read",
    readProgress = readProgress,
    isBookmarked = isBookmarked
)
```

## Error Handling

### Simplified Error Strategy
```kotlin
// Simple error handling - no complex retry logic
sealed class DiscoverError {
    object NetworkError : DiscoverError()
    object CacheError : DiscoverError()
    data class UnknownError(val message: String) : DiscoverError()
}

fun handleError(error: DiscoverError): String {
    return when (error) {
        is DiscoverError.NetworkError -> "Check your internet connection"
        is DiscoverError.CacheError -> "Unable to load cached content"
        is DiscoverError.UnknownError -> error.message
    }
}
```

## Testing Strategy

### Simplified Testing Approach
```kotlin
// Focus on core functionality only
@Test
fun `loadAllSections returns data for all three sections`() = runTest {
    // Given
    val mockVideos = listOf(mockVideo())
    val mockNews = listOf(mockNews())
    val mockArticles = listOf(mockArticle())
    
    whenever(repository.getVideos(5)).thenReturn(mockVideos)
    whenever(repository.getNews(5)).thenReturn(mockNews)
    whenever(repository.getArticles(5)).thenReturn(mockArticles)
    
    // When
    viewModel.loadAllSections()
    
    // Then
    val result = viewModel.sectionsData.value
    assertThat(result).isInstanceOf(Result.Success::class.java)
    val sections = (result as Result.Success).data
    assertThat(sections.videos).hasSize(1)
    assertThat(sections.news).hasSize(1)
    assertThat(sections.articles).hasSize(1)
}

@Test
fun `clicking see all navigates to dedicated screen`() {
    // Test navigation to dedicated screens
    fragment.binding.buttonSeeAllVideos.performClick()
    verify(navController).navigate(R.id.action_discover_to_videos_list)
}
```

## Implementation Simplifications

### Key Simplifications from Current Implementation

1. **Remove Complex Search**: No real-time search, suggestions, or advanced filtering
2. **Remove Analytics**: No A/B testing, recommendation engine, or complex tracking
3. **Remove Content Reporting**: No content validation or reporting system
4. **Simplified Error Handling**: Basic error messages without retry logic
5. **Remove Complex State Management**: Single state object instead of multiple flows
6. **Simplified Adapters**: One adapter per content type instead of mixed adapter
7. **Remove Advanced Features**: No offline downloads, content sharing, or deep linking
8. **Simplified Navigation**: Direct navigation without complex routing
9. **Remove Credibility System**: Basic source display without scoring
10. **Simplified Caching**: Basic cache without intelligent management

### Benefits of Simplification

- **Reduced Complexity**: Easier to maintain and debug
- **Better Performance**: Fewer components and simpler data flow
- **Improved UX**: Clear content organization and navigation
- **Faster Development**: Less code to write and test
- **Better Accessibility**: Simpler UI structure is more accessible
- **Reduced Memory Usage**: Fewer objects and simpler state management