package com.example.internshipproject.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    helperText: String? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = isError,
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            supportingText = {
                if (isError && errorMessage != null) {
                    Text(errorMessage, color = Color.Red, fontSize = 12.sp)
                } else if (helperText != null) {
                    Text(helperText, fontSize = 12.sp)
                }
            }
        )
    }
}
object PasswordValidator {
    private const val MIN_LENGTH = 8
    private val SPECIAL_CHARACTERS = "!@#\$%^&*()_+-=[]{}|;:,.<>?"

    fun validatePassword(password: String): Pair<Boolean, String> {
        if (password.length < MIN_LENGTH) {
            return Pair(false, "Password must be at least $MIN_LENGTH characters")
        }
        if (!password.any { it.isUpperCase() }) {
            return Pair(false, "Must contain uppercase letter")
        }
        if (!password.any { it.isLowerCase() }) {
            return Pair(false, "Must contain lowercase letter")
        }
        if (!password.any { SPECIAL_CHARACTERS.contains(it) }) {
            return Pair(false, "Must contain special character (!@#$%^&*)")
        }
        return Pair(true, "")
    }
}
