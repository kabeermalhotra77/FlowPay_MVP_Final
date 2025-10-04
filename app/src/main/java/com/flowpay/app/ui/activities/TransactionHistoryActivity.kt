package com.flowpay.app.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowpay.app.data.PaymentDetails
import com.flowpay.app.data.PaymentStatus
import com.flowpay.app.data.Transaction
import com.flowpay.app.ui.theme.FlowPayTheme
import com.flowpay.app.ui.components.TransactionDetailDialog
import com.flowpay.app.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Data classes
data class TransactionStats(
    val totalAmount: Double,
    val successRate: Float,
    val totalCount: Int,
    val todayCount: Int
)

// Utility functions
fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return formatter.format(amount)
}

fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd MMM, HH:mm", Locale("en", "IN"))
    return formatter.format(Date(timestamp))
}

fun getStatusColor(status: String): Color {
    return when (status.uppercase()) {
        "SUCCESS", "SUCCESSFUL", "COMPLETED" -> Color(0xFF4CAF50)
        "PENDING" -> Color(0xFFFF9800)
        "FAILED", "DECLINED", "CANCELLED" -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }
}

class TransactionHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            FlowPayTheme {
                TransactionHistoryScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val transactionViewModel: TransactionViewModel = viewModel()
    
    // State
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showAdvancedFilters by remember { mutableStateOf(false) }
    
    // Collect data
    val allTransactions by transactionViewModel.loadAllTransactions().collectAsState(initial = emptyList())
    val isLoading by transactionViewModel.isLoading.collectAsState()
    val error by transactionViewModel.error.collectAsState()
    
    // Filter transactions
    val filteredTransactions = remember(allTransactions, searchQuery, selectedFilter) {
        allTransactions.filter { transaction ->
            val matchesSearch = searchQuery.isEmpty() || 
                transaction.recipientName?.contains(searchQuery, ignoreCase = true) == true ||
                transaction.phoneNumber?.contains(searchQuery, ignoreCase = true) == true ||
                transaction.bankName.contains(searchQuery, ignoreCase = true) ||
                transaction.amount.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = when (selectedFilter) {
                "All" -> true
                "Success" -> transaction.status.uppercase() in listOf("SUCCESS", "SUCCESSFUL", "COMPLETED")
                "Failed" -> transaction.status.uppercase() in listOf("FAILED", "DECLINED", "CANCELLED")
                "Pending" -> transaction.status.uppercase() == "PENDING"
                else -> true
            }
            
            matchesSearch && matchesFilter
        }
    }
    
    // Calculate transaction statistics for enhanced header
    val transactionStats = remember(filteredTransactions) {
        val totalAmount = filteredTransactions.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val successCount = filteredTransactions.count { it.status.uppercase() in listOf("SUCCESS", "SUCCESSFUL", "COMPLETED") }
        val successRate = if (filteredTransactions.isNotEmpty()) (successCount.toFloat() / filteredTransactions.size) * 100 else 0f
        val todayCount = filteredTransactions.count { 
            val today = System.currentTimeMillis()
            val transactionDate = it.timestamp
            (today - transactionDate) < 24 * 60 * 60 * 1000 // Within 24 hours
        }
        
        TransactionStats(
            totalAmount = totalAmount,
            successRate = successRate,
            totalCount = filteredTransactions.size,
            todayCount = todayCount
        )
    }
    
