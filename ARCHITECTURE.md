# Health Assistant App - Architecture

## Repository Pattern Implementation

This project has been refactored to implement the Repository pattern with Hilt dependency injection to improve code organization, testability, and maintainability.

### Key Components

#### 1. Repository Interfaces
Located in `data/repository/interfaces`:
- `AuthRepository` - Authentication operations
- `HealthRepository` - Health metrics data
- `UserProfileRepository` - User profile information

#### 2. Repository Implementations
Located in `data/repository/impl`:
- `FirebaseAuthRepositoryImpl` - Firebase authentication implementation
- `HealthRepositoryImpl` - Health metrics implementation
- `UserProfileRepositoryImpl` - User profile data implementation using DataStore

#### 3. Dependency Injection Modules
Located in `di`:
- `RepositoryModule` - Provides repository implementations
- `DataStoreModule` - Provides DataStore dependencies
- `SessionModule` - Provides SessionManager

#### 4. ViewModels
- `AuthViewModel` - Handles authentication operations
- `HealthMetricsViewModel` - Manages health metrics data
- `ProfileViewModel` - Handles user profile data

### Dependency Injection with Hilt

Hilt is configured to provide all repositories as singletons, ensuring consistent state throughout the application.

### Key Improvements

1. **Separation of Concerns**:
   - Data sources (Firebase, DataStore) are now isolated behind repository interfaces
   - ViewModels no longer directly interact with data sources

2. **Testability**:
   - All components can now be easily mocked for testing
   - Repository interfaces allow for test implementations

3. **Maintainability**:
   - Clear architecture makes it easier to understand the codebase
   - New features can be added by extending repositories without modifying UI code

4. **Asynchronous Operations**:
   - All repository methods use suspend functions or Flow for asynchronous operations
   - Consistent error handling across the application

### Flow of Data

1. UI (Activities/Fragments) -> ViewModels -> Repositories -> Data Sources
2. Data Sources -> Repositories -> ViewModels (via Flow/LiveData) -> UI

### Usage Examples

#### Injecting a Repository:
```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val healthRepository: HealthRepository
) : ViewModel() {
    // Use the repository here
}
```

#### Using a Repository in a ViewModel:
```kotlin
val healthMetrics: LiveData<HealthMetrics> = healthRepository.getHealthMetrics().asLiveData()

fun updateSteps(steps: Int) {
    viewModelScope.launch {
        healthRepository.updateSteps(steps)
    }
}
```