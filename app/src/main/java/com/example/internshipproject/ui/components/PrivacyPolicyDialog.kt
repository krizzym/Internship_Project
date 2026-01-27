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
import com.example.internshipproject.ui.theme.PurpleButton

@Composable
fun PrivacyPolicyDialog(
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
                    text = "Privacy Policy",
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
FirstStep Privacy Policy

Effective Date: January 23, 2026

1. INFORMATION WE COLLECT
We collect information you provide directly to us:
- Personal Information: Name, email address, phone number, educational background
- Account Credentials: Username and encrypted password
- Professional Information: Resume, skills, work experience
- Company Information: Company name, address, industry type, logo

2. HOW WE USE YOUR INFORMATION
We use the information we collect to:
- Provide, maintain, and improve our services
- Process and facilitate internship applications
- Send you technical notices and support messages
- Respond to your comments and questions
- Monitor and analyze trends, usage, and activities

3. INFORMATION SHARING
We do not sell your personal information. We may share your information:
- With companies when you apply for their internships
- With service providers who assist in our operations
- To comply with legal obligations
- With your consent or at your direction

4. DATA SECURITY
We implement appropriate technical and organizational measures to protect your personal information against unauthorized access, alteration, disclosure, or destruction.

5. YOUR RIGHTS
You have the right to:
- Access your personal information
- Correct inaccurate information
- Delete your account and data
- Object to processing of your information
- Withdraw consent at any time

6. COOKIES AND TRACKING
We use cookies and similar tracking technologies to track activity on our platform and hold certain information to improve user experience.

7. DATA RETENTION
We retain your information for as long as your account is active or as needed to provide you services. You may request deletion of your data at any time.

8. CHILDREN'S PRIVACY
Our services are not directed to children under 18. We do not knowingly collect personal information from children under 18.

9. CHANGES TO THIS POLICY
We may update this Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page.

10. CONTACT US
If you have questions about this Privacy Policy, please contact us at:
Email: privacy@firststep.com

By using FirstStep, you consent to the collection and use of information as described in this Privacy Policy.
                        """.trimIndent(),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleButton)
                ) {
                    Text("Close", fontSize = 16.sp)
                }
            }
        }
    }
}