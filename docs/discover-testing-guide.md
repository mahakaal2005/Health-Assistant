# Discover Feature Testing Guide

## Overview

This guide covers the comprehensive testing strategy for the Discover feature, including unit tests, integration tests, and UI tests. The testing approach ensures reliability, performance, and user experience quality.

## Test Structure

### Test Organization

```
app/src/test/java/com/example/health_assistant/features/discover/
├── data/
│   ├── DiscoverDaoTest.kt                           # Database operations
│   ├── DiscoverRepositoryImplTest.kt                # Repository logic (basic)
│   ├── DiscoverRepositoryImplComprehensiveTest.kt   # Repository logic (comprehensive)
│   ├── DiscoverFirebaseSyncIntegrationTest.kt       # Firebase sync integration
│   ├── DiscoverDatabaseIntegrationTest.kt           # Database integration
│   ├── cache/
│   │   └── ContentCacheManagerTest.kt               # Cache management
│   └── entity/
│       └── DiscoverEntitiesTest.kt                  # Entity validation
├── domain/
│   ├── DiscoverManagerTest.kt                       # Business logic coordinator
│   ├── analytics/
│   │   ├── AnalyticsManagerTest.kt                  # Analytics tracking
│   │   ├── ABTestManagerTest.kt                     # A/B testing
│   │   └── RecommendationEngineTest.kt              # Content recommendations
│   ├── error/
│   │   ├── ErrorMapperTest.kt                       # Error handling
│   │   └── RetryManagerTest.kt                      # Retry logic
│   ├── model/
│   │   └── ContentValidationResultTest.kt           # Model validation
│   ├── usecase/
│   │   ├── SimpleUseCasesTest.kt                    # Use case logic
│   │   ├── AnalyticsTrackingUseCaseTest.kt          # Analytics use cases
│   │   └── ReportContentUseCaseTest.kt              # Content reporting
│   └── validation/
│       ├── ContentCredibilityValidatorTest.kt       # Content validation
│       └── ContentCredibilityIntegrationTest.kt     # Validation integration
├── navigation/
│   ├── DiscoverNavigationHelperTest.kt              # Navigation logic
│   ├── DiscoverDeepLinkHandlerTest.kt               # Deep linking
│   └── DiscoverNavigationIntegrationTest.kt         # Navigation integration
├── presentation/
│   ├── DiscoverViewModelTest.kt                     # Main ViewModel
│   ├── ArticleReaderViewModelTest.kt                # Article reading
│   ├── VideoPlayerViewModelTest.kt                  # Video playback
│   ├── BookmarksViewModelTest.kt                    # Bookmark management
│   ├── DiscoverContentAdapterTest.kt                # RecyclerView adapter
│   ├── DiscoverContentUtilsTest.kt                  # UI utilities
│   ├── DiscoverSearchFunctionalityTest.kt           # Search functionality
│   ├── ContentSharingManagerTest.kt                 # Content sharing
│   ├── DeepLinkManagerTest.kt                       # Deep link handling
│   └── ErrorStateViewTest.kt                        # Error state UI
└── workers/
    ├── ContentSyncWorkerTest.kt                     # Background sync
    ├── ContentSyncSchedulerTest.kt                  # Sync scheduling
    └── SyncStatusManagerTest.kt                     # Sync status tracking
```

### UI Tests (androidTest)

```
app/src/androidTest/java/com/example/health_assistant/features/discover/
└── ui/
    ├── DiscoverContentLoadingUITest.kt              # Content loading flows
    ├── DiscoverBookmarkingUITest.kt                 # Bookmark interactions
    └── DiscoverSearchUITest.kt                      # Search interactions
```

## Running Tests

### Unit Tests

Run all unit tests for the Discover feature:

```bash
# Run all Discover unit tests
./gradlew testDebugUnitTest --tests "*discover*"

# Run specific test classes
./gradlew testDebugUnitTest --tests "DiscoverRepositoryImplComprehensiveTest"
./gradlew testDebugUnitTest --tests "DiscoverViewModelTest"
./gradlew testDebugUnitTest --tests "DiscoverManagerTest"

# Run tests with coverage
./gradlew testDebugUnitTest jacocoTestReport
```

### Integration Tests

Run integration tests that test component interactions:

```bash
# Run Firebase sync integration tests
./gradlew testDebugUnitTest --tests "DiscoverFirebaseSyncIntegrationTest"

# Run database integration tests
./gradlew testDebugUnitTest --tests "DiscoverDatabaseIntegrationTest"

# Run content validation integration tests
./gradlew testDebugUnitTest --tests "ContentCredibilityIntegrationTest"
```

