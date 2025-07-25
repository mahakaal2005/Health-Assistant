package com.example.health_assistant.auth.components

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.health_assistant.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for profile information display styling
 * Tests the application of HealthTypography and HealthSpacing design system tokens
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ProfileInformationDisplayTest {

    private lateinit var context: Context
    private lateinit var inflater: LayoutInflater

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inflater = LayoutInflater.from(context)
    }

    @Test
    fun `profile title uses HealthTypography Title Medium style`() {
        // Arrange & Act
        val profileView = inflater.inflate(R.layout.fragment_profile, null)
        val profileTitle = profileView.findViewById<TextView>(R.id.profile_title)

        // Assert
        assertNotNull(profileTitle, "Profile title should exist")
        
        // Verify text is set correctly
        assertEquals("Profile", profileTitle.text.toString())
        assertTrue(profileTitle.textSize > 0, "Profile title should have text size set")
    }

    @Test
    fun `profile subtitle uses HealthTypography Label Medium style`() {
        // Arrange & Act
        val profileView = inflater.inflate(R.layout.fragment_profile, null)
        val profileSubtitle = profileView.findViewById<TextView>(R.id.profile_subtitle)

        // Assert
        assertNotNull(profileSubtitle, "Profile subtitle should exist")
        
        // Verify text is set correctly
        assertEquals("Manage your health profile", profileSubtitle.text.toString())
        assertTrue(profileSubtitle.textSize > 0, "Profile subtitle should have text size set")
    }

    @Test
    fun `user name uses HealthTypography Body Large style`() {
        // Arrange & Act
        val profileView = inflater.inflate(R.layout.fragment_profile, null)
        val userName = profileView.findViewById<TextView>(R.id.user_full_name)

        // Assert
        assertNotNull(userName, "User name should exist")
        
        // Verify text is set correctly
        assertEquals("Alex Johnson", userName.text.toString())
        assertTrue(userName.textSize > 0, "User name should have text size set")
    }

    @Test
    fun `user bio uses HealthTypography Label Medium style`() {
        // Arrange & Act
        val profileView = inflater.inflate(R.layout.fragment_profile, null)
        val userBio = profileView.findViewById<TextView>(R.id.user_bio)

        // Assert
        assertNotNull(userBio, "User bio should exist")
        
        // Verify text is set correctly
        assertEquals("Add your bio in Edit Profile", userBio.text.toString())
        assertTrue(userBio.textSize > 0, "User bio should have text size set")
    }

    @Test
    fun `profile layout uses HealthSpacing tokens consistently`() {
        // Arrange & Act
        val profileView = inflater.inflate(R.layout.fragment_profile, null)
        val userName = profileView.findViewById<TextView>(R.id.user_full_name)
        val userBio = profileView.findViewById<TextView>(R.id.user_bio)
        val editButton = profileView.findViewById<Button>(R.id.edit_profile_button)

        // Assert
        assertNotNull(userName, "User name should exist")
        assertNotNull(userBio, "User bio should exist")
        assertNotNull(editButton, "Edit button should exist")
        
        // Verify margin values match design system tokens
        val userNameParams = userName.layoutParams as android.view.ViewGroup.MarginLayoutParams
        val userBioParams = userBio.layoutParams as android.view.ViewGroup.MarginLayoutParams
        val editButtonParams = editButton.layoutParams as android.view.ViewGroup.MarginLayoutParams
        
        val expectedMediumMargin = context.resources.getDimensionPixelSize(R.dimen.ds_margin_medium)
        val expectedStandardMargin = context.resources.getDimensionPixelSize(R.dimen.ds_margin_standard)
        val expectedXXLMargin = context.resources.getDimensionPixelSize(R.dimen.ds_margin_xxl)
        
        assertEquals(expectedMediumMargin, userNameParams.topMargin, "User name should use ds_margin_medium")
        assertEquals(expectedMediumMargin, userBioParams.topMargin, "User bio should use ds_margin_medium")
        assertEquals(expectedStandardMargin, editButtonParams.topMargin, "Edit button should use ds_margin_standard")
        assertEquals(expectedXXLMargin, userBioParams.leftMargin, "User bio should use ds_margin_xxl for horizontal margins")
    }

    @Test
    fun `profile buttons use consistent spacing`() {
        // Arrange & Act
        val profileView = inflater.inflate(R.layout.fragment_profile, null)
        val prescriptionsButton = profileView.findViewById<Button>(R.id.prescriptions_button)
        val logoutButton = profileView.findViewById<Button>(R.id.logoutButton)

        // Assert
        assertNotNull(prescriptionsButton, "Prescriptions button should exist")
        assertNotNull(logoutButton, "Logout button should exist")
        
        // Verify margin values match design system tokens
        val prescriptionsButtonParams = prescriptionsButton.layoutParams as android.view.ViewGroup.MarginLayoutParams
        val logoutButtonParams = logoutButton.layoutParams as android.view.ViewGroup.MarginLayoutParams
        
        val expectedStandardMargin = context.resources.getDimensionPixelSize(R.dimen.ds_margin_standard)
        val expectedXXLMargin = context.resources.getDimensionPixelSize(R.dimen.ds_margin_xxl)
        val expectedXLMargin = context.resources.getDimensionPixelSize(R.dimen.ds_margin_xl)
        
        assertEquals(expectedStandardMargin, prescriptionsButtonParams.leftMargin, "Prescriptions button should use ds_margin_standard")
        assertEquals(expectedXXLMargin, prescriptionsButtonParams.topMargin, "Prescriptions button should use ds_margin_xxl")
        assertEquals(expectedXXLMargin, logoutButtonParams.topMargin, "Logout button should use ds_margin_xxl")
        assertEquals(expectedXLMargin, logoutButtonParams.bottomMargin, "Logout button should use ds_margin_xl")
    }

    @Test
    fun `profile buttons preserve accessibility attributes`() {
        // Arrange & Act
        val profileView = inflater.inflate(R.layout.fragment_profile, null)
        val editButton = profileView.findViewById<Button>(R.id.edit_profile_button)
        val prescriptionsButton = profileView.findViewById<Button>(R.id.prescriptions_button)
        val logoutButton = profileView.findViewById<Button>(R.id.logoutButton)

        // Assert
        assertNotNull(editButton, "Edit button should exist")
        assertNotNull(prescriptionsButton, "Prescriptions button should exist")
        assertNotNull(logoutButton, "Logout button should exist")
        
        // Verify buttons are clickable and focusable for accessibility
        assertTrue(editButton.isClickable, "Edit button should be clickable")
        assertTrue(editButton.isFocusable, "Edit button should be focusable")
        assertTrue(prescriptionsButton.isClickable, "Prescriptions button should be clickable")
        assertTrue(prescriptionsButton.isFocusable, "Prescriptions button should be focusable")
        assertTrue(logoutButton.isClickable, "Logout button should be clickable")
        assertTrue(logoutButton.isFocusable, "Logout button should be focusable")
        
        // Verify button text is set
        assertEquals("Edit Profile", editButton.text.toString())
        assertEquals("Prescriptions", prescriptionsButton.text.toString())
        assertEquals("Logout", logoutButton.text.toString())
    }
}