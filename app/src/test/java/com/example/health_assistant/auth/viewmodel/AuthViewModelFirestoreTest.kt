package com.example.health_assistant.auth.viewmodel

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.AuthRepository
import com.example.health_assistant.data.repository.interfaces.UserProfile
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AuthViewModel with Firestore integration
 */
@ExperimentalCoroutinesApi
class AuthViewModelFirestoreTest {

    private lateinit var viewModel: AuthViewModel
    private val mockAuthRepository = mockk<AuthRepository>()
    private val mockUserProfileRepository = mockk<UserProfileRepository>()
    private val mockFirebaseUser = mockk<FirebaseUser>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock getCurrentUser flow
        every { mockAuthRepository.getCurrentUser() } returns flowOf(Result.Success(null))

        viewModel = AuthViewModel(mockAuthRepository, mockUserProfileRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `registerUser should create Firestore profile on successful signup`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val userId = "test_user_123"

        every { mockFirebaseUser.uid } returns userId
        every { mockFirebaseUser.email } returns email
        every { mockFirebaseUser.displayName } returns null
        every { mockFirebaseUser.photoUrl } returns null

        coEvery { mockAuthRepository.registerUser(email, password) } returns Result.Success(mockFirebaseUser)
        coEvery { mockUserProfileRepository.createUserProfileInFirestore(any()) } returns Result.Success(Unit)

        // When
        viewModel.registerUser(email, password)

        // Then
        assertEquals(AuthState.Success, viewModel.authState.value)
        coVerify {
            mockUserProfileRepository.createUserProfileInFirestore(
                match { profile ->
                    profile.userId == userId &&
                    profile.email == email &&
                    !profile.isProfileComplete
                }
            )
        }
    }

    @Test
    fun `registerUser should fallback to local storage when Firestore fails`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val userId = "test_user_123"

        every { mockFirebaseUser.uid } returns userId
        every { mockFirebaseUser.email } returns email
        every { mockFirebaseUser.displayName } returns null
        every { mockFirebaseUser.photoUrl } returns null

        coEvery { mockAuthRepository.registerUser(email, password) } returns Result.Success(mockFirebaseUser)
        coEvery { mockUserProfileRepository.createUserProfileInFirestore(any()) } returns Result.Error(Exception("Firestore failed"))
        coEvery { mockUserProfileRepository.saveUserProfile(userId, email) } returns Result.Success(Unit)

        // When
        viewModel.registerUser(email, password)

        // Then
        assertEquals(AuthState.Success, viewModel.authState.value)
        coVerify { mockUserProfileRepository.createUserProfileInFirestore(any()) }
        coVerify { mockUserProfileRepository.saveUserProfile(userId, email) }
    }

    @Test
    fun `registerUser should handle auth failure properly`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val errorMessage = "Invalid credentials"

        coEvery { mockAuthRepository.registerUser(email, password) } returns Result.Error(Exception(errorMessage))

        // When
        viewModel.registerUser(email, password)

        // Then
        assertTrue(viewModel.authState.value is AuthState.Error)
        assertEquals(errorMessage, (viewModel.authState.value as AuthState.Error).message)
        coVerify(exactly = 0) { mockUserProfileRepository.createUserProfileInFirestore(any()) }
    }

    @Test
    fun `signInUser should sync profile from Firestore on successful login`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val userId = "test_user_123"

        every { mockFirebaseUser.uid } returns userId
        every { mockFirebaseUser.email } returns email

        coEvery { mockAuthRepository.signInUser(email, password) } returns Result.Success(mockFirebaseUser)
        coEvery { mockUserProfileRepository.syncUserProfileFromFirestore(userId) } returns Result.Success(mockk<UserProfile>())

        // When
        viewModel.signInUser(email, password)

        // Then
        assertEquals(AuthState.Success, viewModel.authState.value)
        coVerify { mockUserProfileRepository.syncUserProfileFromFirestore(userId) }
    }

    @Test
    fun `signInUser should fallback to local storage when Firestore sync fails`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val userId = "test_user_123"

        every { mockFirebaseUser.uid } returns userId
        every { mockFirebaseUser.email } returns email

        coEvery { mockAuthRepository.signInUser(email, password) } returns Result.Success(mockFirebaseUser)
        coEvery { mockUserProfileRepository.syncUserProfileFromFirestore(userId) } returns Result.Error(Exception("Sync failed"))
        coEvery { mockUserProfileRepository.saveUserProfile(userId, email) } returns Result.Success(Unit)

        // When
        viewModel.signInUser(email, password)

        // Then
        assertEquals(AuthState.Success, viewModel.authState.value)
        coVerify { mockUserProfileRepository.syncUserProfileFromFirestore(userId) }
        coVerify { mockUserProfileRepository.saveUserProfile(userId, email) }
    }

    @Test
    fun `signInUser should handle auth failure properly`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val errorMessage = "Invalid credentials"

        coEvery { mockAuthRepository.signInUser(email, password) } returns Result.Error(Exception(errorMessage))

        // When
        viewModel.signInUser(email, password)

        // Then
        assertTrue(viewModel.authState.value is AuthState.Error)
        assertEquals(errorMessage, (viewModel.authState.value as AuthState.Error).message)
        coVerify(exactly = 0) { mockUserProfileRepository.syncUserProfileFromFirestore(any()) }
    }

    @Test
    fun `isProfileComplete should handle repository error gracefully`() = runTest {
        // Given
        coEvery { mockUserProfileRepository.isProfileComplete() } returns Result.Error(Exception("Error"))

        // When
        val result = viewModel.isProfileComplete()

        // Then
        assertFalse(result) // Should return false on error
    }
}