    // Theme colors - Updated to match main screen exactly
    val backgroundColor = Color.Black
    val primaryColor = Color(0xFF5B8DEF) // Main blue color
    val primaryForegroundColor = Color.White
    val cardColor = Color(0xFF1F1F1F) // For other cards in dark theme
    val borderColor = Color(0xFF404040)
    val mutedForegroundColor = Color(0xFF888888)
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp)
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 420.dp)
                    .align(Alignment.Center)
                    .background(Color.Black)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Add spacing from status bar
                Spacer(modifier = Modifier.height(12.dp))
                
                // Clean Gradient Header Card - Minimal, focused design
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .height(160.dp) // More compact, focused design
                        .shadow(
                            elevation = 6.dp, // Match main screen elevation
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.10f),
                            spotColor = Color.Black.copy(alpha = 0.10f)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF7BA8F5), // Lighter, softer blue (top)
                                        Color(0xFF6A96EE)  // Lighter blue with slight depth (bottom)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp) // Match main screen padding for better breathing room
                        ) {
                            // Top Section with Navigation and Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                // Left side - Back Button and Title
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Enhanced Back Button with better visual feedback
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp) // Slightly larger for better touch target
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.25f)) // Increased opacity
                                            .clickable { onBackClick() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Back",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp) // Slightly larger icon
                                        )
                                    }
                                    
                                    // Enhanced Title with main screen styling
                                    Column {
                                        Text(
                                            text = "Transaction History",
                                            fontSize = 24.sp, // Match main screen title size
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            letterSpacing = 0.5.sp, // Match main screen letter spacing
                                            style = androidx.compose.ui.text.TextStyle(
                                                shadow = Shadow(
                                                    color = Color.Black.copy(alpha = 0.15f),
                                                    offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                                                    blurRadius = 6f
                                                )
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(6.dp)) // Match main screen spacing
                                        Text(
                                            text = "View your payment history",
                                            fontSize = 15.sp, // Match main screen subtitle size
                                            fontWeight = FontWeight.Normal,
                                            color = Color.White.copy(alpha = 0.95f), // Match main screen opacity
                                            letterSpacing = 0.2.sp,
                                            style = androidx.compose.ui.text.TextStyle(
                                                shadow = Shadow(
                                                    color = Color.Black.copy(alpha = 0.1f),
                                                    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                                                    blurRadius = 3f
                                                )
                                            )
                                        )
                                    }
                                }
                                
                                // Right side - Enhanced Action Buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Enhanced Search Button with better visual feedback
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp) // Larger touch target
                                            .clip(CircleShape)
                                            .background(
                                                if (showSearchBar) Color.White.copy(alpha = 0.3f) 
                                                else Color.White.copy(alpha = 0.22f) // Match main screen opacity
                                            )
                                            .clickable { showSearchBar = !showSearchBar },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp) // Slightly larger icon
                                        )
                                    }
                                    
                                    // Enhanced Filters Button
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp) // Larger touch target
                                            .clip(CircleShape)
                                            .background(
                                                if (showAdvancedFilters) Color.White.copy(alpha = 0.3f)
                                                else Color.White.copy(alpha = 0.22f) // Match main screen opacity
                                            )
                                            .clickable { showAdvancedFilters = !showAdvancedFilters },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = "Filters",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp) // Slightly larger icon
                                        )
                                    }
                                    
                                    // Enhanced Clear All Button
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp) // Larger touch target
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.22f)) // Match main screen opacity
                                            .clickable { transactionViewModel.clearAllTransactions() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Clear All",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp) // Slightly larger icon
                                        )
                                    }
                                }
                            }
                            
                            // Add visual breathing room - clean, minimal design
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Subtle visual accent - elegant divider line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.15f),
                                        RoundedCornerShape(0.5.dp)
                                    )
                            )
                        }
                    }
                }
            
                // Search Bar - Enhanced modern styling matching main screen exactly
                if (showSearchBar) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = Color.Black.copy(alpha = 0.15f),
                                spotColor = Color.Black.copy(alpha = 0.15f)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)), // Match main screen card color
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { 
                                Text(
                                    "Search transactions...", 
                                    color = mutedForegroundColor,
                                    fontSize = 14.sp
                                ) 
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp), // Reduced padding
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = mutedForegroundColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = primaryForegroundColor,
                                unfocusedTextColor = primaryForegroundColor,
                                focusedBorderColor = Color(0xFF7BA8F5), // Match main screen accent color
                                unfocusedBorderColor = borderColor,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
                
                // Enhanced Filter Section with better visual hierarchy
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color.Black.copy(alpha = 0.1f),
                            spotColor = Color.Black.copy(alpha = 0.1f)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F0F0F)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        // Filter Header with count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filters",
                                fontSize = 14.sp, // Reduced size
                                fontWeight = FontWeight.SemiBold,
                                color = primaryForegroundColor,
                                letterSpacing = 0.2.sp
                            )
                            
                            if (selectedFilter != "All") {
                                Text(
                                    text = "${filteredTransactions.size} results",
                                    fontSize = 10.sp, // Reduced size
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF7BA8F5),
                                    letterSpacing = 0.1.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Enhanced Filter Chips with better visual design
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            val filters = listOf("All", "Success", "Failed", "Pending")
                            items(filters) { filter ->
                                val isSelected = selectedFilter == filter
                                val count = when (filter) {
                                    "All" -> allTransactions.size
                                    "Success" -> allTransactions.count { it.status.uppercase() in listOf("SUCCESS", "SUCCESSFUL", "COMPLETED") }
                                    "Failed" -> allTransactions.count { it.status.uppercase() in listOf("FAILED", "DECLINED", "CANCELLED") }
                                    "Pending" -> allTransactions.count { it.status.uppercase() == "PENDING" }
                                    else -> 0
                                }
                                
                                FilterChip(
                                    onClick = { selectedFilter = filter },
                                    label = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                filter, 
                                                color = if (isSelected) Color.White else primaryForegroundColor,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                            if (count > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) Color.White.copy(alpha = 0.2f)
                                                            else Color(0xFF7BA8F5).copy(alpha = 0.2f)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = count.toString(),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else Color(0xFF7BA8F5)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    selected = isSelected,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF7BA8F5),
                                        containerColor = Color(0xFF1A1A1A),
                                        selectedLabelColor = Color.White,
                                        labelColor = primaryForegroundColor
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) Color(0xFF7BA8F5) else Color(0xFF404040),
                                        selectedBorderColor = Color(0xFF7BA8F5),
                                        borderWidth = 1.dp
                                    )
                                )
                            }
                        }
                        
                        // Advanced Filters Section
                        if (showAdvancedFilters) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Quick Date Filters
                                val quickFilters = listOf("Today", "This Week", "This Month")
                                quickFilters.forEach { quickFilter ->
                                    FilterChip(
                                        onClick = { /* Implement date filtering */ },
                                        label = { 
                                            Text(
                                                quickFilter, 
                                                color = primaryForegroundColor,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp
                                            ) 
                                        },
                                        selected = false,
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = Color(0xFF1A1A1A),
                                            labelColor = primaryForegroundColor
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = false,
                                            borderColor = Color(0xFF404040),
                                            borderWidth = 1.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Transaction List with Enhanced States
            when {
                isLoading -> {
                    // Enhanced loading state with skeleton loading
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = Color.Black.copy(alpha = 0.15f),
                                spotColor = Color.Black.copy(alpha = 0.15f)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F0F0F)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Enhanced loading animation
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF7BA8F5).copy(alpha = 0.3f),
                                                Color(0xFF6A96EE).copy(alpha = 0.1f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = Color(0xFF7BA8F5).copy(alpha = 0.3f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = Color(0xFF7BA8F5),
                                    strokeWidth = 4.dp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                text = "Loading Transactions",
                                fontSize = 16.sp, // Reduced size
                                fontWeight = FontWeight.Bold,
                                color = primaryForegroundColor,
                                letterSpacing = 0.2.sp
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Fetching your payment history...",
                                fontSize = 12.sp, // Reduced size
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFAAAAAA),
                                textAlign = TextAlign.Center,
                                letterSpacing = 0.1.sp
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Skeleton loading items
                            repeat(3) {
                                SkeletonTransactionItem()
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
                
                error != null -> {
                    // Enhanced error state matching main screen exactly
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp) // Reduced spacing
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(20.dp), // Match main screen card shape
                                ambientColor = Color.Black.copy(alpha = 0.15f),
                                spotColor = Color.Black.copy(alpha = 0.15f)
                            ),
                        shape = RoundedCornerShape(20.dp), // Match main screen card shape
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F0F0F) // Match main screen card color
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp), // Reduced padding
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        Color(0xFF000000),
                                        CircleShape
                                    )
                                    .border(
                                        width = 0.8.dp,
                                        color = Color(0xFF7BA8F5).copy(alpha = 0.25f), // Match main screen accent color
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Error",
                                    modifier = Modifier.size(32.dp),
                                    tint = Color(0xFFFF6B6B)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp)) // Match main screen spacing
                            Text(
                                text = "Failed to load transactions",
                                fontSize = 14.sp, // Reduced size
                                fontWeight = FontWeight.SemiBold,
                                color = primaryForegroundColor
                            )
                            Spacer(modifier = Modifier.height(6.dp)) // Match main screen spacing
                            Text(
                                text = error ?: "Unknown error",
                                fontSize = 11.sp, // Reduced size
                                color = mutedForegroundColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp)) // Match main screen spacing
                            Button(
                                onClick = { transactionViewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7BA8F5) // Match main screen accent color
                                ),
                                shape = RoundedCornerShape(12.dp) // Match main screen button shape
                            ) {
                                Text(
                                    "Retry", 
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp // Reduced size
                                )
                            }
                        }
                    }
                }
                
                filteredTransactions.isEmpty() -> {
                    // Enhanced empty state with better visual hierarchy
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = Color.Black.copy(alpha = 0.15f),
                                spotColor = Color.Black.copy(alpha = 0.15f)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F0F0F)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Enhanced empty state illustration
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF7BA8F5).copy(alpha = 0.1f),
                                                Color(0xFF6A96EE).copy(alpha = 0.05f)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = Color(0xFF7BA8F5).copy(alpha = 0.2f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "No transactions",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color(0xFF7BA8F5).copy(alpha = 0.6f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No Matching Transactions" else "No Transactions Yet",
                                fontSize = 18.sp, // Reduced size
                                fontWeight = FontWeight.Bold,
                                color = primaryForegroundColor,
                                letterSpacing = 0.2.sp,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = if (searchQuery.isNotEmpty()) 
                                    "Try adjusting your search criteria or filters" 
                                else 
                                    "Your payment history will appear here once you make transactions",
                                fontSize = 12.sp, // Reduced size
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFAAAAAA),
                                textAlign = TextAlign.Center,
                                letterSpacing = 0.1.sp,
                                lineHeight = 16.sp
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Action buttons for empty state
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (searchQuery.isNotEmpty()) {
                                    Button(
                                        onClick = { searchQuery = "" },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF7BA8F5)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            "Clear Search",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp // Reduced size
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = { /* Navigate to payment screen */ },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF7BA8F5)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            "Make Payment",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp // Reduced size
                                        )
                                    }
                                }
                                
                                OutlinedButton(
                                    onClick = { showSearchBar = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Search",
                                        color = Color(0xFF7BA8F5),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp // Reduced size
                                    )
                                }
                            }
                        }
                    }
                }
                
                else -> {
                    // Enhanced transaction list with modern card container - Match main screen exactly
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp) // Reduced spacing
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(20.dp), // Match main screen card shape
                                ambientColor = Color.Black.copy(alpha = 0.15f),
                                spotColor = Color.Black.copy(alpha = 0.15f)
                            ),
                        shape = RoundedCornerShape(20.dp), // Match main screen card shape
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F0F0F) // Match main screen card color exactly
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 700.dp), // Increased height for better content density
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), // Reduced padding
                            verticalArrangement = Arrangement.spacedBy(8.dp) // Reduced spacing
                        ) {
                            items(filteredTransactions) { transaction ->
                                TransactionHistoryItem(
                                    transaction = transaction,
                                    onDelete = { transactionViewModel.deleteTransaction(transaction) },
                                    onClick = { selectedTransaction = transaction }
                                )
                            }
                        }
                    }
                }
            }
            
            // Bottom spacing for better scrolling
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Transaction Detail Dialog
        selectedTransaction?.let { transaction ->
            TransactionDetailDialog(
                transaction = transaction,
                onDismiss = { selectedTransaction = null }
            )
        }
    }
    }
}

