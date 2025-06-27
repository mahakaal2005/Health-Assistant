package com.example.health_assistant.features.profile.validation

import com.example.health_assistant.features.profile.state.ProfileField
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for ProfileValidationRules
 * Tests all validation scenarios to ensure robust input validation
 */
@RunWith(JUnit4::class)
class ProfileValidationRulesTest {

    @Test
    fun `validateDisplayName should return valid for proper names`() {
        // Valid display names
        val validNames = listOf(
            "John Doe",
            "Alice_Smith",
            "Bob-Johnson",
            "Maria123",
            "A B",
            "Test User"
        )

        validNames.forEach { name ->
            val result = ProfileValidationRules.validateDisplayName(name)
            assertTrue("'$name' should be valid", result.isValid)
            assertNull("No error message for valid name", result.errorMessage)
        }
    }

    @Test
    fun `validateDisplayName should return invalid for empty or blank names`() {
        val invalidNames = listOf("", "   ", "\t", "\n")

        invalidNames.forEach { name ->
            val result = ProfileValidationRules.validateDisplayName(name)
            assertFalse("'$name' should be invalid", result.isValid)
            assertEquals("Should return required error", "Display name is required", result.errorMessage)
            assertEquals("Should return correct field", ProfileField.DISPLAY_NAME, result.field)
        }
    }

    @Test
    fun `validateDisplayName should return invalid for too short names`() {
        val shortName = "A"
        val result = ProfileValidationRules.validateDisplayName(shortName)

        assertFalse("Single character should be invalid", result.isValid)
        assertEquals("Should return length error", "Display name must be at least 2 characters", result.errorMessage)
    }

    @Test
    fun `validateDisplayName should return invalid for too long names`() {
        val longName = "A".repeat(51)
        val result = ProfileValidationRules.validateDisplayName(longName)

        assertFalse("Too long name should be invalid", result.isValid)
        assertEquals("Should return length error", "Display name cannot exceed 50 characters", result.errorMessage)
    }

    @Test
    fun `validateDisplayName should return invalid for names with invalid characters`() {
        val invalidNames = listOf(
            "John@Doe",
            "Alice#Smith",
            "Bob$Johnson",
            "Test%User",
            "Name&User"
        )

        invalidNames.forEach { name ->
            val result = ProfileValidationRules.validateDisplayName(name)
            assertFalse("'$name' should be invalid", result.isValid)
            assertEquals("Should return character error", "Display name contains invalid characters", result.errorMessage)
        }
    }

    @Test
    fun `validateDisplayName should return invalid for names with leading or trailing spaces`() {
        val invalidNames = listOf(" John Doe", "John Doe ", " John Doe ")

        invalidNames.forEach { name ->
            val result = ProfileValidationRules.validateDisplayName(name)
            assertFalse("'$name' should be invalid", result.isValid)
            assertEquals("Should return space error", "Display name cannot start or end with spaces", result.errorMessage)
        }
    }

    @Test
    fun `validateBirthday should return valid for proper dates`() {
        val validDates = listOf(
            "1990-05-15",
            "1985-12-25",
            "2000-01-01",
            "1995-07-04"
        )

        validDates.forEach { date ->
            val result = ProfileValidationRules.validateBirthday(date)
            assertTrue("'$date' should be valid", result.isValid)
            assertNull("No error message for valid date", result.errorMessage)
        }
    }

    @Test
    fun `validateBirthday should return invalid for future dates`() {
        val futureDate = "2030-01-01"
        val result = ProfileValidationRules.validateBirthday(futureDate)

        assertFalse("Future date should be invalid", result.isValid)
        assertEquals("Should return future date error", "Birthday cannot be in the future", result.errorMessage)
    }

    @Test
    fun `validateBirthday should return invalid for unreasonable ages`() {
        val tooYoung = "2020-01-01" // Too young
        val tooOld = "1900-01-01"   // Too old

        val youngResult = ProfileValidationRules.validateBirthday(tooYoung)
        assertFalse("Too young should be invalid", youngResult.isValid)
        assertEquals("Should return age error", "Age must be at least 5 years", youngResult.errorMessage)

        val oldResult = ProfileValidationRules.validateBirthday(tooOld)
        assertFalse("Too old should be invalid", oldResult.isValid)
        assertEquals("Should return age error", "Please enter a valid birth date", oldResult.errorMessage)
    }

    @Test
    fun `validateBirthday should return invalid for wrong format`() {
        val invalidFormats = listOf(
            "15-05-1990",
            "05/15/1990",
            "1990.05.15",
            "May 15, 1990",
            "1990-5-15",
            "90-05-15"
        )

        invalidFormats.forEach { date ->
            val result = ProfileValidationRules.validateBirthday(date)
            assertFalse("'$date' should be invalid format", result.isValid)
            assertTrue("Should contain format error", result.errorMessage?.contains("format") == true)
        }
    }

    @Test
    fun `validateGender should return valid for accepted options`() {
        val validGenders = listOf("Male", "Female", "Other", "Prefer not to say")

        validGenders.forEach { gender ->
            val result = ProfileValidationRules.validateGender(gender)
            assertTrue("'$gender' should be valid", result.isValid)
            assertNull("No error message for valid gender", result.errorMessage)
        }
    }

