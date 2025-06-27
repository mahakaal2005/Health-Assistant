package com.example.health_assistant.data.repository.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.UserProfile
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.tasks.await

/**
 * Unit tests for UserProfileRepositoryImpl Firestore functionality
 */
class UserProfileRepositoryFirestoreTest {

    private lateinit var repository: UserProfileRepositoryImpl
    private val mockDataStore = mockk<DataStore<Preferences>>()
    private val mockFirestore = mockk<FirebaseFirestore>()
    private val mockCollection = mockk<CollectionReference>()
    private val mockDocument = mockk<DocumentReference>()
    private val mockTask = mockk<Task<Void>>()
    private val mockDocumentSnapshot = mockk<DocumentSnapshot>()
    private val mockGetTask = mockk<Task<DocumentSnapshot>>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        // Mock Firestore collection and document structure
        every { mockFirestore.collection("users") } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocument

        repository = UserProfileRepositoryImpl(mockDataStore, mockFirestore)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `createUserProfileInFirestore should save profile successfully`() = runTest {
        // Given
        val userProfile = UserProfile(
            userId = "test_user_123",
            email = "test@example.com",
            displayName = "Test User",
            createdAt = System.currentTimeMillis(),
            isProfileComplete = false
        )

        // Mock successful Firestore write
        every { mockDocument.set(any()) } returns mockTask
        coEvery { mockTask.await() } returns mockk()

        // Mock DataStore save (called internally)
        coEvery { mockDataStore.edit(any()) } returns mockk()

        // When
        val result = repository.createUserProfileInFirestore(userProfile)

        // Then
        assertTrue("Result should be success", result is Result.Success)
        verify { mockDocument.set(any()) }
        coVerify { mockTask.await() }
    }

    @Test
    fun `createUserProfileInFirestore should handle Firestore failure`() = runTest {
        // Given
        val userProfile = UserProfile(
            userId = "test_user_123",
            email = "test@example.com",
            createdAt = System.currentTimeMillis()
        )

        // Mock Firestore write failure
        every { mockDocument.set(any()) } returns mockTask
        coEvery { mockTask.await() } throws Exception("Firestore error")

        // When
        val result = repository.createUserProfileInFirestore(userProfile)

        // Then
        assertTrue("Result should be error", result is Result.Error)
        assertEquals("Failed to create user profile in Firestore", (result as Result.Error).message)
    }

    @Test
    fun `syncUserProfileFromFirestore should return profile when document exists`() = runTest {
        // Given
        val userId = "test_user_123"
        val profileData = mapOf(
            "uid" to "test_user_123",
            "email" to "test@example.com",
            "displayName" to "Test User",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "isProfileComplete" to false
        )

        // Mock successful Firestore read
        every { mockDocument.get() } returns mockGetTask
        coEvery { mockGetTask.await() } returns mockDocumentSnapshot
        every { mockDocumentSnapshot.exists() } returns true
        every { mockDocumentSnapshot.data } returns profileData

        // Mock DataStore update (called internally)
        coEvery { mockDataStore.edit(any()) } returns mockk()

        // When
        val result = repository.syncUserProfileFromFirestore(userId)

        // Then
        assertTrue("Result should be success", result is Result.Success)
        val userProfile = (result as Result.Success).data
        assertNotNull("User profile should not be null", userProfile)
        assertEquals("User ID should match", "test_user_123", userProfile?.userId)
        assertEquals("Email should match", "test@example.com", userProfile?.email)
    }

    @Test
    fun `syncUserProfileFromFirestore should return null when document does not exist`() = runTest {
        // Given
        val userId = "test_user_123"

        // Mock document not found
        every { mockDocument.get() } returns mockGetTask
        coEvery { mockGetTask.await() } returns mockDocumentSnapshot
        every { mockDocumentSnapshot.exists() } returns false

        // When
        val result = repository.syncUserProfileFromFirestore(userId)

        // Then
        assertTrue("Result should be success", result is Result.Success)
        assertNull("User profile should be null", (result as Result.Success).data)
    }

    @Test
    fun `syncUserProfileFromFirestore should handle Firestore read failure`() = runTest {
        // Given
        val userId = "test_user_123"

        // Mock Firestore read failure
        every { mockDocument.get() } returns mockGetTask
        coEvery { mockGetTask.await() } throws Exception("Network error")

        // When
        val result = repository.syncUserProfileFromFirestore(userId)

        // Then
        assertTrue("Result should be error", result is Result.Error)
        assertEquals("Failed to sync user profile from Firestore", (result as Result.Error).message)
    }

    @Test
    fun `updateUserProfileInFirestore should update profile successfully`() = runTest {
        // Given
        val userProfile = UserProfile(
            userId = "test_user_123",
            email = "test@example.com",
            displayName = "Updated User",
            gender = "Male",
            height = 175.0f,
            weight = 70.0f,
            isProfileComplete = true
        )

        // Mock successful Firestore update
        every { mockDocument.set(any(), any()) } returns mockTask
        coEvery { mockTask.await() } returns mockk()

        // When
        val result = repository.updateUserProfileInFirestore(userProfile)

        // Then
        assertTrue("Result should be success", result is Result.Success)
        verify { mockDocument.set(any(), any()) }
        coVerify { mockTask.await() }
    }

    @Test
    fun `updateUserProfileInFirestore should handle update failure`() = runTest {
        // Given
        val userProfile = UserProfile(
            userId = "test_user_123",
            email = "test@example.com"
        )

        // Mock Firestore update failure
        every { mockDocument.set(any(), any()) } returns mockTask
        coEvery { mockTask.await() } throws Exception("Update failed")

        // When
        val result = repository.updateUserProfileInFirestore(userProfile)

        // Then
        assertTrue("Result should be error", result is Result.Error)
        assertEquals("Failed to update user profile in Firestore", (result as Result.Error).message)
    }
}