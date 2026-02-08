package com.example.internshipproject.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.internshipproject.ui.theme.PrimaryDeepBlueButton

@Composable
fun TermsAndConditionsDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Terms and Conditions",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = """
Welcome to FirstStep Internship Connection Platform

1. ACCEPTANCE OF TERMS
By accessing and using this platform, you accept and agree to be bound by the terms and provisions of this agreement.

2. USER REGISTRATION
- Users must provide accurate and complete information during registration
- Users are responsible for maintaining the confidentiality of their account
- Users must be 18 years or older to register

3. STUDENT USERS
- Students must provide truthful information about their education and qualifications
- Students agree to upload genuine documents (resume, transcripts)
- Students must conduct themselves professionally when applying for internships

4. COMPANY USERS
- Companies must be legally registered entities
- Companies agree to post legitimate internship opportunities
- Companies must comply with labor laws and fair employment practices

5. INTERNSHIP APPLICATIONS
- All applications must be submitted in good faith
- Students agree to honor internship commitments if selected
- Companies agree to review applications fairly and respond in a timely manner

6. PRIVACY AND DATA PROTECTION
- We collect and store user information as described in our Privacy Policy
- User data will be protected and not shared without consent
- Users have the right to access, modify, or delete their data

7. PROHIBITED CONDUCT
Users must NOT:
- Post false or misleading information
- Harass, abuse, or harm other users
- Use the platform for illegal activities
- Attempt to hack or compromise the system
- Share account credentials with others

8. INTELLECTUAL PROPERTY
- All content on this platform is owned by FirstStep
- Users retain rights to their uploaded content
- Users grant FirstStep license to use uploaded content for platform purposes

9. TERMINATION
- We reserve the right to suspend or terminate accounts for violations
- Users may delete their accounts at any time
- Upon termination, user data will be handled per our Privacy Policy

10. LIABILITY
- FirstStep is not responsible for the quality of internships posted
- Users interact with each other at their own risk
- We are not liable for disputes between students and companies

11. CHANGES TO TERMS
- We reserve the right to modify these terms at any time
- Continued use of the platform constitutes acceptance of new terms
- Users will be notified of significant changes

12. CONTACT
For questions about these terms, contact us at:
Email: support@firststep.com

Last Updated: January 23, 2026

By using this platform, you acknowledge that you have read, understood, and agree to be bound by these Terms and Conditions.
                        """.trimIndent(),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeepBlueButton)
                ) {
                    Text("Close", fontSize = 16.sp)
                }
            }
        }
    }
}