# Activity Card Feature - Product Requirements Document

## Overview
The Activity Card feature will provide users with an automated daily health overview that captures and displays their health metrics and activities. Unlike the note and diary features, activity cards are read-only, automatically generated at midnight each day, and provide a comprehensive snapshot of the user's daily health journey.

**UPDATED ARCHITECTURE APPROACH**: After analyzing the existing codebase, this feature will leverage the existing journal system's flexible `JournalEntryEntity` structure rather than creating separate database tables. This approach maintains consistency with the current architecture and avoids breaking changes.

## Core Requirements

### 1. Functional Requirements

#### 1.1 Automatic Daily Generation
- **Trigger**: Activity cards are automatically created at midnight (00:00) every day
- **Frequency**: One activity card per day
- **Persistence**: Cards are stored permanently and cannot be deleted by users
- **Retroactive**: System should generate cards for past days if data exists

#### 1.2 Health Data Aggregation
The activity card will collect and display the following daily metrics:
- **Step Count**: Total steps taken during the day
- **Distance Traveled**: Calculated distance based on steps
- **Calories Burned**: Estimated calorie expenditure
- **Active Time**: Duration of physical activity
- **Heart Rate**: Average, minimum, and maximum heart rate (if available)
- **Sleep Data**: Sleep duration and quality metrics (if tracked)
- **Water Intake**: Daily hydration tracking
- **Mood/Energy Levels**: User-reported wellness indicators

#### 1.3 Data Sources
- Step tracking service integration
- Manual user inputs (water, mood, etc.)
- Health sensors (heart rate, if available)
- Third-party fitness app integrations (future enhancement)

#### 1.4 Display and Navigation
- **View-Only Interface**: No editing capabilities
- **Card-based Layout**: Similar to note/diary cards but with distinct styling
- **Chronological Organization**: Cards displayed in reverse chronological order (newest first)
- **Quick Stats**: Summary metrics visible on card preview
- **Detailed View**: Expandable or navigable detailed view for each card

### 2. Technical Requirements

#### 2.1 Data Model (Updated Approach)
Instead of creating separate entities, we'll use the existing `JournalEntryEntity` with a special type "activity_summary" to store daily activity cards:

```kotlin
// Using existing JournalEntryEntity structure
data class ActivityCard(
    val id: Long,
    val date: LocalDate, // derived from timestamp
    val stepCount: Int,
    val distanceKm: Double,
    val caloriesBurned: Int,
    val activeMinutes: Int,
    val heartRateData: HeartRateMetrics?,
    val sleepData: SleepMetrics?,
    val waterIntakeMl: Int,
    val moodRating: Int?,
    val energyLevel: Int?,
    val notes: String?
)

// Activity card will be stored as JournalEntryEntity with:
// - type = "activity_summary"
// - content = JSON serialized activity data
// - timestamp = midnight of the day
```

#### 2.2 Database Integration (Updated)
- **No new tables**: Use existing `journal_entries` table
- **Type field**: "activity_summary" for activity cards
- **Content field**: JSON serialized activity data using existing Gson converters
- **Timestamp**: Midnight timestamp for the specific day
- **Leverage existing**: Use current JournalEntryDao and JournalRepository

#### 2.3 Architecture Components (Updated)

##### 2.3.1 Data Layer (Leveraging Existing)
- `ActivityCardRepository`: Extension of existing JournalRepository
- `JournalEntryDao`: Use existing DAO with type-specific queries
- `ActivityCardMapper`: Maps between ActivityCard domain model and JournalEntryEntity
- `ActivityCardJsonConverter`: Handles JSON serialization of activity data

##### 2.3.2 Domain Layer
- `ActivityCard`: Core domain model (separate from JournalEntry)
- `GenerateActivityCardUseCase`: Business logic for card creation
- `GetActivityCardsUseCase`: Retrieve cards with filtering (uses existing journal queries)
- `ActivityCardRepository`: Interface extending journal repository capabilities

##### 2.3.3 Presentation Layer
- `ActivityCardListFragment`: Display list of activity cards
- `ActivityCardDetailFragment`: Enhanced detail view (update existing placeholder)
- `ActivityCardViewModel`: State management and business logic
- `ActivityCardAdapter`: RecyclerView adapter for card list

##### 2.3.4 Background Services
- `ActivityCardGeneratorWorker`: Scheduled work for midnight card generation
- Integration with existing `HealthDataSyncWorker`: Enhance to include activity data
- Use existing step tracking and health data services

