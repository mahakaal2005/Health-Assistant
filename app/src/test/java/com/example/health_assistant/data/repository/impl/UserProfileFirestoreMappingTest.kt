package com.example.health_assistant.data.repository.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.UserProfile
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.Timestamp
import com.google.android.gms.tasks.Task
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests to verify Firestore field mapping with proper null handling
 */
class UserProfileFirestoreMappingTest {

    private lateinit var mockDataStore: DataStore<Preferences>
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockCollection: CollectionReference
    private lateinit var mockDocument: DocumentReference
    private lateinit var mockDocumentSnapshot: DocumentSnapshot
    private lateinit var repository: UserProfileRepositoryImpl

    @Before
    fun setup() {
        mockDataStore = mockk(relaxed = true)
        mockFirestore = mockk()
        mockCollection = mockk()
        mockDocument = mockk()
        mockDocumentSnapshot = mockk()

        every { mockFirestore.collection("users") } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocument

        // Use relaxed mocking for DataStore to avoid complex setup
        every { mockDataStore.data } returns flowOf(mockk(relaxed = true))

        repository = UserProfileRepositoryImpl(mockDataStore, mockFirestore)
    }

    @Test
    fun `createUserProfileInFirestore should map null fields to proper defaults`() = runTest {
        // Given: UserProfile with null optional fields
        val userProfile = UserProfile(
            userId = "test-user-id",
            email = "test@example.com",
            displayName = null, // null field
            photoUrl = null, // null field
            createdAt = System.currentTimeMillis(),
            gender = null, // null field
            height = null, // null field
            weight = null, // null field
            birthday = null, // null field
            isProfileComplete = false
        )

        // Mock Firestore set operation
        val mockVoidTask: Task<Void> = mockk()
        every { mockDocument.set(any()) } returns mockVoidTask
        coEvery { mockVoidTask.await() } returns mockk<Void>()

        // Capture the data being sent to Firestore
        val capturedData = slot<Map<String, Any>>()
        every { mockDocument.set(capture(capturedData)) } returns mockVoidTask

        // When: Creating user profile in Firestore
        val result = repository.createUserProfileInFirestore(userProfile)

        // Then: Verify result is successful
        assertTrue("Result should be success", result is Result.Success)

        // Verify that null fields are mapped to proper defaults
        val firestoreData = capturedData.captured
        assertEquals("Empty string for null displayName", "", firestoreData["displayName"])
        assertEquals("Empty string for null photoUrl", "", firestoreData["photoUrl"])
        assertEquals("Empty string for null gender", "", firestoreData["gender"])
        assertEquals("Zero for null height", 0.0f, firestoreData["height"])
        assertEquals("Zero for null weight", 0.0f, firestoreData["weight"])
        assertEquals("Empty string for null birthday", "", firestoreData["birthday"])

        // Verify required fields are preserved
        assertEquals("User ID preserved", "test-user-id", firestoreData["uid"])
        assertEquals("Email preserved", "test@example.com", firestoreData["email"])
        assertEquals("Profile complete flag preserved", false, firestoreData["isProfileComplete"])

        // Verify preferences are included
        val preferences = firestoreData["preferences"] as Map<*, *>
        assertEquals("Default notifications preference", true, preferences["notifications"])
        assertEquals("Default units preference", "metric", preferences["units"])
    }

    @Test
    fun `syncUserProfileFromFirestore should convert defaults back to null`() = runTest {
        // Given: Firestore document with default values (empty strings and zeros)
        val firestoreData = mapOf(
            "uid" to "test-user-id",
            "email" to "test@example.com",
            "displayName" to "", // Empty string should become null
            "photoUrl" to "", // Empty string should become null
            "createdAt" to Timestamp.now(),
            "gender" to "", // Empty string should become null
            "height" to 0.0f, // Zero should become null
            "weight" to 0.0f, // Zero should become null
            "birthday" to "", // Empty string should become null
            "isProfileComplete" to false
        )

        // Mock Firestore get operation
        val mockDocumentTask: Task<DocumentSnapshot> = mockk()
        every { mockDocument.get() } returns mockDocumentTask
        coEvery { mockDocumentTask.await() } returns mockDocumentSnapshot
        every { mockDocumentSnapshot.exists() } returns true
        every { mockDocumentSnapshot.data } returns firestoreData

        // When: Syncing user profile from Firestore
        val result = repository.syncUserProfileFromFirestore("test-user-id")

        // Then: Verify result is successful
        assertTrue("Result should be success", result is Result.Success)

        val userProfile = (result as Result.Success).data
        assertNotNull("UserProfile should not be null", userProfile)

        // Verify that default values are converted back to null
        assertNull("Empty displayName should be null", userProfile!!.displayName)
        assertNull("Empty photoUrl should be null", userProfile.photoUrl)
        assertNull("Empty gender should be null", userProfile.gender)
        assertNull("Zero height should be null", userProfile.height)
        assertNull("Zero weight should be null", userProfile.weight)
        assertNull("Empty birthday should be null", userProfile.birthday)

        // Verify required fields are preserved
        assertEquals("User ID preserved", "test-user-id", userProfile.userId)
        assertEquals("Email preserved", "test@example.com", userProfile.email)
        assertEquals("Profile complete flag preserved", false, userProfile.isProfileComplete)
    }

    @Test
    fun `syncUserProfileFromFirestore should preserve actual values`() = runTest {
        // Given: Firestore document with actual values
        val firestoreData = mapOf(
            "uid" to "test-user-id",
            "email" to "test@example.com",
            "displayName" to "John Doe",
            "photoUrl" to "https://example.com/photo.jpg",
            "createdAt" to Timestamp.now(),
            "gender" to "Male",
            "height" to 175.5f,
            "weight" to 70.2f,
            "birthday" to "1990-01-01",
            "isProfileComplete" to true
        )

        // Mock Firestore get operation
        val mockDocumentTask: Task<DocumentSnapshot> = mockk()
        every { mockDocument.get() } returns mockDocumentTask
        coEvery { mockDocumentTask.await() } returns mockDocumentSnapshot
        every { mockDocumentSnapshot.exists() } returns true
        every { mockDocumentSnapshot.data } returns firestoreData

        // When: Syncing user profile from Firestore
        val result = repository.syncUserProfileFromFirestore("test-user-id")

        // Then: Verify result is successful
        assertTrue("Result should be success", result is Result.Success)

        val userProfile = (result as Result.Success).data
        assertNotNull("UserProfile should not be null", userProfile)

        // Verify that actual values are preserved
        assertEquals("DisplayName preserved", "John Doe", userProfile!!.displayName)
        assertEquals("PhotoUrl preserved", "https://example.com/photo.jpg", userProfile.photoUrl)
        assertEquals("Gender preserved", "Male", userProfile.gender)
        assertEquals("Height preserved", 175.5f, userProfile.height)
        assertEquals("Weight preserved", 70.2f, userProfile.weight)
        assertEquals("Birthday preserved", "1990-01-01", userProfile.birthday)
        assertEquals("Profile complete preserved", true, userProfile.isProfileComplete)
    }
}