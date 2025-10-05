package com.ainotebuddy.app.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ainotebuddy.app.billing.BillingManager
import com.ainotebuddy.app.billing.PurchaseState
import com.android.billingclient.api.ProductDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val billingManager = remember { BillingManager.getInstance(context) }
    
    val purchaseState by billingManager.purchaseState.collectAsStateWithLifecycle()
    val availableProducts by billingManager.availableProducts.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        billingManager.startConnection()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium Features") },
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
                .verticalScroll(rememberScrollState())
        ) {
            // Premium Header
            PremiumHeader(purchaseState)
            
            // Features List
            PremiumFeaturesList()
            
            // Pricing Cards
            if (purchaseState !is PurchaseState.MonthlySubscribed && 
                purchaseState !is PurchaseState.LifetimePurchased) {
                PricingSection(
                    availableProducts = availableProducts,
                    onPurchaseClick = { productDetails ->
                        if (context is androidx.activity.ComponentActivity) {
                            billingManager.launchBillingFlow(context, productDetails)
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PremiumHeader(purchaseState: PurchaseState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = when (purchaseState) {
                    is PurchaseState.MonthlySubscribed -> "Premium Active"
                    is PurchaseState.LifetimePurchased -> "Lifetime Premium"
                    else -> "Upgrade to Premium"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when (purchaseState) {
                    is PurchaseState.MonthlySubscribed -> "Enjoy all premium features"
                    is PurchaseState.LifetimePurchased -> "Thank you for your support!"
                    else -> "Remove ads and unlock advanced features"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PremiumFeaturesList() {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Premium Features",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val features = listOf(
            PremiumFeature(
                icon = Icons.Default.Block,
                title = "Ad-Free Experience",
                description = "Remove all advertisements for distraction-free note-taking"
            ),
            PremiumFeature(
                icon = Icons.Default.CloudSync,
                title = "Unlimited Cloud Sync",
                description = "Sync unlimited notes across all your devices"
            ),
            PremiumFeature(
                icon = Icons.Default.Psychology,
                title = "Advanced AI Features",
                description = "Access to premium AI models and unlimited AI processing"
            ),
            PremiumFeature(
                icon = Icons.Default.Security,
                title = "Enhanced Security",
                description = "Advanced encryption and secure vault features"
            ),
            PremiumFeature(
                icon = Icons.Default.Palette,
                title = "Custom Themes",
                description = "Personalize your app with exclusive themes and colors"
            ),
            PremiumFeature(
                icon = Icons.Default.Support,
                title = "Priority Support",
                description = "Get faster response times for support requests"
            )
        )
        
        features.forEach { feature ->
            FeatureItem(feature = feature)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FeatureItem(feature: PremiumFeature) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = feature.icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PricingSection(
    availableProducts: List<ProductDetails>,
    onPurchaseClick: (ProductDetails) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Choose Your Plan",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val monthlyProduct = availableProducts.find { 
            it.productId == BillingManager.MONTHLY_SUBSCRIPTION 
        }
        val lifetimeProduct = availableProducts.find { 
            it.productId == BillingManager.LIFETIME_PURCHASE 
        }
        
        // Monthly Subscription Card
        monthlyProduct?.let { product ->
            PricingCard(
                title = "Monthly",
                price = "₹49",
                period = "/month",
                features = listOf("All premium features", "Cancel anytime"),
                isPopular = false,
                onPurchaseClick = { onPurchaseClick(product) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Lifetime Purchase Card
        lifetimeProduct?.let { product ->
            PricingCard(
                title = "Lifetime",
                price = "₹499",
                period = "one-time",
                features = listOf("All premium features", "One-time payment", "Best value"),
                isPopular = true,
                onPurchaseClick = { onPurchaseClick(product) }
            )
        }
    }
}

@Composable
private fun PricingCard(
    title: String,
    price: String,
    period: String,
    features: List<String>,
    isPopular: Boolean,
    onPurchaseClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPopular) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            if (isPopular) {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "BEST VALUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                Text(
                    text = period,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onPurchaseClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPopular) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(
                    text = "Get $title",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private data class PremiumFeature(
    val icon: ImageVector,
    val title: String,
    val description: String
)