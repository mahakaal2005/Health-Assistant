# Implementation Plan

- [x] 1. Create simplified data models for section-based content


  - Create VideoContent, NewsContent, and ArticleContent data classes with essential fields only
  - Create DiscoverSections container class for all three content types
  - Add simple mapping extension functions from existing entities to new models
  - Remove complex fields like analytics, credibility scoring, and advanced metadata
  - _Requirements: 1.1, 1.2, 7.1_

- [x] 2. Design new horizontal card layouts for each content type


  - Create item_video_horizontal.xml with thumbnail, title, duration, and play button
  - Create item_news_horizontal.xml with image, headline, source, and time
  - Create item_article_horizontal.xml with title, author, reading time, and progress
  - Ensure consistent card dimensions and spacing across all layouts
  - _Requirements: 2.1, 3.1, 4.1, 7.1, 7.3_

- [x] 3. Create simplified adapters for each content section


  - Implement VideosAdapter with simple ViewHolder and click handling
  - Implement NewsAdapter with image loading and time formatting
  - Implement ArticlesAdapter with bookmark toggle and progress display
  - Add DiffUtil callbacks for efficient list updates
  - _Requirements: 2.1, 3.1, 4.1, 7.4_



- [ ] 4. Redesign main discover fragment layout with three sections
  - Create new fragment_discover.xml with vertical LinearLayout containing three sections
  - Add section headers with titles and "See All" buttons
  - Add horizontal RecyclerViews for each content type
  - Include SwipeRefreshLayout for pull-to-refresh functionality


  - _Requirements: 1.1, 1.2, 1.3, 7.1, 7.2_

- [ ] 5. Simplify DiscoverViewModel to handle section-based data
  - Remove complex state management and use single DiscoverSections state
  - Implement loadAllSections() method to fetch 5 items per section

  - Add simple refresh functionality without complex retry logic
  - Remove search, analytics, and content reporting functionality
  - _Requirements: 1.1, 5.1, 5.2, 6.2_

- [x] 6. Update DiscoverFragment with simplified section-based UI


  - Setup three separate adapters for videos, news, and articles
  - Implement section header click listeners for "See All" navigation
  - Add simple error handling with basic error messages
  - Remove complex search UI and advanced interaction features
  - _Requirements: 1.1, 1.4, 5.1, 5.3, 7.4_

- [ ] 7. Create dedicated "See All" screens for each content type
  - Create VideosListFragment with vertical RecyclerView for all videos
  - Create NewsListFragment with vertical RecyclerView for all news
  - Create ArticlesListFragment with vertical RecyclerView for all articles
  - Add simple navigation actions in nav_main.xml
  - _Requirements: 1.4, 2.2, 3.2, 4.2_

- [ ] 8. Implement simplified repository methods for section data
  - Add getVideos(limit), getNews(limit), and getArticles(limit) methods
  - Implement getAllVideos(), getAllNews(), and getAllArticles() for "See All" screens
  - Remove complex filtering, search, and analytics methods
  - Keep existing toggleBookmark() and syncContent() methods
  - _Requirements: 1.1, 6.1, 6.2_

- [ ] 9. Add simple offline support with cached content display
  - Update repository to return cached content when offline
  - Add simple offline indicators in UI when appropriate
  - Implement basic cache management without complex algorithms
  - Remove advanced offline features like content prefetching
  - _Requirements: 6.1, 6.3, 6.4_

- [ ] 10. Update navigation graph for simplified discover flow
  - Add navigation actions from discover to dedicated list screens
  - Update existing navigation to video player and article reader
  - Remove complex deep linking and sharing navigation
  - Ensure proper back stack management for new screens
  - _Requirements: 1.4, 2.2, 3.2, 4.2_

- [ ] 11. Implement basic error handling and loading states
  - Add simple loading indicators for each section
  - Create basic error messages without complex retry mechanisms
  - Add empty state layouts for when sections have no content
  - Remove advanced error handling like exponential backoff
  - _Requirements: 5.4, 6.5, 7.4_

- [ ] 12. Add simple bookmark functionality for articles
  - Implement bookmark toggle in ArticlesAdapter
  - Update UI to show bookmark state with simple icons
  - Connect to existing bookmark repository methods
  - Remove complex bookmark management and sync features
  - _Requirements: 4.4, 4.5_

- [ ] 13. Create consistent visual styling across all sections
  - Update colors, typography, and spacing to match design system
  - Ensure consistent card elevations and corner radius
  - Add smooth transitions for loading and content updates
  - Remove complex animations and visual effects
  - _Requirements: 7.1, 7.2, 7.3, 7.5_

- [ ] 14. Remove unused complex features from existing implementation
  - Remove or comment out search functionality, analytics, and content reporting
  - Clean up unused imports and dependencies
  - Remove complex error handling and retry mechanisms
  - Simplify dependency injection modules by removing unused components
  - _Requirements: 5.1, 5.2_

- [ ] 15. Write basic tests for simplified implementation
  - Create unit tests for simplified ViewModel methods
  - Test adapter functionality and data binding
  - Add integration tests for section loading and navigation
  - Remove complex test scenarios for features that were removed
  - _Requirements: All requirements - quality assurance_