    @Test
    fun `validateGender should return invalid for unaccepted options`() {
        val invalidGenders = listOf("Unknown", "Custom", "", "male", "MALE")

        invalidGenders.forEach { gender ->
            val result = ProfileValidationRules.validateGender(gender)
            assertFalse("'$gender' should be invalid", result.isValid)
            assertEquals("Should return selection error", "Please select a valid gender option", result.errorMessage)
        }
    }

    @Test
    fun `validateHeight should return valid for reasonable heights`() {
        val validHeights = listOf("150", "175.5", "200", "50", "300")

        validHeights.forEach { height ->
            val result = ProfileValidationRules.validateHeight(height)
            assertTrue("'$height' should be valid", result.isValid)
            assertNull("No error message for valid height", result.errorMessage)
        }
    }

    @Test
    fun `validateHeight should return invalid for unreasonable heights`() {
        val invalidHeights = mapOf(
            "49" to "Height must be at least 50 cm",
            "301" to "Height cannot exceed 300 cm",
            "0" to "Height must be at least 50 cm",
            "-50" to "Height must be at least 50 cm"
        )

        invalidHeights.forEach { (height, expectedError) ->
            val result = ProfileValidationRules.validateHeight(height)
            assertFalse("'$height' should be invalid", result.isValid)
            assertEquals("Should return correct error for $height", expectedError, result.errorMessage)
        }
    }

    @Test
    fun `validateHeight should return invalid for non-numeric values`() {
        val nonNumericHeights = listOf("abc", "1.2.3", "tall", "", "null")

        nonNumericHeights.forEach { height ->
            val result = ProfileValidationRules.validateHeight(height)
            assertFalse("'$height' should be invalid", result.isValid)
            assertTrue("Should contain number error", result.errorMessage?.contains("number") == true)
        }
    }

    @Test
    fun `validateWeight should return valid for reasonable weights`() {
        val validWeights = listOf("70", "85.5", "120", "20", "500")

        validWeights.forEach { weight ->
            val result = ProfileValidationRules.validateWeight(weight)
            assertTrue("'$weight' should be valid", result.isValid)
            assertNull("No error message for valid weight", result.errorMessage)
        }
    }

    @Test
    fun `validateWeight should return invalid for unreasonable weights`() {
        val invalidWeights = mapOf(
            "19" to "Weight must be at least 20 kg",
            "501" to "Weight cannot exceed 500 kg",
            "0" to "Weight must be at least 20 kg",
            "-10" to "Weight must be at least 20 kg"
        )

        invalidWeights.forEach { (weight, expectedError) ->
            val result = ProfileValidationRules.validateWeight(weight)
            assertFalse("'$weight' should be invalid", result.isValid)
            assertEquals("Should return correct error for $weight", expectedError, result.errorMessage)
        }
    }

    @Test
    fun `validateAllFields should return all errors for invalid input`() {
        val errors = ProfileValidationRules.validateAllFields(
            displayName = "",
            birthday = "invalid-date",
            gender = "invalid-gender",
            height = "1000",
            weight = "10"
        )

        assertEquals("Should return 5 errors", 5, errors.size)
        assertTrue("Should contain display name error", errors.containsKey(ProfileField.DISPLAY_NAME))
        assertTrue("Should contain birthday error", errors.containsKey(ProfileField.BIRTHDAY))
        assertTrue("Should contain gender error", errors.containsKey(ProfileField.GENDER))
        assertTrue("Should contain height error", errors.containsKey(ProfileField.HEIGHT))
        assertTrue("Should contain weight error", errors.containsKey(ProfileField.WEIGHT))
    }

    @Test
    fun `validateAllFields should return no errors for valid input`() {
        val errors = ProfileValidationRules.validateAllFields(
            displayName = "John Doe",
            birthday = "1990-05-15",
            gender = "Male",
            height = "175",
            weight = "70"
        )

        assertTrue("Should return no errors", errors.isEmpty())
    }

    @Test
    fun `isFormComplete should return true for complete valid form`() {
        val isComplete = ProfileValidationRules.isFormComplete(
            displayName = "John Doe",
            birthday = "1990-05-15",
            gender = "Male",
            height = "175",
            weight = "70"
        )

        assertTrue("Form should be complete", isComplete)
    }

    @Test
    fun `isFormComplete should return true for form with only required fields`() {
        val isComplete = ProfileValidationRules.isFormComplete(
            displayName = "John Doe",
            birthday = null,
            gender = null,
            height = null,
            weight = null
        )

        assertTrue("Form should be complete with only display name", isComplete)
    }

    @Test
    fun `isFormComplete should return false for form with invalid required field`() {
        val isComplete = ProfileValidationRules.isFormComplete(
            displayName = "",
            birthday = "1990-05-15",
            gender = "Male",
            height = "175",
            weight = "70"
        )

        assertFalse("Form should not be complete without valid display name", isComplete)
    }

    @Test
    fun `getValidationSummary should return appropriate messages`() {
        val noErrors = ProfileValidationRules.getValidationSummary(emptyMap())
        assertEquals("Should return valid message", "All fields are valid", noErrors)

        val oneError = ProfileValidationRules.getValidationSummary(
            mapOf(ProfileField.DISPLAY_NAME to "Display name is required")
        )
        assertEquals("Should return single error message", "Please fix the display_name field", oneError)

        val multipleErrors = ProfileValidationRules.getValidationSummary(
            mapOf(
                ProfileField.DISPLAY_NAME to "Display name is required",
                ProfileField.HEIGHT to "Invalid height"
            )
        )
        assertEquals("Should return multiple errors message", "Please fix 2 fields with errors", multipleErrors)
    }
}