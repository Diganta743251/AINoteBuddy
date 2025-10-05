package com.ainotebuddy.app.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms of Service") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Terms of Service",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Last updated: [DATE]",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TermsSection(
                title = "1. Acceptance of Terms",
                content = "By downloading, installing, or using AI NoteBuddy, you agree to be bound by these Terms of Service."
            )
            
            TermsSection(
                title = "2. Description of Service",
                content = "AI NoteBuddy is a note-taking application that uses artificial intelligence to enhance your productivity and organization."
            )
            
            TermsSection(
                title = "3. User Accounts",
                content = "You may need to create an account to access certain features. You are responsible for maintaining the confidentiality of your account credentials."
            )
            
            TermsSection(
                title = "4. Privacy and Data",
                content = "Your privacy is important to us. Please review our Privacy Policy to understand how we collect, use, and protect your information."
            )
            
            TermsSection(
                title = "5. Acceptable Use",
                content = "You agree not to use the service for any unlawful purposes or in any way that could damage, disable, or impair the service."
            )
            
            TermsSection(
                title = "6. Intellectual Property",
                content = "The service and its original content are and will remain the exclusive property of AI NoteBuddy and its licensors."
            )
            
            TermsSection(
                title = "7. Termination",
                content = "We may terminate or suspend your account and access to the service at our sole discretion, without prior notice."
            )
            
            TermsSection(
                title = "8. Disclaimer",
                content = "The service is provided on an \"AS IS\" and \"AS AVAILABLE\" basis without warranties of any kind."
            )
            
            TermsSection(
                title = "9. Limitation of Liability",
                content = "In no event shall AI NoteBuddy be liable for any indirect, incidental, special, consequential, or punitive damages."
            )
            
            TermsSection(
                title = "10. Changes to Terms",
                content = "We reserve the right to modify these terms at any time. Continued use of the service constitutes acceptance of the modified terms."
            )
            
            TermsSection(
                title = "11. Contact Information",
                content = "If you have any questions about these Terms of Service, please contact us at [EMAIL]."
            )
        }
    }
}

@Composable
private fun TermsSection(
    title: String,
    content: String
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}