@Composable
fun TransactionHistoryItem(
    transaction: Transaction,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val primaryForegroundColor = Color.White
    val cardColor = Color(0xFF1A1A1A)
    val mutedForegroundColor = Color(0xFF888888)
    val statusColor = getStatusColor(transaction.status)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp) // Reduced height for better space utilization
            .clickable { onClick() }
            .shadow(
                elevation = 6.dp, // Increased elevation for better depth
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Transaction details with enhanced hierarchy
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Top row - Name and Amount with better visual hierarchy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Enhanced recipient name with avatar placeholder
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar placeholder with status color accent
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            statusColor.copy(alpha = 0.2f),
                                            statusColor.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                                .border(
                                    width = 2.dp,
                                    color = statusColor.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (transaction.recipientName ?: transaction.phoneNumber ?: "U").take(1).uppercase(),
                                fontSize = 14.sp, // Reduced size
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                letterSpacing = 0.3.sp
                            )
                        }
                        
                        Column {
                            Text(
                                text = transaction.recipientName ?: transaction.phoneNumber ?: "Unknown",
                                fontSize = 14.sp, // Reduced size
                                fontWeight = FontWeight.SemiBold,
                                color = primaryForegroundColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                letterSpacing = 0.2.sp
                            )
                            Text(
                                text = transaction.bankName,
                                fontSize = 10.sp, // Reduced size
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFAAAAAA),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                letterSpacing = 0.1.sp
                            )
                        }
                    }
                    
                    // Enhanced amount display
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = formatAmount(transaction.amount.toDoubleOrNull() ?: 0.0),
                            fontSize = 16.sp, // Reduced size
                            fontWeight = FontWeight.Bold,
                            color = primaryForegroundColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = formatDate(transaction.timestamp),
                            fontSize = 9.sp, // Reduced size
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF888888),
                            letterSpacing = 0.05.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Bottom row - Enhanced status and metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Enhanced status indicator with icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Status icon
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (transaction.status.uppercase()) {
                                    "SUCCESS", "SUCCESSFUL", "COMPLETED" -> "✓"
                                    "PENDING" -> "⏱"
                                    "FAILED", "DECLINED", "CANCELLED" -> "✕"
                                    else -> "?"
                                },
                                fontSize = 10.sp, // Reduced size
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                        
                        // Status text with better styling
                        Text(
                            text = transaction.status.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 11.sp, // Reduced size
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            letterSpacing = 0.1.sp
                        )
                    }
                    
                    // Transaction reference or additional info
                    if (transaction.phoneNumber != null) {
                        Text(
                            text = "Ref: ${transaction.phoneNumber.takeLast(4)}",
                            fontSize = 8.sp, // Reduced size
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF666666),
                            letterSpacing = 0.05.sp
                        )
                    }
                }
            }
            
            // Right side - Action buttons with better visual design
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Delete button with confirmation state
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.1f))
                        .clickable { onDelete },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp), // Reduced size
                        tint = Color.Red.copy(alpha = 0.8f)
                    )
                }
                
                // View details indicator
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7BA8F5).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "→",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7BA8F5)
                    )
                }
            }
        }
    }
}

@Composable
fun SkeletonTransactionItem() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side skeleton
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar skeleton
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2A2A))
                )
                
                Column {
                    // Name skeleton
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2A2A2A))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Bank skeleton
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2A2A2A))
                    )
                }
            }
            
            // Right side skeleton
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Amount skeleton
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF2A2A2A))
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Date skeleton
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF2A2A2A))
                )
            }
        }
    }
}

