package com.example.health_assistant.features.profile

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.UserProfile
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import com.example.health_assistant.features.profile.state.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for EditProfileViewModel
 * Tests state management, validation, and repository interactions
 */
@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class EditProfileViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mockUserProfileRepository: UserProfileRepository
    private lateinit var viewModel: EditProfileViewModel

    private val sampleUserProfile = UserProfile(
        userId = "test-user-id",
        email = "test@example.com",
        displayName = "Test User",
        photoUrl = null,
        birthday = "1990-05-15",
        gender = "Male",
        height = 175f,
        weight = 70f,
        isProfileComplete = true,
        createdAt = System.currentTimeMillis()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockUserProfileRepository = mockk()
        viewModel = EditProfileViewModel(mockUserProfileRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `loadProfile should emit Success state when repository returns valid data`() = runTest {
        // Given
        coEvery { mockUserProfileRepository.getUserProfile() } returns Result.Success(sampleUserProfile)

        // When
        viewModel.loadProfile()

        // Then
        val uiState = viewModel.uiState.first()
        assertTrue("Should emit Success state", uiState is EditProfileUiState.Success)

        val successState = uiState as EditProfileUiState.Success
        assertEquals("Should contain correct user ID", "test-user-id", successState.profile.userId)
        assertEquals("Should contain correct email", "test@example.com", successState.profile.email)
        assertEquals("Should contain correct display name", "Test User", successState.profile.displayName)
    }

    @Test
    fun `loadProfile should emit Error state when repository returns error`() = runTest {
        // Given
        coEvery { mockUserProfileRepository.getUserProfile() } returns Result.Error("Network error")

        // When
        viewModel.loadProfile()

        // Then
        val uiState = viewModel.uiState.first()
        assertTrue("Should emit Error state", uiState is EditProfileUiState.Error)

        val errorState = uiState as EditProfileUiState.Error
        assertEquals("Should contain error message", "Network error", errorState.message)
        assertEquals("Should classify as network error", ErrorCause.NETWORK, errorState.cause)
    }

    @Test
    fun `loadProfile should emit Error state when profile is null`() = runTest {
        // Given
        coEvery { mockUserProfileRepository.getUserProfile() } returns Result.Success(null)

        // When
        viewModel.loadProfile()

        // Then
        val uiState = viewModel.uiState.first()
        assertTrue("Should emit Error state", uiState is EditProfileUiState.Error)

        val errorState = uiState as EditProfileUiState.Error
        assertEquals("Should contain profile not found message", "Profile not found. Please try again.", errorState.message)
    }

    @Test
    fun `updateDisplayName should trigger real-time validation`() = runTest {
        // Given
        val validName = "John Doe"
        val invalidName = ""

        // When - Valid name
        viewModel.updateDisplayName(validName)

        // Then
        assertEquals("Should update current display name", validName, viewModel.currentDisplayName.first())
        val validValidation = viewModel.displayNameValidation.first()
        assertTrue("Should show valid state", validValidation is FieldValidationState.Valid)

        // When - Invalid name
        viewModel.updateDisplayName(invalidName)

        // Then
        assertEquals("Should update current display name", invalidName, viewModel.currentDisplayName.first())
        val invalidValidation = viewModel.displayNameValidation.first()
        assertTrue("Should show invalid state", invalidValidation is FieldValidationState.Invalid)
    }

    @Test
    fun `updateHeight should validate numeric input`() = runTest {
        // Given
        val validHeight = "175"
        val invalidHeight = "1000"

        // When - Valid height
        viewModel.updateHeight(validHeight)

        // Then
        assertEquals("Should update current height", validHeight, viewModel.currentHeight.first())
        val validValidation = viewModel.heightValidation.first()
        assertTrue("Should show valid state", validValidation is FieldValidationState.Valid)

        // When - Invalid height
        viewModel.updateHeight(invalidHeight)

        // Then
        assertEquals("Should update current height", invalidHeight, viewModel.currentHeight.first())
        val invalidValidation = viewModel.heightValidation.first()
        assertTrue("Should show invalid state", invalidValidation is FieldValidationState.Invalid)
    }

    @Test
    fun `updateWeight should validate numeric input`() = runTest {
        // Given
        val validWeight = "70"
        val invalidWeight = "10"

        // When - Valid weight
        viewModel.updateWeight(validWeight)

        // Then
        assertEquals("Should update current weight", validWeight, viewModel.currentWeight.first())
        val validValidation = viewModel.weightValidation.first()
        assertTrue("Should show valid state", validValidation is FieldValidationState.Valid)

        // When - Invalid weight
        viewModel.updateWeight(invalidWeight)

        // Then
        assertEquals("Should update current weight", invalidWeight, viewModel.currentWeight.first())
        val invalidValidation = viewModel.weightValidation.first()
        assertTrue("Should show invalid state", invalidValidation is FieldValidationState.Invalid)
    }

    @Test
    fun `updateProfilePhoto should handle image URI correctly`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        every { mockUri.toString() } returns "content://test-image-uri"

        // When
        viewModel.updateProfilePhoto(mockUri)

        // Then
        assertEquals("Should update photo URL", "content://test-image-uri", viewModel.currentPhotoUrl.first())

        val photoState = viewModel.photoUploadState.first()
        assertTrue("Should show success state", photoState is PhotoUploadState.Success)

        val successState = photoState as PhotoUploadState.Success
        assertEquals("Should contain correct photo URL", "content://test-image-uri", successState.photoUrl)
    }

    @Test
    fun `saveProfile should validate input before saving`() = runTest {
        // Given - Load a profile first
        coEvery { mockUserProfileRepository.getUserProfile() } returns Result.Success(sampleUserProfile)
        viewModel.loadProfile()

        // Set invalid display name
        viewModel.updateDisplayName("")

        // When
        viewModel.saveProfile()

        // Then
        val saveState = viewModel.saveState.first()
        assertTrue("Should emit validation error", saveState is SaveOperationState.Error)

        val errorState = saveState as SaveOperationState.Error
        assertEquals("Should contain validation error", SaveErrorCause.VALIDATION, errorState.cause)
        assertFalse("Should not be retryable", errorState.retryable)
    }

    @Test
    fun `saveProfile should call repository when validation passes`() = runTest {
        // Given - Load a profile first
        coEvery { mockUserProfileRepository.getUserProfile() } returns Result.Success(sampleUserProfile)
        coEvery { mockUserProfileRepository.updateUserProfileInFirestore(any()) } returns Result.Success(Unit)
        coEvery { mockUserProfileRepository.saveUserProfile(any(), any()) } returns Result.Success(Unit)

        viewModel.loadProfile()

        // Set valid data
        viewModel.updateDisplayName("Updated Name")

        // When
        viewModel.saveProfile()

        // Then
        coVerify { mockUserProfileRepository.updateUserProfileInFirestore(any()) }
        coVerify { mockUserProfileRepository.saveUserProfile(any(), any()) }

        val saveState = viewModel.saveState.first()
        assertTrue("Should emit success state", saveState is SaveOperationState.Success)
    }

    @Test
    fun `saveProfile should handle repository errors correctly`() = runTest {
        // Given - Load a profile first
        coEvery { mockUserProfileRepository.getUserProfile() } returns Result.Success(sampleUserProfile)
        coEvery { mockUserProfileRepository.updateUserProfileInFirestore(any()) } returns Result.Error("Firestore error")

        viewModel.loadProfile()
        viewModel.updateDisplayName("Updated Name")

        // When
        viewModel.saveProfile()

        // Then
        val saveState = viewModel.saveState.first()
        assertTrue("Should emit error state", saveState is SaveOperationState.Error)

        val errorState = saveState as SaveOperationState.Error
        assertEquals("Should contain firestore error", SaveErrorCause.FIRESTORE, errorState.cause)
        assertTrue("Should be retryable", errorState.retryable)
    }

    @Test
    fun `hasUnsavedChanges should detect form modifications`() = runTest {
        // Given - Load a profile first
        coEvery { mockUserProfileRepository.getUserProfile() } returns Result.Success(sampleUserProfile)
        viewModel.loadProfile()

        // Initially no changes
        assertFalse("Should have no unsaved changes initially", viewModel.hasUnsavedChanges())

        // When - Make a change
        viewModel.updateDisplayName("Changed Name")

        // Then
        assertTrue("Should detect unsaved changes", viewModel.hasUnsavedChanges())
    }

    @Test
    fun `form state should track validation and changes correctly`() = runTest {
        // Given - Load a profile first
        coEvery { mockUserProfileRepository.getUserProfile() } returns Result.Success(sampleUserProfile)
        viewModel.loadProfile()

        // When - Make valid changes
        viewModel.updateDisplayName("Valid Name")
        viewModel.updateHeight("175")

        // Then
        val formState = viewModel.formState.first()
        assertTrue("Should have changes", formState.hasChanges)
        assertTrue("Should be valid", formState.isValid)
        assertTrue("Should be dirty", formState.isDirty)
    }

    @Test
    fun `loading states should be managed correctly`() = runTest {
        // Given
        coEvery { mockUserProfileRepository.getUserProfile() } returns Result.Success(sampleUserProfile)

        // When
        viewModel.loadProfile()

        // Then - Check loading state progression
        val loadingState = viewModel.loadingState.first()
        assertFalse("Should not be loading after completion", loadingState.isLoadingProfile)
    }

    @Test
    fun `resetSaveState should reset to idle`() = runTest {
        // Given - Force save state to error
        coEvery { mockUserProfileRepository.getUserProfile() } returns Result.Success(sampleUserProfile)
        coEvery { mockUserProfileRepository.updateUserProfileInFirestore(any()) } returns Result.Error("Error")

        viewModel.loadProfile()
        viewModel.updateDisplayName("Test")
        viewModel.saveProfile()

        // Verify error state first
        assertTrue("Should be in error state", viewModel.saveState.first() is SaveOperationState.Error)

        // When
        viewModel.resetSaveState()

        // Then
        assertTrue("Should reset to idle", viewModel.saveState.first() is SaveOperationState.Idle)
    }
}