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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finalapp.accommodationapp.data.UserSession
import com.finalapp.accommodationapp.data.model.Booking
import com.finalapp.accommodationapp.data.repository.student.BookingRepository
import com.finalapp.accommodationapp.data.repository.landlord.LandlordRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordBookingManagementScreen(
    onNavigateBack: () -> Unit,
    onPropertyClick: (Int) -> Unit,
    onHomeClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val bookingRepository = remember { BookingRepository() }
    val landlordRepository = remember { LandlordRepository() }

    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var processingBookingId by remember { mutableStateOf<Int?>(null) }
    var showSuccessMessage by remember { mutableStateOf("") }
    var pendingCount by remember { mutableStateOf(0) }

    // Load bookings
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoading = true

            // Get landlord ID
            val userId = UserSession.currentUser?.userId ?: 0
            val landlordProfile = landlordRepository.getLandlordByUserId(userId)

            if (landlordProfile != null) {
                bookings = bookingRepository.getLandlordBookings(landlordProfile.landlordId)
                pendingCount = bookings.count { it.status == "pending" }
            }

            isLoading = false
        }
    }

    // Filter bookings by status
    val filteredBookings = when (selectedTab) {
        0 -> bookings.filter { it.status == "pending" } // Pending
        1 -> bookings.filter { it.status == "approved" } // Approved
        2 -> bookings.filter { it.status in listOf("rejected", "cancelled", "completed") } // History
        else -> bookings
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking Requests") }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = false,
                    onClick = onHomeClick
                )
                NavigationBarItem(
                    icon = {
                        BadgedBox(
                            badge = {
                                if (pendingCount > 0) {
                                    Badge {
                                        Text(
                                            text = pendingCount.toString(),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Bookings")
                        }
                    },
                    label = { Text("Bookings") },
                    selected = true,
                    onClick = { }
                )
            }
        },
        snackbarHost = {
            if (showSuccessMessage.isNotEmpty()) {
                Snackbar(
                    action = {
                        TextButton(onClick = { showSuccessMessage = "" }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(showSuccessMessage)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Pending")
                            if (pendingCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge { Text(pendingCount.toString()) }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Approved") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("History") }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredBookings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (selectedTab) {
                                0 -> "No pending booking requests"
                                1 -> "No approved bookings"
                                else -> "No booking history"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredBookings) { booking ->
                        LandlordBookingCard(
                            booking = booking,
                            onPropertyClick = { onPropertyClick(booking.propertyId) },
                            onApprove = if (booking.status == "pending") {
                                {
                                    coroutineScope.launch {
                                        processingBookingId = booking.bookingId
                                        val success = bookingRepository.updateBookingStatus(booking.bookingId, "approved")
                                        if (success) {
                                            showSuccessMessage = "Booking approved successfully"
                                            // Reload bookings
                                            val userId = UserSession.currentUser?.userId ?: 0
                                            val landlordProfile = landlordRepository.getLandlordByUserId(userId)
                                            if (landlordProfile != null) {
                                                bookings = bookingRepository.getLandlordBookings(landlordProfile.landlordId)
                                                pendingCount = bookings.count { it.status == "pending" }
                                            }
                                        }
                                        processingBookingId = null
                                    }
                                }
                            } else null,
                            onReject = if (booking.status == "pending") {
                                {
                                    coroutineScope.launch {
                                        processingBookingId = booking.bookingId
                                        val success = bookingRepository.updateBookingStatus(booking.bookingId, "rejected")
                                        if (success) {
                                            showSuccessMessage = "Booking rejected"
                                            // Reload bookings
                                            val userId = UserSession.currentUser?.userId ?: 0
                                            val landlordProfile = landlordRepository.getLandlordByUserId(userId)
                                            if (landlordProfile != null) {
                                                bookings = bookingRepository.getLandlordBookings(landlordProfile.landlordId)
                                                pendingCount = bookings.count { it.status == "pending" }
                                            }
                                        }
                                        processingBookingId = null
                                    }
                                }
                            } else null,
                            isProcessing = processingBookingId == booking.bookingId
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordBookingCard(
    booking: Booking,
    onPropertyClick: () -> Unit,
    onApprove: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    isProcessing: Boolean = false
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        onClick = onPropertyClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Property and student info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.propertyTitle ?: "Property",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Student: ${booking.studentName ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = booking.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dates and price
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
                    text = "${dateFormat.format(booking.startDate)} - ${dateFormat.format(booking.endDate)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Total: €${booking.totalPrice}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            // Message from student
            booking.messageToLandlord?.let { message ->
                if (message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Action buttons for pending bookings
            if (onApprove != null && onReject != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        OutlinedButton(
                            onClick = onReject,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Approve")
                        }
                    }
                }
            }

            // Booking date
            booking.createdAt?.let { created ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Requested on: ${dateFormat.format(created)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (containerColor, contentColor, icon) = when (status.lowercase()) {
        "pending" -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Filled.Refresh
        )
        "approved" -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Filled.Check
        )
        "rejected" -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Filled.Close
        )
        "cancelled" -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Filled.Clear
        )
        else -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Filled.Info
        )
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.capitalize(),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}