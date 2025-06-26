package com.example.health_assistant.features.profile.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.health_assistant.data.repository.interfaces.AuthRepository
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: ProfileViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeEach
    fun setup() {
        userProfileRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        every { userProfileRepository.getUserEmail() } returns flowOf("test@email.com")
        every { userProfileRepository.getUserId() } returns flowOf("user123")
        every { authRepository.isUserLoggedIn() } returns true
        viewModel = ProfileViewModel(userProfileRepository, authRepository)
    }

    @Test
    fun `userEmail emits correct value`() {
        val observer = mockk<Observer<String?>>(relaxed = true)
        viewModel.userEmail.observeForever(observer)
        verify { observer.onChanged("test@email.com") }
        viewModel.userEmail.removeObserver(observer)
    }

    @Test
    fun `isUserLoggedIn returns true`() {
        assertEquals(true, viewModel.isUserLoggedIn())
    }

    @Test
    fun `saveUserProfile calls repository`() = testScope.runTest {
        coEvery { userProfileRepository.saveUserProfile(any(), any()) } just Runs
        viewModel.saveUserProfile("user123", "test@email.com")
        coVerify { userProfileRepository.saveUserProfile("user123", "test@email.com") }
    }

    @Test
    fun `signOut calls both repositories`() = testScope.runTest {
        coEvery { authRepository.signOut() } just Runs
        coEvery { userProfileRepository.clearUserProfile() } just Runs
        viewModel.signOut()
        coVerify { authRepository.signOut() }
        coVerify { userProfileRepository.clearUserProfile() }
    }
}