#### 2.4 JSON Structure for Activity Card Storage
```json
{
  "stepCount": 8500,
  "distanceKm": 6.2,
  "caloriesBurned": 450,
  "activeMinutes": 65,
  "heartRate": {
    "average": 72,
    "minimum": 58,
    "maximum": 145,
    "resting": 62
  },
  "sleep": {
    "durationHours": 7.5,
    "qualityScore": 8,
    "bedTime": "23:30",
    "wakeTime": "07:00"
  },
  "waterIntakeMl": 2100,
  "moodRating": 4,
  "energyLevel": 3,
  "notes": "Good active day with morning run"
}
```

### 3. Implementation Strategy (Updated)

#### 3.1 Phase 1: Core Data Integration (Weeks 1-2)
- Create ActivityCard domain model
- Implement ActivityCardMapper for JournalEntryEntity conversion
- Create ActivityCardRepository extending existing journal functionality
- Add JSON converters for activity data serialization

#### 3.2 Phase 2: Business Logic (Weeks 3-4)
- Implement GenerateActivityCardUseCase
- Create data collection services for daily metrics
- Integrate with existing step tracking and health data services
- Add ActivityCardGeneratorWorker for scheduled generation

#### 3.3 Phase 3: UI Implementation (Weeks 5-6)
- Update existing ActivityDetailFragment with proper UI
- Create ActivityCardListFragment
- Implement ActivityCardViewModel
- Add navigation integration with existing journal system

#### 3.4 Phase 4: Integration and Polish (Weeks 7-8)
- Enhance existing HealthDataSyncWorker for activity cards
- Add activity card filtering to existing journal queries
- Performance optimization and testing
- Documentation and deployment

### 4. Database Queries (Updated)

Using existing JournalEntryDao with activity-specific queries:

```kotlin
// Get all activity cards (daily summaries)
@Query("SELECT * FROM journal_entries WHERE type = 'activity_summary' ORDER BY timestamp DESC")
fun getAllActivityCards(): Flow<List<JournalEntryEntity>>

// Get activity card for specific date
@Query("SELECT * FROM journal_entries WHERE type = 'activity_summary' AND DATE(timestamp/1000, 'unixepoch') = :date")
suspend fun getActivityCardByDate(date: String): JournalEntryEntity?

// Check if activity card exists for date
@Query("SELECT EXISTS(SELECT 1 FROM journal_entries WHERE type = 'activity_summary' AND DATE(timestamp/1000, 'unixepoch') = :date)")
suspend fun activityCardExistsForDate(date: String): Boolean
```

### 5. Benefits of Updated Approach

#### 5.1 Architectural Consistency
- **Reuses existing infrastructure**: No need for new DAOs, databases, or converters
- **Maintains patterns**: Follows established journal entry patterns
- **Simplifies maintenance**: Single source of truth for all health data

#### 5.2 Development Efficiency
- **Faster implementation**: Leverages existing, tested code
- **Reduced complexity**: No new database migrations or schema changes
- **Lower risk**: No breaking changes to existing functionality

#### 5.3 Future Scalability
- **Unified queries**: Can query activity data alongside other journal entries
- **Flexible storage**: JSON content allows easy schema evolution
- **Integration ready**: Easy to correlate activity data with mood, sleep, etc.

### 6. Migration Strategy

Since we're using the existing database structure:
- **No migrations needed**: Activity cards use existing journal_entries table
- **Backward compatible**: Existing journal functionality remains unchanged
- **Gradual rollout**: Can be deployed without affecting existing users

### 7. User Interface Requirements

#### 7.1 Activity Card List View
- **Layout**: RecyclerView with card-based items
- **Card Preview**: Date, step count, key metrics summary
- **Visual Indicators**: Progress rings, health status colors
- **No Edit Actions**: No delete/edit buttons (read-only)
- **Loading States**: Skeleton loaders for data fetch
- **Empty States**: Helpful messages for new users

#### 7.2 Activity Card Detail View
- **Header**: Date and day of week prominently displayed
- **Metrics Sections**: 
  - Physical Activity (steps, distance, calories)
  - Health Vitals (heart rate, sleep)
  - Wellness (mood, energy, hydration)
- **Visual Charts**: Progress rings, trend graphs
- **Achievement Badges**: Goal completion indicators
- **Export Options**: Share card summary (future enhancement)

#### 7.3 Navigation Integration
- **Bottom Navigation**: Add "Activity" tab alongside existing tabs
- **Deep Linking**: Direct navigation to specific activity cards
- **Search/Filter**: Find cards by date range or metrics

### 8. Business Logic