### UI Tests

Run UI tests on device or emulator:

```bash
# Run all Discover UI tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.example.health_assistant.features.discover.ui

# Run specific UI test classes
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.health_assistant.features.discover.ui.DiscoverContentLoadingUITest

# Run UI tests with specific device configuration
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.size=large
```

## Test Categories

### 1. Unit Tests

#### Repository Tests
- **Purpose**: Test data access logic, caching, and sync operations
- **Key Areas**:
  - Offline-first data access patterns
  - Firebase sync operations
  - Error handling and fallback mechanisms
  - Data transformation and mapping
  - Cache management

```kotlin
@Test
fun `getHealthArticles returns cached data first then syncs`() = runTest {
    // Given
    val cachedArticles = listOf(createTestHealthArticleEntity())
    coEvery { discoverDao.getHealthArticlesFlow(testUserId, null, 20) } returns flowOf(cachedArticles)
    
    // When
    val result = repository.getHealthArticles(testUserId, null, 20).first()
    
    // Then
    assertTrue(result is Result.Success)
    assertEquals(1, result.data.size)
    coVerify { discoverDao.getHealthArticlesFlow(testUserId, null, 20) }
}
```

#### ViewModel Tests
- **Purpose**: Test UI state management and user interaction handling
- **Key Areas**:
  - State transitions (loading, success, error)
  - User input validation
  - Content filtering and search
  - Bookmark management
  - Error recovery

```kotlin
@Test
fun `searchContent should update search state and results`() = runTest {
    val query = "test query"
    val mockResults = SearchResults(query = query, results = listOf(createMockArticle()))
    
    coEvery { discoverManager.searchContent(query, any()) } returns Result.Success(mockResults)
    
    viewModel.searchContent(query)
    testScheduler.advanceUntilIdle()
    
    assertEquals(query, viewModel.searchQuery.value)
    assertTrue(viewModel.isSearchActive.value)
}
```

#### Use Case Tests
- **Purpose**: Test individual business operations
- **Key Areas**:
  - Input validation
  - Business rule enforcement
  - Error handling
  - Data transformation

#### Domain Model Tests
- **Purpose**: Test business logic and validation rules
- **Key Areas**:
  - Content validation
  - Credibility scoring
  - Data consistency
  - Business rule enforcement

### 2. Integration Tests

#### Firebase Sync Integration
- **Purpose**: Test complete sync flow from Firebase to local database
- **Key Areas**:
  - Full sync operations
  - Incremental updates
  - Conflict resolution
  - Network failure handling
  - Data consistency

```kotlin
@Test
fun `full sync flow updates all content types`() = runTest {
    // Given - Mock Firebase data
    val firebaseArticles = listOf(createFirebaseArticle())
    mockFirestoreCollection("health_articles", firebaseArticles)
    
    // When - Trigger sync
    val syncResult = repository.syncContent(testUserId)
    
    // Then - Verify sync completed
    assertTrue(syncResult is Result.Success)
    coVerify { discoverDao.insertHealthArticles(any()) }
}
```

#### Database Integration
- **Purpose**: Test database operations with real Room database
- **Key Areas**:
  - CRUD operations
  - Query performance
  - Data relationships
  - Transaction handling
  - Migration testing

### 3. UI Tests

#### Content Loading Tests
- **Purpose**: Test content loading user flows
- **Key Areas**:
  - Initial loading states
  - Content display
  - Error states and recovery
  - Pull-to-refresh functionality
  - Pagination

```kotlin
@Test
fun discoverFragment_displaysContentAfterLoading() {
    composeTestRule.onNodeWithContentDescription("Discover").performClick()
    
    composeTestRule.waitUntil(timeoutMillis = 5000) {
        composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
    }
    
    composeTestRule.onNodeWithTag("content_recycler_view").assertIsDisplayed()
}
```

#### Bookmark Tests
- **Purpose**: Test bookmark functionality
- **Key Areas**:
  - Bookmark toggle interactions
  - Visual state changes
  - Bookmark management
  - Sync across devices

#### Search Tests
- **Purpose**: Test search functionality
- **Key Areas**:
  - Search input handling
  - Result display
  - Filtering and sorting
  - Search suggestions
  - Empty states

## Test Data Management

### Mock Data Creation

Use consistent test data creation helpers:

```kotlin
// Helper functions for creating test data
private fun createTestHealthArticleEntity(
    id: String = "test-article-1",
    title: String = "Test Article",
    category: String = "nutrition"
) = HealthArticleEntity(
    id = id,
    title = title,
    summary = "Test summary",
    content = "Test content",
    category = category,
    // ... other fields
)

private fun createTestDiscoverContentArticle() = DiscoverContent.Article(
    id = "test-article-1",
    title = "Test Article",
    // ... other fields
)
```

