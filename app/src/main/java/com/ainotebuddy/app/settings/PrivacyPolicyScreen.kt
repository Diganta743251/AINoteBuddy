package com.ainotebuddy.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.ainotebuddy.app.ui.theme.AnimatedGradientBackground
import com.ainotebuddy.app.ui.theme.GlassCard
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Web
import androidx.compose.ui.unit.sp

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Animated gradient background
        AnimatedGradientBackground()
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                PolicyHeader(
                    title = "Privacy Policy",
                    subtitle = "How we protect your data",
                    onBack = onBack
                )
            }
            
            // Privacy Policy Content
            item {
                PrivacyPolicyContent()
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Animated gradient background
        AnimatedGradientBackground()
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                PolicyHeader(
                    title = "About AINoteBuddy",
                    subtitle = "Your AI-powered note companion",
                    onBack = onBack
                )
            }
            
            // App Info
            item {
                AppInfoCard()
            }
            
            // Features
            item {
                FeaturesCard()
            }
            
            // Team
            item {
                TeamCard()
            }
            
            // Contact
            item {
                ContactCard()
            }
        }
    }
}

@Composable
fun PolicyHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
                
                // Empty space for balance
                Spacer(modifier = Modifier.size(40.dp))
            }
        }
    }
}

@Composable
fun PrivacyPolicyContent() {
    GlassCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PolicySection(
                title = "Data Collection",
                content = "We collect minimal data necessary for app functionality. Your notes are stored locally on your device and optionally synced to your Google Drive account."
            )
            
            PolicySection(
                title = "Data Usage",
                content = "Your data is used solely to provide note-taking functionality. We do not sell, trade, or share your personal information with third parties."
            )
            
            PolicySection(
                title = "Data Security",
                content = "We implement industry-standard security measures to protect your data. Notes in the vault are encrypted and require biometric authentication."
            )
            
            PolicySection(
                title = "Third-Party Services",
                content = "We use Google Drive for cloud sync and Google ML Kit for text recognition. These services have their own privacy policies."
            )
            
            PolicySection(
                title = "Your Rights",
                content = "You have the right to access, modify, or delete your data at any time. You can export your notes or delete your account entirely."
            )
            
            PolicySection(
                title = "Contact Us",
                content = "If you have questions about this privacy policy, please contact us at privacy@ainotebuddy.com"
            )
        }
    }
}

@Composable
fun AppInfoCard() {
    GlassCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AINoteBuddy",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Version 2.0.0",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your AI-powered note-taking companion that helps you capture, organize, and enhance your thoughts with advanced features and beautiful design.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FeaturesCard() {
    GlassCard {
        Column {
            Text(
                text = "Key Features",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    FeatureItem(
                        icon = Icons.Filled.Info,
                        title = "Rich Text Editing",
                        description = "Create beautiful notes with formatting options"
                    )
                }
                item {
                    FeatureItem(
                        icon = Icons.Filled.Mic,
                        title = "Voice Notes",
                        description = "Convert speech to text with AI"
                    )
                }
                item {
                    FeatureItem(
                        icon = Icons.Filled.CameraAlt,
                        title = "Image Recognition",
                        description = "Extract text from images using OCR"
                    )
                }
                item {
                    FeatureItem(
                        icon = Icons.Filled.Lock,
                        title = "Secure Vault",
                        description = "Protect sensitive notes with biometric lock"
                    )
                }
                item {
                    FeatureItem(
                        icon = Icons.Filled.Cloud,
                        title = "Cloud Sync",
                        description = "Sync notes across devices with Google Drive"
                    )
                }
                item {
                    FeatureItem(
                        icon = Icons.Filled.Palette,
                        title = "Multiple Themes",
                        description = "Choose from light, dark, Material You, and futuristic themes"
                    )
                }
            }
        }
    }
}

@Composable
fun TeamCard() {
    GlassCard {
        Column {
            Text(
                text = "Development Team",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TeamMember(
                        name = "AINoteBuddy Team",
                        role = "Development & Design",
                        description = "Passionate developers creating the best note-taking experience"
                    )
                }
                item {
                    TeamMember(
                        name = "AI Integration",
                        role = "Machine Learning",
                        description = "Advanced AI features for intelligent note processing"
                    )
                }
                item {
                    TeamMember(
                        name = "User Experience",
                        role = "UX/UI Design",
                        description = "Beautiful and intuitive interface design"
                    )
                }
            }
        }
    }
}

@Composable
fun ContactCard() {
    GlassCard {
        Column {
            Text(
                text = "Get in Touch",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ContactItem(
                        icon = Icons.Filled.Email,
                        title = "Support",
                        value = "support@ainotebuddy.com"
                    )
                }
                item {
                    ContactItem(
                        icon = Icons.Filled.BugReport,
                        title = "Bug Reports",
                        value = "bugs@ainotebuddy.com"
                    )
                }
                item {
                    ContactItem(
                        icon = Icons.Filled.PrivacyTip,
                        title = "Privacy",
                        value = "privacy@ainotebuddy.com"
                    )
                }
                item {
                    ContactItem(
                        icon = Icons.Filled.Web,
                        title = "Website",
                        value = "www.ainotebuddy.com"
                    )
                }
            }
        }
    }
}

@Composable
fun PolicySection(
    title: String,
    content: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            lineHeight = 20.sp
        )
    }
}

@Composable
fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF6A82FB),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun TeamMember(
    name: String,
    role: String,
    description: String
) {
    Column {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        Text(
            text = role,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6A82FB),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ContactItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF6A82FB),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
} 