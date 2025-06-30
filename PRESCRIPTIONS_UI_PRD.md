# Product Requirements Document (PRD)
## Prescriptions UI Feature - Health Assistant App

### Document Information
- **Version**: 2.0
- **Date**: June 30, 2025
- **Product**: Health Assistant Android App
- **Feature**: Prescriptions Management UI
- **Status**: Planning Phase
- **Architecture Alignment**: Full integration with existing MVVM + Room + Hilt + Firebase architecture

---

## 1. Executive Summary

### 1.1 Overview
Implement a comprehensive Prescriptions Management UI within the Health Assistant app to allow users to digitally organize, search, and manage their medical prescriptions through photo capture and categorization, following the existing app's proven architecture patterns.

### 1.2 Objectives
- Enable users to digitally store prescription images for easy access
- Provide organized categorization by disease type (initially with mock data, Room database integration at the end)
- Implement efficient search and filtering capabilities
- Maintain visual consistency with existing app design system
- Integrate seamlessly with existing Hilt DI and architecture patterns
- **Phase 1**: Complete UI implementation with mock data
- **Phase 2**: Room database integration after UI is finalized
- Ensure zero impact on existing working features

### 1.3 Success Metrics
- UI completion: 100% functional prescription management interface with mock data
- User adoption: 70% of active users utilize prescriptions feature within 30 days (post-database integration)
- User engagement: Average 3+ prescriptions saved per active user
- UI consistency: 100% adherence to existing design system
- Performance: <2 second load times for prescription list
- Zero regressions: No impact on existing app functionality throughout implementation

---

## 2. Architecture Analysis & Alignment

### 2.1 Existing Project Architecture
Based on deep analysis of the current Health Assistant app structure:

**Current Tech Stack:**
- **Language**: Kotlin with Java 11 compatibility
- **Architecture**: MVVM with Repository pattern
- **DI**: Hilt (already configured)
- **Database**: Room (HealthAssistantDatabase already exists) - *Will be extended at the end*
- **Remote**: Firebase (Auth, Firestore, Storage)
- **Image Loading**: Coil3
- **UI**: ViewBinding + DataBinding enabled
- **Navigation**: Navigation Component
- **Coroutines**: Full coroutines support with Flow
- **Testing**: JUnit5, MockK, Turbine for comprehensive testing

**Implementation Strategy - Updated:**
1. **Phase 1**: UI-First Approach with mock data and in-memory storage
2. **Phase 2**: Room database integration after UI is complete and tested
3. **Gradle Management**: All dependency additions will be communicated for manual implementation

### 2.2 Integration Strategy - Revised
**UI-First Safe Implementation Approach:**
1. **Mock Data Implementation**: Start with in-memory data storage using sealed classes and lists
2. **Repository Pattern**: Implement repository interfaces with mock implementations first
3. **UI Layer Complete**: Build entire prescription management UI with full functionality
4. **Database Migration**: Extend existing Room database after UI is proven and stable
5. **DI Integration**: Extend existing Hilt modules without modification
6. **Feature Isolation**: Create new feature module following existing patterns
7. **Testing Strategy**: Mirror existing test structure and patterns

---

## 3. Target Users

### 3.1 Primary Users
- **Chronic condition patients**: Need to manage multiple ongoing prescriptions
- **Elderly users**: Require simple, accessible prescription organization
- **Caregivers**: Managing prescriptions for family members

### 3.2 User Personas
**Persona 1: Maria (65, Diabetes Patient)**
- Manages 5+ daily medications
- Visits multiple specialists
- Needs quick access during doctor visits

**Persona 2: John (35, Caregiver)**
- Manages prescriptions for elderly parent
- Travels frequently, needs digital access
- Requires organization by doctor/condition

---

## 4. Feature Requirements

### 4.1 Functional Requirements

#### 4.1.1 Core Features
- **FR-001**: Display all saved prescriptions in a scrollable list
- **FR-002**: Group prescriptions by disease category with section headers
- **FR-003**: Search prescriptions by doctor's name with real-time filtering
- **FR-004**: Add new prescriptions via camera photo capture
- **FR-005**: View prescription details in full-screen mode
- **FR-006**: Edit prescription metadata (doctor name, category)
- **FR-007**: Delete individual prescriptions with confirmation