#### 8.1 Card Generation Logic
```kotlin
// Pseudocode for daily card generation
fun generateDailyActivityCard(date: LocalDate) {
    // Collect data from various sources
    val stepData = stepTrackingService.getDailySteps(date)
    val heartRateData = healthSensorManager.getDailyHeartRate(date)
    val userInputs = userDataRepository.getDailyInputs(date)
    
    // Calculate derived metrics
    val distance = calculateDistance(stepData.count, userProfile.strideLength)
    val calories = calculateCalories(stepData.count, userProfile.weight)
    
    // Create and store activity card
    val activityCard = ActivityCard(
        id = UUID.randomUUID().toString(),
        date = date,
        createdAt = LocalDateTime.now(),
        stepCount = stepData.count,
        distanceKm = distance,
        caloriesBurned = calories,
        // ... other metrics
    )
    
    activityCardRepository.insertCard(activityCard)
}
```

#### 8.2 Data Aggregation Strategy
- **Real-time Collection**: Continuously collect metrics throughout the day
- **Midnight Aggregation**: Finalize and create card at day boundary
- **Retroactive Processing**: Handle missing cards for past days
- **Data Validation**: Ensure metric accuracy and handle edge cases

### 9. Performance Requirements

#### 9.1 Response Times
- **Card List Loading**: < 500ms for 30 cards
- **Detail View Navigation**: < 200ms
- **Card Generation**: < 2 seconds (background process)

#### 9.2 Storage Efficiency
- **Local Storage**: Optimize for minimal space usage
- **Data Compression**: Efficient serialization for cloud sync
- **Cleanup Strategy**: Archive old cards (retain locally, compress for cloud)

#### 9.3 Battery Optimization
- **Background Limits**: Minimal battery impact for data collection
- **Efficient Scheduling**: Use system-optimized work scheduling
- **Sensor Management**: Smart sensor usage patterns

### 10. Security and Privacy

#### 10.1 Data Protection
- **Local Encryption**: Sensitive health data encrypted at rest
- **Secure Transmission**: HTTPS/TLS for cloud sync
- **User Consent**: Clear privacy policy for health data usage

#### 10.2 Access Control
- **User Authentication**: Secure access to personal health data
- **Data Isolation**: User data completely isolated between accounts

### 11. Future Enhancements

#### 11.1 Advanced Analytics
- **Trend Analysis**: Weekly/monthly health trends
- **Goal Tracking**: Progress toward health objectives
- **Insights Generation**: AI-powered health insights

#### 11.2 Integration Capabilities
- **Wearable Devices**: Sync with smartwatches and fitness trackers
- **Health Apps**: Integration with Google Fit, Apple Health
- **Medical Records**: Connect with healthcare providers (long-term)

#### 11.3 Social Features
- **Achievement Sharing**: Share milestones on social platforms
- **Family Tracking**: Shared family health dashboard
- **Community Challenges**: Group fitness challenges

### 12. Success Metrics

#### 12.1 User Engagement
- **Daily Card View Rate**: % of users viewing their daily card
- **Retention**: Users regularly checking activity cards
- **Session Duration**: Time spent viewing health data

#### 12.2 Technical Performance
- **Card Generation Success Rate**: >99.5%
- **Sync Reliability**: <1% data loss rate
- **App Performance**: No degradation from background processing

### 13. Implementation Timeline

#### Phase 1 (Weeks 1-2): Core Infrastructure
- Database schema and entities
- Repository and use case implementation
- Basic data collection service

#### Phase 2 (Weeks 3-4): UI Implementation
- Activity card list and detail views
- Navigation integration
- Basic card generation

#### Phase 3 (Weeks 5-6): Automation and Polish
- Midnight scheduling implementation
- Advanced UI features and animations
- Testing and bug fixes

#### Phase 4 (Weeks 7-8): Integration and Launch
- Cloud sync implementation
- Performance optimization
- Documentation and deployment

### 14. Risk Mitigation

#### 14.1 Technical Risks
- **Data Loss**: Implement robust backup and recovery
- **Performance Issues**: Continuous monitoring and optimization
- **Battery Drain**: Careful background processing management

#### 14.2 User Experience Risks
- **Overwhelming Information**: Progressive disclosure of data
- **Privacy Concerns**: Transparent data usage policies
- **Adoption Challenges**: Intuitive onboarding and tutorials

## Conclusion

The Activity Card feature will provide users with valuable insights into their daily health journey while maintaining simplicity and privacy. The read-only nature ensures data integrity while the automatic generation provides consistent tracking without user burden. This feature will enhance the overall health assistant experience by creating a comprehensive, long-term view of user health patterns and progress.