### Test Database Setup

For integration tests requiring database:

```kotlin
@Before
fun setup() {
    val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        HealthAssistantDatabase::class.java
    ).allowMainThreadQueries().build()
    
    discoverDao = database.discoverDao()
}

@After
fun tearDown() {
    database.close()
}
```

## Test Coverage Goals

### Coverage Targets

- **Unit Tests**: 90%+ coverage for business logic
- **Integration Tests**: 80%+ coverage for data flows
- **UI Tests**: 70%+ coverage for critical user paths

### Key Areas for Coverage

1. **Repository Layer**: 95% coverage
   - All CRUD operations
   - Error handling paths
   - Sync operations
   - Cache management

2. **ViewModel Layer**: 90% coverage
   - State management
   - User interactions
   - Error handling
   - Data transformation

3. **Use Cases**: 95% coverage
   - Business logic
   - Validation rules
   - Error scenarios

4. **UI Layer**: 70% coverage
   - Critical user flows
   - Error states
   - Loading states
   - User interactions

## Test Execution Strategy

### Continuous Integration

```yaml
# Example CI configuration
test_discover_feature:
  runs-on: ubuntu-latest
  steps:
    - name: Run Unit Tests
      run: ./gradlew testDebugUnitTest --tests "*discover*"
    
    - name: Run Integration Tests
      run: ./gradlew testDebugUnitTest --tests "*Integration*"
    
    - name: Generate Coverage Report
      run: ./gradlew jacocoTestReport
    
    - name: Upload Coverage
      uses: codecov/codecov-action@v1
```

### Local Development

```bash
# Quick test run during development
./gradlew testDebugUnitTest --tests "*discover*" --continue

# Full test suite with coverage
./gradlew clean testDebugUnitTest jacocoTestReport

# UI tests on connected device
./gradlew connectedAndroidTest --tests "*discover*"
```

## Performance Testing

### Database Performance Tests

```kotlin
@Test
fun `large dataset query performance`() = runTest {
    // Given - Large dataset
    val articles = (1..1000).map { createTestHealthArticleEntity(it.toString()) }
    discoverDao.insertHealthArticles(articles)
    
    // When - Measure query time
    val startTime = System.currentTimeMillis()
    val result = discoverDao.getHealthArticles(testUserId, null, 20)
    val duration = System.currentTimeMillis() - startTime
    
    // Then - Verify performance
    assertTrue(duration < 100) // Should complete in under 100ms
    assertEquals(20, result.size)
}
```

### Memory Usage Tests

```kotlin
@Test
fun `content adapter memory usage`() {
    // Test RecyclerView adapter memory efficiency
    val adapter = DiscoverContentAdapter()
    val largeDataset = (1..1000).map { createTestContent(it) }
    
    // Measure memory before and after
    val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    adapter.submitList(largeDataset)
    val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    
    // Verify reasonable memory usage
    val memoryIncrease = finalMemory - initialMemory
    assertTrue(memoryIncrease < 50 * 1024 * 1024) // Less than 50MB increase
}
```

## Debugging Test Failures

### Common Issues and Solutions

1. **Flaky Tests**
   - Use `runTest` for coroutine tests
   - Properly advance test schedulers
   - Use `waitUntil` for UI tests
   - Mock time-dependent operations

2. **Database Tests Failing**
   - Ensure proper database cleanup
   - Use in-memory database for tests
   - Check data isolation between tests

3. **UI Tests Timing Out**
   - Increase timeout values for slow operations
   - Use proper wait conditions
   - Check for UI thread blocking

### Test Debugging Tools

```kotlin
// Add logging to tests for debugging
@Test
fun `debug test with logging`() = runTest {
    println("Test started at: ${System.currentTimeMillis()}")
    
    // Test logic with debug prints
    val result = repository.getHealthArticles(testUserId, null, 20).first()
    println("Result: $result")
    
    assertTrue(result is Result.Success)
}
```

## Best Practices

1. **Test Naming**: Use descriptive names that explain the scenario
2. **Test Structure**: Follow Given-When-Then pattern
3. **Test Independence**: Each test should be independent and isolated
4. **Mock Usage**: Mock external dependencies, test real business logic
5. **Error Testing**: Always test error scenarios and edge cases
6. **Performance**: Include performance assertions for critical paths
7. **Maintainability**: Keep tests simple and focused on single behaviors

This comprehensive testing guide ensures the Discover feature is thoroughly tested across all layers and user scenarios.