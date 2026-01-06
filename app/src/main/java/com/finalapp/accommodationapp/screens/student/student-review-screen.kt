package com.finalapp.accommodationapp.screens.student

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.finalapp.accommodationapp.data.model.Booking
import com.finalapp.accommodationapp.data.repository.student.BookingRepository
import com.finalapp.accommodationapp.data.repository.student.ReviewRepository
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentReviewScreen(
    bookingId: Int,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val reviewRepository = remember { ReviewRepository() }
    val bookingRepository = remember { BookingRepository() }
    val studentRepository = remember { StudentRepository() }
    val snackbarHostState = remember { SnackbarHostState() }

    var booking by remember { mutableStateOf<Booking?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var propertyRating by remember { mutableIntStateOf(0) }
    var landlordRating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var studentId by remember { mutableIntStateOf(0) }

    // Load booking details
    LaunchedEffect(bookingId) {
        coroutineScope.launch {
            isLoading = true
            try {
                val userId = UserSession.currentUser?.userId ?: 0
                val studentProfile = studentRepository.getStudentProfile(userId)

                if (studentProfile != null) {
                    studentId = studentProfile.studentId
                    val bookings = bookingRepository.getStudentBookings(studentProfile.studentId)
                    booking = bookings.find { it.bookingId == bookingId }

                    if (booking == null) {
                        snackbarHostState.showSnackbar("Booking not found")
                    } else if (booking?.status?.lowercase() != "completed") {
                        snackbarHostState.showSnackbar("Can only review completed bookings")
                    }
                } else {
                    snackbarHostState.showSnackbar("Unable to load student profile")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error loading booking: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Leave a Review") },
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
        } else if (booking == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Booking not found",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Booking Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = booking?.propertyTitle ?: "Property #${booking?.propertyId}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (!booking?.propertyAddress.isNullOrEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = booking?.propertyAddress ?: "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Dates
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        if (booking?.startDate != null && booking?.endDate != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${dateFormat.format(booking?.startDate)} - ${dateFormat.format(booking?.endDate)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Property Rating Section
                Text(
                    text = "Rate the Property",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "How would you rate the property overall?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                StarRatingSelector(
                    rating = propertyRating,
                    onRatingChange = { propertyRating = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Landlord Rating Section
                Text(
                    text = "Rate the Landlord",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "How was your experience with the landlord?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                StarRatingSelector(
                    rating = landlordRating,
                    onRatingChange = { landlordRating = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Comment Section
                Text(
                    text = "Your Review (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Share your experience with other students",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = { Text("Tell us about your experience...") },
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (propertyRating == 0 || landlordRating == 0) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Please provide both property and landlord ratings")
                            }
                            return@Button
                        }

                        coroutineScope.launch {
                            isSubmitting = true
                            try {
                                val landlordId = booking?.landlordId

                                if (landlordId == null) {
                                    snackbarHostState.showSnackbar("Unable to identify landlord for this booking")
                                    isSubmitting = false
                                    return@launch
                                }

                                val success = reviewRepository.submitReview(
                                    bookingId = bookingId,
                                    propertyId = booking?.propertyId ?: 0,
                                    studentId = studentId,
                                    landlordId = landlordId,
                                    propertyRating = propertyRating,
                                    landlordRating = landlordRating,
                                    comment = comment.ifBlank { null }
                                )

                                if (success) {
                                    snackbarHostState.showSnackbar("Review submitted successfully!")
                                    // Navigate back after a short delay
                                    kotlinx.coroutines.delay(1000)
                                    onNavigateBack()
                                } else {
                                    snackbarHostState.showSnackbar("Failed to submit review. You may have already reviewed this booking.")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error submitting review: ${e.message}")
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting && propertyRating > 0 && landlordRating > 0
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isSubmitting) "Submitting..." else "Submit Review")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun StarRatingSelector(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.Star,
                contentDescription = "Star $i",
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onRatingChange(i) }
                    .padding(4.dp),
                tint = if (i <= rating) Color(0xFFFFD700) else Color.LightGray
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Rating label
    Text(
        text = when (rating) {
            0 -> "Tap to rate"
            1 -> "Poor"
            2 -> "Fair"
            3 -> "Good"
            4 -> "Very Good"
            5 -> "Excellent"
            else -> ""
        },
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = if (rating > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}
