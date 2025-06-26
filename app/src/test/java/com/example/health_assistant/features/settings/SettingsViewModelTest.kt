package com.example.health_assistant.features.settings

import app.cash.turbine.test
import com.example.health_assistant.features.settings.data.SettingsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private lateinit var repository: SettingsRepository
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeEach
    fun setup() {
        repository = mockk(relaxed = true)
        // Mock repository flows
        coEvery { repository.userName } returns MutableStateFlow("Test User")
        coEvery { repository.userAge } returns MutableStateFlow(30)
        coEvery { repository.userGender } returns MutableStateFlow("Female")
        coEvery { repository.userHealthGoal } returns MutableStateFlow("Lose weight")
        coEvery { repository.userHealthStatus } returns MutableStateFlow("Good")
        coEvery { repository.avatarUri } returns MutableStateFlow(null)
        coEvery { repository.stepGoal } returns MutableStateFlow(10000)
        // Add more mocks as needed
        viewModel = SettingsViewModel(repository)
    }

    @Test
    fun `userName emits correct value`() = testScope.runTest {
        viewModel.userName.test {
            assertEquals("Test User", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `userAgeGenderText combines age and gender`() = testScope.runTest {
        viewModel.userAgeGenderText.test {
            assertEquals("30 Years • Female", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Add more tests for edge cases, e.g., null avatarUri, default values, etc.
}