#### 4.1.2 User Interface Features
- **FR-008**: Floating Action Button for adding new prescriptions
- **FR-009**: Search bar at top of prescriptions list
- **FR-010**: Prescription cards showing thumbnail, doctor, date, category
- **FR-011**: Quick action menu for each prescription (view/edit/delete)
- **FR-012**: Empty state when no prescriptions exist
- **FR-013**: Loading states during data operations

#### 4.1.3 Add Prescription Flow
- **FR-014**: Camera integration for prescription photo capture
- **FR-015**: Photo preview before saving
- **FR-016**: Text input for doctor name
- **FR-017**: Dropdown/autocomplete for disease category selection
- **FR-018**: Form validation for required fields
- **FR-019**: Save prescription with metadata

### 4.2 Non-Functional Requirements

#### 4.2.1 Performance
- **NFR-001**: List loads within 2 seconds
- **NFR-002**: Search filtering responds within 500ms
- **NFR-003**: Image capture and preview within 3 seconds
- **NFR-004**: Smooth scrolling at 60fps

#### 4.2.2 Usability
- **NFR-005**: Intuitive navigation following Material Design principles
- **NFR-006**: Accessible to users with disabilities (contrast, text size)
- **NFR-007**: Consistent with existing app UI patterns
- **NFR-008**: Error messages are clear and actionable

#### 4.2.3 Technical
- **NFR-009**: MVVM architecture pattern compliance
- **NFR-010**: ViewBinding for all UI interactions
- **NFR-011**: Proper memory management for images
- **NFR-012**: Handle device rotation gracefully

---

## 5. Technical Specifications - Updated for UI-First Approach

### 5.1 Architecture Implementation - Phase 1 (UI Focus)

#### 5.1.1 Mock Data Layer (Initial Implementation)
**In-Memory Data Management:**
```kotlin
// Mock data models for initial UI implementation
data class Prescription(
    val id: String = UUID.randomUUID().toString(),
    val imageUri: String,
    val localImagePath: String,
    val doctorName: String,
    val diseaseCategory: DiseaseCategory,
    val dateAdded: LocalDateTime,
    val dateModified: LocalDateTime,
    val notes: String? = null,
    val userId: String
)

data class DiseaseCategory(
    val id: String,
    val name: String,
    val displayName: String,
    val iconRes: Int?,
    val isCustom: Boolean = false
)

// Mock repository for UI development
class MockPrescriptionRepository : PrescriptionRepository {
    private val _prescriptions = MutableStateFlow<List<Prescription>>(emptyList())
    override fun getAllPrescriptions(): Flow<List<Prescription>> = _prescriptions.asStateFlow()
    // ... other mock implementations
}
```

#### 5.1.2 Repository Layer (Mock Implementation First)
```kotlin
// Interface (final design)
interface PrescriptionRepository {
    fun getAllPrescriptions(): Flow<List<Prescription>>
    fun getPrescriptionsByCategory(category: String): Flow<List<Prescription>>
    fun searchPrescriptionsByDoctor(doctorName: String): Flow<List<Prescription>>
    suspend fun insertPrescription(prescription: Prescription): Result<Unit>
    suspend fun updatePrescription(prescription: Prescription): Result<Unit>
    suspend fun deletePrescription(prescriptionId: String): Result<Unit>
}

// Mock implementation for UI development
@Singleton
class MockPrescriptionRepositoryImpl @Inject constructor() : PrescriptionRepository {
    private val mockData = mutableListOf<Prescription>()
    private val _prescriptions = MutableStateFlow<List<Prescription>>(mockData)
    
    override fun getAllPrescriptions(): Flow<List<Prescription>> = _prescriptions.asStateFlow()
    // ... complete mock implementation for all UI testing
}
```

#### 5.1.3 Room Database Integration (Phase 2 - End Implementation)
**Database Migration Strategy (To be implemented after UI completion):**
```kotlin
// Future implementation after UI is complete
@Database(
    entities = [
        ProfileImageEntity::class,  // Existing entity
        PrescriptionEntity::class,  // New entity - Phase 2
        DiseaseCategoryEntity::class // New entity - Phase 2
    ],
    version = 2, // Increment version safely - Phase 2
    exportSchema = false
)
abstract class HealthAssistantDatabase : RoomDatabase() {
    abstract fun profileImageDao(): ProfileImageDao // Existing
    abstract fun prescriptionDao(): PrescriptionDao // Phase 2
    abstract fun diseaseCategoryDao(): DiseaseCategoryDao // Phase 2
}
```

