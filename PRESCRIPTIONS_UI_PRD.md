# Prescriptions Management UI - Developer Implementation Guide

## Technical Overview
**Feature**: Digital Prescription Management System  
**Integration**: Health Assistant Android App  
**Development Phase**: UI-First Implementation → Database Integration  
**Architecture**: MVVM + Repository + Hilt DI  

## Implementation Strategy

### Phase 1: UI Implementation (Current)
- Mock data with sealed classes
- In-memory storage using StateFlow
- Complete UI/UX implementation
- Zero impact on existing Google Fit integration

### Phase 2: Database Integration (Future)
- Room database entities
- Migration from mock data
- Cloud sync with Firebase

## Technical Specifications

### Data Models
```kotlin
// Mock data structure for Phase 1
data class Prescription(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val imageUri: String? = null,
    val category: PrescriptionCategory,
    val dateAdded: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val doctorName: String? = null,
    val dosage: String? = null,
    val frequency: String? = null
)

enum class PrescriptionCategory(val displayName: String) {
    CARDIOLOGY("Heart & Blood"),
    DIABETES("Diabetes"),
    RESPIRATORY("Respiratory"), 
    NEUROLOGICAL("Neurological"),
    DERMATOLOGY("Skin & Allergies"),
    ORTHOPEDIC("Bones & Joints"),
    GENERAL("General Medicine"),
    EMERGENCY("Emergency")
}

// Repository interface
interface PrescriptionRepository {
    fun getAllPrescriptions(): Flow<List<Prescription>>
    fun getPrescriptionsByCategory(category: PrescriptionCategory): Flow<List<Prescription>>
    suspend fun addPrescription(prescription: Prescription): Result<Unit>
    suspend fun deletePrescription(id: String): Result<Unit>
    suspend fun searchPrescriptions(query: String): List<Prescription>
}
```

### UI Components Architecture

#### PrescriptionsFragment.kt
```kotlin
@AndroidEntryPoint
class PrescriptionsFragment : Fragment() {
    private val viewModel: PrescriptionsViewModel by viewModels()
    private lateinit var binding: FragmentPrescriptionsBinding
    private lateinit var prescriptionsAdapter: PrescriptionsAdapter
    
    // Image capture for prescriptions
    private lateinit var imageCaptureLauncher: ActivityResultLauncher<Intent>
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupImageCapture()
        setupSearch()
        setupFAB()
        observeViewModel()
    }
}
```

#### Key UI Features
1. **Grid/List Toggle**: Switch between card grid and detailed list view
2. **Category Filtering**: Horizontal scrollable category chips
3. **Search Functionality**: Real-time search with debouncing
4. **Image Capture**: Camera integration for prescription photos
5. **Sorting Options**: By date, name, category
6. **Swipe Actions**: Delete and edit gestures

### ViewModels & Business Logic

#### PrescriptionsViewModel.kt
```kotlin
@HiltViewModel
class PrescriptionsViewModel @Inject constructor(
    private val prescriptionRepository: PrescriptionRepository
) : ViewModel() {
    
    private val _prescriptions = MutableLiveData<List<Prescription>>()
    val prescriptions: LiveData<List<Prescription>> = _prescriptions
    
    private val _selectedCategory = MutableLiveData<PrescriptionCategory?>()
    val selectedCategory: LiveData<PrescriptionCategory?> = _selectedCategory
    
    private val _searchQuery = MutableLiveData<String>()
    val searchQuery: LiveData<String> = _searchQuery
    
    // Combine filters for reactive UI
    val filteredPrescriptions: LiveData<List<Prescription>> = 
        MediatorLiveData<List<Prescription>>().apply {
            addSource(prescriptions) { updateFilteredList() }
            addSource(selectedCategory) { updateFilteredList() }
            addSource(searchQuery) { updateFilteredList() }
        }
    
    fun addPrescription(prescription: Prescription) {
        viewModelScope.launch {
            prescriptionRepository.addPrescription(prescription)
        }
    }
    
    fun deletePrescription(id: String) {
        viewModelScope.launch {
            prescriptionRepository.deletePrescription(id)
        }
    }
}
```

### Repository Implementation (Phase 1 - Mock Data)

#### PrescriptionRepositoryImpl.kt
```kotlin
@Singleton
class PrescriptionRepositoryImpl @Inject constructor() : PrescriptionRepository {
    
    private val _prescriptions = MutableStateFlow(generateMockPrescriptions())
    
    override fun getAllPrescriptions(): Flow<List<Prescription>> = _prescriptions
    
    override fun getPrescriptionsByCategory(category: PrescriptionCategory): Flow<List<Prescription>> {
        return _prescriptions.map { list ->
            list.filter { it.category == category }
        }
    }
    
    override suspend fun addPrescription(prescription: Prescription): Result<Unit> {
        return try {
            val currentList = _prescriptions.value.toMutableList()
            currentList.add(prescription)
            _prescriptions.value = currentList
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to add prescription: ${e.message}")
        }
    }
    
    override suspend fun deletePrescription(id: String): Result<Unit> {
        return try {
            val currentList = _prescriptions.value.toMutableList()
            currentList.removeAll { it.id == id }
            _prescriptions.value = currentList
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete prescription: ${e.message}")
        }
    }
    
    private fun generateMockPrescriptions(): List<Prescription> {
        return listOf(
            Prescription(
                name = "Lisinopril 10mg",
                category = PrescriptionCategory.CARDIOLOGY,
                doctorName = "Dr. Smith",
                dosage = "10mg",
                frequency = "Once daily"
            ),
            Prescription(
                name = "Metformin 500mg", 
                category = PrescriptionCategory.DIABETES,
                doctorName = "Dr. Johnson",
                dosage = "500mg",
                frequency = "Twice daily"
            ),
            // ... more mock data
        )
    }
}
```

