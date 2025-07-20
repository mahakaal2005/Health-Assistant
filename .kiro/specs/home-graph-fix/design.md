# Design Document

## Overview

This design addresses the issue where the 7-day graph in the home fragment loses historical data when transitioning to the next day. The current implementation appears to only show present-day data, causing previous days' step counts to be lost when the day changes. The fix will ensure proper data persistence and retrieval for a rolling 7-day window while maintaining the existing UI and user experience.

## Architecture

The solution leverages the existing architecture components:

- **HomeFragment**: Contains the chart display logic and UI interactions
- **ChartManager**: Handles chart configuration and data visualization
- **HealthRepository**: Manages data persistence and retrieval
- **DailyStepData**: Data model representing daily step information
- **EnhancedHealthTracker**: Device sensor integration for real-time data

The fix will focus on enhancing the data persistence layer and ensuring proper historical data retrieval without modifying the UI components.

## Components and Interfaces

### 1. Data Persistence Enhancement

**HealthRepository Interface Updates**
- Ensure `getWeeklyStepData(startDate: LocalDate)` properly retrieves 7 days of historical data
- Enhance `saveDailyStepData(stepData: DailyStepData)` to persist data with proper date keys
- Add data validation to prevent data loss during day transitions

**HealthRepositoryImpl Enhancements**
- Modify the in-memory cache `_dailyStepDataMap` to maintain historical data beyond current day
- Implement proper date-based key management for multi-day data storage
- Add data migration logic to handle day transitions without data loss

### 2. Chart Data Management

**ChartManager Updates**
- Ensure `setupChart()` method handles missing historical data gracefully
- Implement zero-value display for days with no recorded data
- Maintain consistent 7-day display regardless of available data

**HomeFragment Chart Setup**
- Enhance `setupStepsChart()` to request full 7-day historical data
- Add error handling for missing data scenarios
- Implement data refresh logic for day transitions

### 3. Data Model Enhancements

**DailyStepData Model**
- Ensure proper date handling for historical data storage
- Add validation for data integrity across day boundaries
- Maintain backward compatibility with existing data structure

## Data Models

### Enhanced Data Storage Structure

```kotlin
// User-specific daily step data with historical tracking
private val _dailyStepDataMap = MutableStateFlow<Map<String, Map<String, DailyStepData>>>(emptyMap())
// Structure: userId -> dateString -> DailyStepData

// Weekly data cache for efficient retrieval
private val _weeklyDataCache = MutableStateFlow<Map<String, List<DailyStepData>>>(emptyMap())
// Structure: userId -> List<DailyStepData> (7 days)
```

### Data Flow for Historical Preservation

1. **Daily Data Collection**: Real-time step data is collected and stored with proper date keys
2. **Historical Data Retrieval**: When charts load, retrieve 7 days of data starting from current day going backward
3. **Data Gap Handling**: For missing days, create DailyStepData entries with zero values
4. **Rolling Window Management**: Maintain exactly 7 days of data, removing oldest when adding new

## Error Handling

### Data Integrity Scenarios

1. **Missing Historical Data**
   - Create zero-value DailyStepData entries for missing days
   - Log warnings for data gaps but continue chart display
   - Graceful degradation to available data

2. **Day Transition Edge Cases**
   - Implement atomic data updates during day changes
   - Add data validation to prevent corruption
   - Backup current day data before transition

3. **User Session Changes**
   - Ensure proper user isolation for historical data
   - Clear cache appropriately when users switch
   - Maintain data consistency across sessions

### Chart Display Error Handling

1. **Empty Data Sets**
   - Display empty chart with proper axis labels
   - Show informative message about data collection
   - Maintain UI consistency

2. **Partial Data Availability**
   - Display available data with zero values for missing days
   - Ensure proper day labels (Mon, Tue, Wed, etc.)
   - Maintain 7-day chart structure

## Testing Strategy

### Unit Testing

1. **Data Persistence Tests**
   - Test historical data storage and retrieval
   - Verify day transition data preservation
   - Test user isolation for multi-user scenarios

2. **Chart Data Tests**
   - Test 7-day data generation with missing days
   - Verify zero-value handling for future/missing days
   - Test rolling window behavior

### Integration Testing

1. **End-to-End Chart Display**
   - Test chart display with various data scenarios
   - Verify day transition behavior
   - Test data refresh on fragment resume

2. **Data Consistency Tests**
   - Test data persistence across app restarts
   - Verify historical data integrity
   - Test concurrent data updates

### Manual Testing Scenarios

1. **Day Transition Testing**
   - Monitor chart behavior during midnight transition
   - Verify historical data preservation
   - Test multiple day transitions

2. **Data Gap Testing**
   - Test chart display with missing historical data
   - Verify zero-value display for empty days
   - Test partial week data scenarios

## Implementation Approach

### Phase 1: Data Persistence Fix
- Enhance HealthRepositoryImpl to properly store historical data
- Implement robust date-based key management
- Add data validation and integrity checks

### Phase 2: Chart Data Retrieval
- Update chart setup methods to request 7-day historical data
- Implement proper error handling for missing data
- Add zero-value generation for incomplete data sets

### Phase 3: Day Transition Handling
- Implement atomic data updates during day changes
- Add data migration logic for seamless transitions
- Ensure rolling window behavior works correctly

### Phase 4: Testing and Validation
- Comprehensive testing of all scenarios
- Performance optimization for data retrieval
- User acceptance testing for UI consistency

## Performance Considerations

1. **Data Caching**: Implement efficient caching for 7-day data windows
2. **Memory Management**: Limit historical data storage to prevent memory leaks
3. **Query Optimization**: Optimize database queries for weekly data retrieval
4. **UI Responsiveness**: Ensure chart updates don't block UI thread

## Security and Privacy

1. **User Data Isolation**: Ensure proper user separation for historical data
2. **Data Encryption**: Maintain existing data security practices
3. **Privacy Compliance**: No additional privacy concerns as this is a bug fix

## Backward Compatibility

1. **Existing Data**: Ensure compatibility with current data format
2. **API Consistency**: Maintain existing method signatures
3. **UI Consistency**: No changes to user-facing interface