### 5.2 Dependencies Management

#### 5.2.1 Required Dependencies (Manual Addition Required)
**Dependencies to be added manually to build.gradle.kts:**
```kotlin
// Camera functionality (new addition for Phase 1)
implementation("androidx.camera:camera-camera2:2.3.0")
implementation("androidx.camera:camera-lifecycle:2.3.0") 
implementation("androidx.camera:camera-view:2.3.0")

// Additional utilities for UI (if needed)
implementation("androidx.activity:activity-result-ktx:1.9.0") // Already included

// Note: Room dependencies already exist in project
// Note: All other required dependencies (Hilt, Coil3, ViewBinding, etc.) already available
```

#### 5.2.2 Permissions Required (Manual Addition)
**Add to AndroidManifest.xml:**
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="28" />
```

---

## 6. Implementation Plan - Updated for UI-First Approach

start implemnting and make sre to update prd with the evelopment done
### 6.1 Phase 1: UI Layouts & Resources Setup (Week 1) ✅ **COMPLETED**
**Pure UI Foundation - No Kotlin Files Yet:**
- [x] Create `fragment_prescriptions.xml` - Main prescriptions screen layout
- [x] Create `item_prescription_card.xml` - Individual prescription card layout
- [x] Create `item_prescription_category_header.xml` - Category section headers
- [x] Create `bottom_sheet_add_prescription.xml` - Add prescription form
- [x] Create `dialog_prescription_detail.xml` - Full-screen prescription view
- [x] Add prescription-related string resources to `strings.xml` (67 strings added)
- [x] Add prescription-related drawable resources (icons, placeholders)
- [x] **UI Validation**: All layouts created following existing design patterns
- [x] **Safety Check**: No resource naming conflicts with existing code

**✅ Phase 1 Results:**
- **5 complete UI layouts** ready for implementation
- **67 comprehensive string resources** covering all UI scenarios
- **Essential drawable resources** for icons and placeholders
- **100% adherence** to existing HealthAssistant design system
- **Zero conflicts** with existing codebase confirmed

### 6.2 Phase 2: Data Models & Mock Repository (Week 2) ✅ **COMPLETED**
**Kotlin Foundation Files:**
- [x] Create `data/model/Prescription.kt` - Core prescription data class with helper methods
- [x] Create `data/model/DiseaseCategory.kt` - Disease category data class with 8 default categories
- [x] Create `data/repository/interfaces/PrescriptionRepository.kt` - Repository interface with full CRUD operations
- [x] Create `data/repository/impl/MockPrescriptionRepositoryImpl.kt` - Mock implementation with sample data
- [x] Create `features/prescriptions/utils/PrescriptionUtils.kt` - Utility functions for validation and formatting
- [x] Set up default disease categories as hardcoded constants (8 medical categories)
- [x] Configure Hilt DI binding for mock repository in RepositoryModule
- [x] **Data Validation**: Comprehensive validation with error handling implemented
- [x] **Safety Check**: No class naming conflicts with existing code confirmed

**✅ Phase 2 Results:**
- **5 core Kotlin files** implementing complete data layer
- **8 default disease categories** (Cardiology, Diabetes, Respiratory, Orthopedic, Dermatology, Neurology, General, Other)
- **3 sample prescriptions** with realistic data for UI testing
- **Reactive data flow** using StateFlow for real-time UI updates
- **Complete repository pattern** following existing app architecture
- **Hilt DI integration** properly configured without conflicts

### 6.3 Phase 3: Fragment & ViewModel Setup (Week 3) ✅ **COMPLETED**
**Core UI Controller Files:**
- [x] Create `features/prescriptions/PrescriptionsFragment.kt` - Main fragment with complete UI integration
- [x] Create `features/prescriptions/PrescriptionsViewModel.kt` - ViewModel with StateFlow and reactive updates
- [x] Create `features/prescriptions/adapter/PrescriptionsAdapter.kt` - RecyclerView adapter with multiple view types
- [x] Create `features/prescriptions/adapter/PrescriptionViewHolder.kt` - Prescription card ViewHolder with animations
- [x] Create `features/prescriptions/adapter/CategoryHeaderViewHolder.kt` - Category header ViewHolder
- [x] Implement search functionality with real-time filtering using TextWatcher
- [x] Connect fragment to navigation graph (nav_main.xml) with proper navigation actions
- [x] Update HomeFragment to navigate to prescriptions instead of showing toast
- [x] **UI Integration**: Complete UI flow working with mock data and reactive updates
- [x] **Safety Check**: Navigation and existing UI components work correctly

**✅ Phase 3 Results:**
- **5 complete UI controller files** implementing full MVVM pattern
- **Reactive search functionality** with real-time doctor name filtering
- **Multiple view types** (category headers + prescription cards) in RecyclerView
- **Animated interactions** including quick actions (view/edit/delete)
- **Navigation integration** properly connected to existing app flow
- **UI state management** for loading, empty states, and error handling
- **Sample data integration** showing 3 prescriptions across different categories

### 6.4 Phase 4: Camera & Add Prescription Flow (Week 4)
**Camera Integration and Forms:**
- [ ] **Manual Dependency Addition**: Add Camera dependencies to build.gradle.kts
- [ ] **Manual Permission Addition**: Add camera permissions to AndroidManifest.xml
- [ ] Create `features/prescriptions/camera/CameraManager.kt` - Camera handling
- [ ] Create `features/prescriptions/dialogs/AddPrescriptionBottomSheet.kt` - Add prescription form
- [ ] Create `features/prescriptions/utils/FileManager.kt` - Local file management
- [ ] Implement image capture and preview functionality
- [ ] Implement form validation and disease category selection
- [ ] Integrate with mock repository for data persistence
- [ ] **Camera Testing**: Verify camera functionality on real device
- [ ] **Safety Check**: Verify camera permissions don't interfere with existing features

### 6.5 Phase 5: Advanced UI Features (Week 5)
**Complete UI Feature Set:**
- [ ] Create `features/prescriptions/dialogs/PrescriptionDetailDialog.kt` - Detail view
- [ ] Create `features/prescriptions/dialogs/EditPrescriptionDialog.kt` - Edit functionality
- [ ] Implement delete functionality with confirmation dialogs
- [ ] Add prescription detail view with full-screen image display
- [ ] Implement quick actions menu (view/edit/delete)
- [ ] Add empty states and loading animations
- [ ] Add comprehensive error handling and user feedback
- [ ] Optimize performance and memory usage
- [ ] Polish animations and transitions following app patterns
- [ ] **UI Polish**: Complete UI testing and refinement
- [ ] **Safety Check**: Full regression testing of existing features

### 6.6 Phase 6: Room Database Integration (Week 6) - **FINAL PHASE**
**Database Migration After UI Completion:**
- [ ] Create `data/local/entity/PrescriptionEntity.kt` - Room entity
- [ ] Create `data/local/entity/DiseaseCategoryEntity.kt` - Room entity
- [ ] Create `data/local/dao/PrescriptionDao.kt` - Data Access Object
- [ ] Create `data/local/dao/DiseaseCategoryDao.kt` - Data Access Object
- [ ] Update `HealthAssistantDatabase.kt` - Add new entities and DAOs
- [ ] Create database migration (v1 → v2) with proper migration strategy
- [ ] Create `data/repository/impl/RoomPrescriptionRepositoryImpl.kt` - Room implementation
- [ ] Update Hilt DI modules to provide Room-based repository
- [ ] Migrate existing mock data to Room database
- [ ] Add database seeding with default disease categories
- [ ] Implement data validation and constraint checking
- [ ] Create comprehensive integration tests for Room operations
- [ ] **Migration Testing**: Test database operations thoroughly
- [ ] **Data Integrity**: Validate all CRUD operations maintain data consistency
- [ ] **Performance Testing**: Verify database operations meet performance requirements
- [ ] **Final Safety Check**: Complete end-to-end testing ensuring zero impact on existing features

---

## 7. Implementation Status Tracking - Updated for UI-First

### 7.1 Implementation Phases Priority
**Phase-by-Phase Progress:**
1. **UI Layouts & Resources** (Pure XML, no conflicts possible)
2. **Data Models & Repository** (Kotlin classes, conflict-checked)
3. **Fragment & ViewModel** (UI controllers with mock data)
4. **Camera Integration** (Manual dependency addition required)
5. **Advanced UI Features** (Complete feature set)
6. **Room Database** (Final integration after UI proven stable)

### 7.2 Completed Features
**UI Implementation Status:**
- [x] **Phase 1**: UI layouts and resources (XML only)
- [x] **Phase 2**: Data models and mock repository (Kotlin foundation)
- [x] **Phase 3**: Fragment, ViewModel, and adapters (UI controllers)
- [ ] **Phase 4**: Camera integration and add prescription flow
- [ ] **Phase 5**: Advanced UI features and polish
- [ ] **Phase 6**: Room database integration (Final)

### 7.3 Manual Implementation Required
**Dependencies & Permissions (To be added manually in Phase 4):**
- [ ] **Camera Dependencies**: Add 3 camera implementation lines to build.gradle.kts
- [ ] **Permissions**: Add camera and storage permissions to AndroidManifest.xml
- [ ] **Verification**: Confirm dependencies work correctly with existing project

### 7.4 Conflict Prevention Strategy
**Naming Convention Safety:**
- **Layouts**: All prescription layouts use `prescription_` prefix
- **Classes**: All prescription classes use `Prescription` prefix
- **Resources**: All prescription resources use `prescription_` prefix
- **Navigation**: Use unique navigation IDs for prescription flows
- **Validation**: Each phase includes conflict checking with existing code

---

## 8. File Structure Plan - No Conflicts Detected

### 8.1 New Layout Files (Phase 1)
```
app/src/main/res/layout/
├── fragment_prescriptions.xml          # Main prescriptions screen
├── item_prescription_card.xml          # Individual prescription card
├── item_prescription_category_header.xml # Category section headers
├── bottom_sheet_add_prescription.xml   # Add prescription form
└── dialog_prescription_detail.xml      # Full-screen prescription view
```

### 8.2 New Kotlin Files (Phases 2-6)
```
app/src/main/java/com/example/health_assistant/
├── data/
│   ├── model/
│   │   ├── Prescription.kt             # Core data model
│   │   └── DiseaseCategory.kt          # Category data model
│   ├── repository/
│   │   ├── interfaces/
│   │   │   └── PrescriptionRepository.kt # Repository interface
│   │   └── impl/
│   │       ├── MockPrescriptionRepositoryImpl.kt # Mock implementation
│   │       └── RoomPrescriptionRepositoryImpl.kt # Room implementation (Phase 6)
│   └── local/ (Phase 6 only)
│       ├── entity/
│       │   ├── PrescriptionEntity.kt   # Room entity
│       │   └── DiseaseCategoryEntity.kt # Room entity
│       └── dao/
│           ├── PrescriptionDao.kt      # Data Access Object
│           └── DiseaseCategoryDao.kt   # Data Access Object
└── features/
    └── prescriptions/
        ├── PrescriptionsFragment.kt    # Main fragment
        ├── PrescriptionsViewModel.kt   # ViewModel
        ├── adapter/
        │   ├── PrescriptionsAdapter.kt # RecyclerView adapter
        │   ├── PrescriptionViewHolder.kt # Card ViewHolder
        │   └── CategoryHeaderViewHolder.kt # Header ViewHolder
        ├── dialogs/
        │   ├── AddPrescriptionBottomSheet.kt # Add form
        │   ├── PrescriptionDetailDialog.kt # Detail view
        │   └── EditPrescriptionDialog.kt # Edit form
        ├── camera/
        │   └── CameraManager.kt        # Camera handling
        └── utils/
            ├── PrescriptionUtils.kt    # Utility functions
            └── FileManager.kt          # File management
```

### 8.3 Resource Updates (Phase 1)
```
app/src/main/res/
├── values/
│   ├── strings.xml     # Add prescription-related strings
│   ├── dimens.xml      # Add prescription-related dimensions  
│   └── colors.xml      # Add prescription-related colors (if needed)
├── drawable/           # Add prescription-related icons/placeholders
└── navigation/         # Add prescription navigation entries
```

---