### UI Layouts & Design System

#### Fragment Layout (fragment_prescriptions.xml)
```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout>
    
    <!-- App Bar with Search -->
    <com.google.android.material.appbar.AppBarLayout>
        <androidx.appcompat.widget.Toolbar/>
        <com.google.android.material.textfield.TextInputLayout>
            <!-- Search EditText -->
        </com.google.android.material.textfield.TextInputLayout>
    </com.google.android.material.appbar.AppBarLayout>
    
    <!-- Category Filter Chips -->
    <com.google.android.material.chip.ChipGroup
        android:id="@+id/categoryChips"
        app:singleSelection="true"/>
    
    <!-- Prescriptions RecyclerView -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/prescriptionsRecycler"
        app:layoutManager="androidx.recyclerview.widget.GridLayoutManager"
        app:spanCount="2"/>
    
    <!-- Floating Action Button -->
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabAddPrescription"/>
        
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

#### Prescription Item Layout (item_prescription.xml)
```xml
<com.google.android.material.card.MaterialCardView>
    
    <LinearLayout android:orientation="vertical">
        
        <!-- Prescription Image -->
        <ImageView 
            android:id="@+id/prescriptionImage"
            android:scaleType="centerCrop"/>
        
        <!-- Prescription Details -->
        <TextView android:id="@+id/prescriptionName"
            style="@style/TextAppearance.Material3.HeadlineSmall"/>
            
        <TextView android:id="@+id/prescriptionCategory"
            style="@style/TextAppearance.Material3.BodyMedium"/>
            
        <TextView android:id="@+id/prescriptionDoctor"
            style="@style/TextAppearance.Material3.BodySmall"/>
        
        <!-- Action Buttons -->
        <LinearLayout android:orientation="horizontal">
            <Button android:id="@+id/btnEdit" style="@style/Widget.Material3.Button.TextButton"/>
            <Button android:id="@+id/btnDelete" style="@style/Widget.Material3.Button.TextButton"/>
        </LinearLayout>
        
    </LinearLayout>
    
</com.google.android.material.card.MaterialCardView>
```

### Integration with Existing App

#### Navigation Integration
```kotlin
// In navigation graph (nav_graph.xml)
<fragment
    android:id="@+id/prescriptionsFragment"
    android:name="com.example.health_assistant.features.prescriptions.PrescriptionsFragment"
    android:label="My Prescriptions">
    
    <action
        android:id="@+id/action_prescriptions_to_add_prescription"
        app:destination="@id/addPrescriptionFragment"/>
        
</fragment>

// In HomeFragment.kt - Quick Action Button
binding.prescriptionsButton.setOnClickListener {
    findNavController().navigate(R.id.action_homeFragment_to_prescriptionsFragment)
}
```

#### Hilt Module Setup
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class PrescriptionModule {
    
    @Binds
    abstract fun bindPrescriptionRepository(
        prescriptionRepositoryImpl: PrescriptionRepositoryImpl
    ): PrescriptionRepository
}
```

## Development Milestones

### Milestone 1: Core UI (Week 1)
- [ ] Fragment layout and navigation
- [ ] RecyclerView with mock data
- [ ] Basic MVVM setup
- [ ] Category filtering chips

### Milestone 2: Advanced Features (Week 2)  
- [ ] Image capture integration
- [ ] Search functionality
- [ ] Add/Edit prescription dialogs
- [ ] Swipe to delete

### Milestone 3: Polish & Testing (Week 3)
- [ ] UI animations and transitions
- [ ] Error handling
- [ ] Unit tests for ViewModel
- [ ] UI tests for key flows

### Milestone 4: Database Preparation (Week 4)
- [ ] Room entity definitions
- [ ] Migration strategy from mock data
- [ ] Repository interface extensions

## Testing Strategy

### Unit Tests
```kotlin
class PrescriptionsViewModelTest {
    
    @Test
    fun `when prescription added, should update prescriptions list`() {
        // Test ViewModel logic
    }
    
    @Test
    fun `when category filter applied, should show filtered results`() {
        // Test filtering logic
    }
}
```

### UI Tests
```kotlin
class PrescriptionsFragmentTest {
    
    @Test
    fun `should display prescriptions in grid layout`() {
        // Test RecyclerView display
    }
    
    @Test
    fun `should filter prescriptions when category selected`() {
        // Test category filtering UI
    }
}
```

## Performance Considerations

### Image Handling
- Use Coil for efficient image loading
- Implement image compression for captured photos
- Cache prescription images locally

### RecyclerView Optimization
- Use DiffUtil for efficient list updates
- Implement view recycling properly
- Consider pagination for large prescription lists

### Memory Management
- Use WeakReference for image callbacks
- Implement proper lifecycle awareness
- Clear resources in onDestroyView()

## Future Enhancements (Phase 2)

### Room Database Schema
```kotlin
@Entity(tableName = "prescriptions")
data class PrescriptionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imagePath: String?,
    val category: String,
    val dateAdded: Long,
    val doctorName: String?,
    val dosage: String?,
    val frequency: String?
)

@Dao
interface PrescriptionDao {
    @Query("SELECT * FROM prescriptions")
    fun getAllPrescriptions(): Flow<List<PrescriptionEntity>>
    
    @Insert
    suspend fun insertPrescription(prescription: PrescriptionEntity)
    
    @Delete
    suspend fun deletePrescription(prescription: PrescriptionEntity)
}
```

This developer guide provides complete implementation details for the Prescriptions UI feature while maintaining zero impact on the existing Google Fit integration.