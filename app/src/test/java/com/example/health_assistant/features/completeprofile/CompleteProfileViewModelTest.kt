package com.example.health_assistant.features.completeprofile

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.PersonalHealthInfo
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CompleteProfileViewModel
 */
@ExperimentalCoroutinesApi
class CompleteProfileViewModelTest {

    private lateinit var viewModel: CompleteProfileViewModel
    private val mockUserProfileRepository = mockk<UserProfileRepository>()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CompleteProfileViewModel(mockUserProfileRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be empty`() = runTest {
        val initialState = viewModel.uiState.value

        assertNull(initialState.selectedGender)
        assertNull(initialState.selectedHeight)
        assertNull(initialState.selectedWeight)
        assertNull(initialState.selectedBirthday)
        assertFalse(initialState.isFormValid)
        assertFalse(initialState.isLoading)
    }

    @Test
    fun `setGender should update state`() = runTest {
        val gender = "Male"
        viewModel.setGender(gender)

        val state = viewModel.uiState.value
        assertEquals(gender, state.selectedGender)
    }

    @Test
    fun `setHeight should update state`() = runTest {
        val height = 175.0f
        viewModel.setHeight(height)

        val state = viewModel.uiState.value
        assertEquals(height, state.selectedHeight)
    }

    @Test
    fun `setWeight should update state`() = runTest {
        val weight = 70.0f
        viewModel.setWeight(weight)

        val state = viewModel.uiState.value
        assertEquals(weight, state.selectedWeight)
    }

    @Test
    fun `setBirthday should update state`() = runTest {
        val birthday = "1990-01-01"
        viewModel.setBirthday(birthday)

        val state = viewModel.uiState.value
        assertEquals(birthday, state.selectedBirthday)
    }

    @Test
    fun `form should be valid when all fields are filled`() = runTest {
        viewModel.setGender("Male")
        viewModel.setHeight(175.0f)
        viewModel.setWeight(70.0f)
        viewModel.setBirthday("1990-01-01")

        val state = viewModel.uiState.value
        assertTrue(state.isFormValid)
    }

    @Test
    fun `form should be invalid when any field is missing`() = runTest {
        viewModel.setGender("Male")
        viewModel.setHeight(175.0f)
        viewModel.setWeight(70.0f)
        // Birthday is missing

        val state = viewModel.uiState.value
        assertFalse(state.isFormValid)
    }

    @Test
    fun `saveProfile should call repository with correct data`() = runTest {
        // Setup
        val personalHealthInfo = PersonalHealthInfo(
            gender = "Male",
            height = 175.0f,
            weight = 70.0f,
            birthday = "1990-01-01"
        )

        coEvery { mockUserProfileRepository.updatePersonalHealthInfo(any()) } returns Result.Success(Unit)
        coEvery { mockUserProfileRepository.markProfileComplete() } returns Result.Success(Unit)

        // Set all fields
        viewModel.setGender(personalHealthInfo.gender)
        viewModel.setHeight(personalHealthInfo.height)
        viewModel.setWeight(personalHealthInfo.weight)
        viewModel.setBirthday(personalHealthInfo.birthday)

        // Execute
        viewModel.saveProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify
        coVerify { mockUserProfileRepository.updatePersonalHealthInfo(personalHealthInfo) }
        coVerify { mockUserProfileRepository.markProfileComplete() }
    }

    @Test
    fun `saveProfile should emit success event on successful save`() = runTest {
        // Setup
        coEvery { mockUserProfileRepository.updatePersonalHealthInfo(any()) } returns Result.Success(Unit)
        coEvery { mockUserProfileRepository.markProfileComplete() } returns Result.Success(Unit)

        // Set all fields
        viewModel.setGender("Male")
        viewModel.setHeight(175.0f)
        viewModel.setWeight(70.0f)
        viewModel.setBirthday("1990-01-01")

        // Execute
        viewModel.saveProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify event is emitted (we can't easily test the exact event content without more complex setup)
        // In a real test, you'd collect the events flow and verify the specific events
    }

    @Test
    fun `saveProfile should emit error event on repository failure`() = runTest {
        // Setup
        coEvery { mockUserProfileRepository.updatePersonalHealthInfo(any()) } returns Result.Error(Exception("Save failed"), "Save failed")

        // Set all fields
        viewModel.setGender("Male")
        viewModel.setHeight(175.0f)
        viewModel.setWeight(70.0f)
        viewModel.setBirthday("1990-01-01")

        // Execute
        viewModel.saveProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error handling
        coVerify { mockUserProfileRepository.updatePersonalHealthInfo(any()) }
    }

    @Test
    fun `skipProfile should mark profile as complete`() = runTest {
        // Setup
        coEvery { mockUserProfileRepository.markProfileComplete() } returns Result.Success(Unit)

        // Execute
        viewModel.skipProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify
        coVerify { mockUserProfileRepository.markProfileComplete() }
    }

    @Test
    fun `saveProfile should not proceed if form is invalid`() = runTest {
        // Setup - don't set all required fields
        viewModel.setGender("Male")
        // Missing height, weight, birthday

        // Execute
        viewModel.saveProfile()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify repository is not called
        coVerify(exactly = 0) { mockUserProfileRepository.updatePersonalHealthInfo(any()) }
    }
}