package com.finalapp.accommodationapp.screens.landlord

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finalapp.accommodationapp.data.UserSession
import com.finalapp.accommodationapp.data.model.Review
import com.finalapp.accommodationapp.data.repository.landlord.LandlordRepository
import com.finalapp.accommodationapp.data.repository.student.ReviewRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordReviewsScreen(
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val reviewRepository = remember { ReviewRepository() }
    val landlordRepository = remember { LandlordRepository() }
    val snackbarHostState = remember { SnackbarHostState() }

    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var landlordId by remember { mutableStateOf<Int?>(null) }
    var averagePropertyRating by remember { mutableStateOf(0.0) }
    var averageLandlordRating by remember { mutableStateOf(0.0) }

    // Load reviews
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoading = true
            try {
                val userId = UserSession.currentUser?.userId
                if (userId != null) {
                    val landlordInfo = landlordRepository.getLandlordByUserId(userId)
                    if (landlordInfo != null) {
                        landlordId = landlordInfo.landlordId
                        reviews = reviewRepository.getReviewsForLandlord(landlordInfo.landlordId)

                        // Calculate average ratings
                        if (reviews.isNotEmpty()) {
                            averagePropertyRating = reviews.map { it.propertyRating }.average()
                            averageLandlordRating = reviews.map { it.landlordRating }.average()
                        }
                    }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error loading reviews: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Reviews") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (reviews.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No reviews yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Reviews from students will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stats Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Review Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = reviews.size.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Total Reviews",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = String.format("%.1f", averagePropertyRating),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = Color(0xFFFFD700)
                                        )
                                    }
                                    Text(
                                        text = "Property Rating",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = String.format("%.1f", averageLandlordRating),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = Color(0xFFFFD700)
                                        )
                                    }
                                    Text(
                                        text = "Landlord Rating",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Individual Reviews
                items(reviews.sortedByDescending { it.createdAt }) { review ->
                    ReviewCard(review = review)
                }
            }
        }
    }
}

@Composable
fun ReviewCard(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with property title and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.propertyTitle ?: "Property #${review.propertyId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!review.studentName.isNullOrEmpty()) {
                        Text(
                            text = "by ${review.studentName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                review.createdAt?.let { date ->
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    Text(
                        text = dateFormat.format(date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ratings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Property Rating
                Column {
                    Text(
                        text = "Property",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (index < review.propertyRating)
                                    Color(0xFFFFD700)
                                else
                                    Color.LightGray
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${review.propertyRating}/5)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Landlord Rating
                Column {
                    Text(
                        text = "Landlord",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (index < review.landlordRating)
                                    Color(0xFFFFD700)
                                else
                                    Color.LightGray
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${review.landlordRating}/5)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Comment
            if (!review.comment.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
