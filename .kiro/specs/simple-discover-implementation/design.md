# Simple Discover Implementation Design

## Overview

This design creates a simplified discover section that displays health content from APIs in a clean, sectioned interface with horizontal scrolling cards. The focus is on basic functionality without complex domain models, analytics, or advanced features.

## Architecture

### Simplified Architecture Pattern
- **Presentation Layer**: Fragment + ViewModel + RecyclerView Adapters
- **Data Layer**: Repository + Remote Data Source + API Services
- **No Domain Layer**: Direct data flow from API to UI models

### Key Components
1. **DiscoverFragment**: Main UI displaying sectioned content
2. **SimpleDiscoverViewModel**: Manages UI state and API calls
3. **DiscoverRepository**: Handles API calls and data aggregation
4. **API Services**: Existing NewsAPI, Guardian, YouTube services
5. **Simple Data Models**: Basic data classes for UI display

## Components and Interfaces

### 1. Data Models (Simplified)

```kotlin
// Simple data classes for UI display
data class HealthContent(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val sourceUrl: String,
    val publishedDate: String,
    val contentType: ContentType // ARTICLE, NEWS, VIDEO
)

enum class ContentType {
    ARTICLE, NEWS, VIDEO
}

data class DiscoverSections(
    val articles: List<HealthContent>,
    val news: List<HealthContent>,
    val videos: List<HealthContent>
)
```

### 2. Repository Interface

```kotlin
interface SimpleDiscoverRepository {
    suspend fun getDiscoverContent(): Result<DiscoverSections>
    suspend fun refreshContent(): Result<DiscoverSections>
}
```

### 3. ViewModel

```kotlin
class SimpleDiscoverViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()
    
    fun loadContent()
    fun refreshContent()
    fun onContentClick(content: HealthContent)
}

data class DiscoverUiState(
    val isLoading: Boolean = false,
    val sections: DiscoverSections? = null,
    val error: String? = null,
    val isRefreshing: Boolean = false
)
```

### 4. Fragment Layout Structure

```xml
<ScrollView>
    <LinearLayout orientation="vertical">
        <!-- Articles Section -->
        <LinearLayout>
            <TextView text="Health Articles" />
            <Button text="See All" />
        </LinearLayout>
        <RecyclerView horizontal="true" />
        
        <!-- News Section -->
        <LinearLayout>
            <TextView text="Health News" />
            <Button text="See All" />
        </LinearLayout>
        <RecyclerView horizontal="true" />
        
        <!-- Videos Section -->
        <LinearLayout>
            <TextView text="Health Videos" />
            <Button text="See All" />
        </LinearLayout>
        <RecyclerView horizontal="true" />
    </LinearLayout>
</ScrollView>
```

## Data Models

### Simplified Content Model
Instead of complex domain models, use a single `HealthContent` data class that can represent articles, news, and videos with common fields needed for display.

### API Response Mapping
Convert API responses directly to `HealthContent` objects in the repository layer, eliminating the need for separate domain models.

## Error Handling

### Simple Error Strategy
- Show toast messages for API failures
- Display retry button when all content fails to load
- Continue showing partial content when some APIs fail
- Use pull-to-refresh for manual retry

### Error States
1. **Loading State**: Show progress indicators
2. **Partial Failure**: Show available content + error toast
3. **Complete Failure**: Show error message with retry button
4. **Network Error**: Show network-specific error message

## Testing Strategy

### Minimal Testing Approach
- **Repository Tests**: Test API integration and data mapping
- **ViewModel Tests**: Test state management and error handling
- **No UI Tests**: Keep it simple, focus on functionality

### Test Coverage
- API response mapping
- Error handling scenarios
- Loading state management
- Content refresh functionality

## Implementation Notes

### Existing Code Reuse
- Keep existing API services (NewsAPI, Guardian, YouTube)
- Reuse ApiKeyManager for API key management
- Simplify HealthContentRemoteDataSource to return HealthContent directly

### Code Removal
- Remove all analytics components
- Remove bookmark functionality
- Remove complex error handling classes
- Remove validation and credibility features
- Remove offline caching
- Remove deep linking and sharing features

### UI Simplifications
- Use simple card layouts for content items
- Implement basic horizontal RecyclerViews
- Use standard Material Design components
- No custom animations or complex interactions

## Navigation

### Simple Navigation Flow
1. **Discover Screen**: Main content browsing
2. **External Browser**: Open content URLs in browser
3. **No Detail Screens**: Keep it simple, use external apps

### Content Opening Strategy
- Articles: Open source URL in browser
- News: Open source URL in browser  
- Videos: Open YouTube URL in YouTube app or browser

## Performance Considerations

### API Optimization
- Limit API calls to essential endpoints
- Use reasonable page sizes (10-20 items per section)
- Implement simple caching with short TTL (5-10 minutes)
- No background sync or complex scheduling

### UI Performance
- Use ViewBinding for layouts
- Implement basic image loading with Glide
- Use DiffUtil for RecyclerView updates
- Keep adapter logic simple