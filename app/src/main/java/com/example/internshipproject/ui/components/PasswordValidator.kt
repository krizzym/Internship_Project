package com.example.internshipproject.ui.components

/**
 * Utility object for validating password strength and requirements
 */
object PasswordValidator {

    private const val MIN_LENGTH = 12
    private val UPPERCASE_REGEX = Regex("[A-Z]")
    private val LOWERCASE_REGEX = Regex("[a-z]")
    private val DIGIT_REGEX = Regex("\\d")
    private val SPECIAL_CHAR_REGEX = Regex("[!@#\$%^&*(),.?\":{}|<>]")

    /**
     * Validates password against all requirements
     * @return ValidationResult containing success status and error message if failed
     */
    fun validate(password: String): ValidationResult {
        return when {
            password.length < MIN_LENGTH ->
                ValidationResult(false, "Password must be at least $MIN_LENGTH characters")

            !UPPERCASE_REGEX.containsMatchIn(password) ->
                ValidationResult(false, "Password must contain at least 1 uppercase letter")

            !LOWERCASE_REGEX.containsMatchIn(password) ->
                ValidationResult(false, "Password must contain at least 1 lowercase letter")

            !DIGIT_REGEX.containsMatchIn(password) ->
                ValidationResult(false, "Password must contain at least 1 number")

            !SPECIAL_CHAR_REGEX.containsMatchIn(password) ->
                ValidationResult(false, "Password must contain at least 1 special character (!@#\$%^&*...)")

            else -> ValidationResult(true, null)
        }
    }

    /**
     * Gets a list of all password requirements with their current status
     * @return List of PasswordRequirement objects showing which requirements are met
     */


    fun getRequirements(password: String): List<PasswordRequirement> {
        return listOf(
            PasswordRequirement(
                "At least $MIN_LENGTH characters",
                password.length >= MIN_LENGTH
            ),
            PasswordRequirement(
                "At least 1 uppercase letter (A-Z)",
                UPPERCASE_REGEX.containsMatchIn(password)
            ),
            PasswordRequirement(
                "At least 1 lowercase letter (a-z)",
                LOWERCASE_REGEX.containsMatchIn(password)
            ),
            PasswordRequirement(
                "At least 1 number (0-9)",
                DIGIT_REGEX.containsMatchIn(password)
            ),
            PasswordRequirement(
                "At least 1 special character (!@#\$%^&*...)",
                SPECIAL_CHAR_REGEX.containsMatchIn(password)
            )
        )
    }

    /**
     * Checks if all password requirements are met
     */
    fun isValid(password: String): Boolean {
        return validate(password).isValid
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String?
    )

    data class PasswordRequirement(
        val description: String,
        val isMet: Boolean
    )
}
