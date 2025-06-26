package com.example.health_assistant.features.onboarding

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private lateinit var repository: OnboardingPreferencesRepository
    private lateinit var viewModel: OnboardingViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeEach
    fun setup() {
        repository = mockk(relaxed = true)
        viewModel = OnboardingViewModel(repository)
    }

    @Test
    fun `setCurrentPage updates currentPage`() = testScope.runTest {
        viewModel.setCurrentPage(2)
        assertEquals(2, viewModel.currentPage.value)
    }

    @Test
    fun `completeOnboarding calls repository`() = testScope.runTest {
        coEvery { repository.setOnboardingCompleted() } returns Unit
        viewModel.completeOnboarding()
        coVerify { repository.setOnboardingCompleted() }
    }

    @Test
    fun `isLastPage returns true for last page`() {
        assertEquals(true, viewModel.isLastPage(2, 3))
    }

    @Test
    fun `isLastPage returns false for non-last page`() {
        assertEquals(false, viewModel.isLastPage(1, 3))
    }
}