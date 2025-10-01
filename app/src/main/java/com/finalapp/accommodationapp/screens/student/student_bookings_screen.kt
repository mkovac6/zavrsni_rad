package com.finalapp.accommodationapp.screens.student

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
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentBookingsScreen(
    onNavigateBack: () -> Unit,
    onPropertyClick: (Int) -> Unit,
    onHomeClick: () -> Unit,
    onFavoritesClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val bookingRepository = remember { BookingRepository() }
    val studentRepository = remember { StudentRepository() }
    val snackbarHostState = remember { SnackbarHostState() }

    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var studentId by remember { mutableStateOf(0) }

    // Load bookings
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoading = true
            try {
                // Get student ID
                val userId = UserSession.currentUser?.userId ?: 0
                val studentProfile = studentRepository.getStudentProfile(userId)

                if (studentProfile != null) {
                    studentId = studentProfile.studentId
                    bookings = bookingRepository.getStudentBookings(studentProfile.studentId)
                } else {
                    snackbarHostState.showSnackbar("Unable to load student profile")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error loading bookings: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Filter bookings by status
    val filteredBookings = when (selectedTab) {
        0 -> bookings // All
        1 -> bookings.filter { it.status.lowercase() == "pending" }
        2 -> bookings.filter { it.status.lowercase() == "approved" }
        3 -> bookings.filter { it.status.lowercase() in listOf("rejected", "cancelled") }
        else -> bookings
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Bookings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = "Bookings") },
                    label = { Text("Bookings") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
                    label = { Text("Favorites") },
                    selected = false,
                    onClick = onFavoritesClick
                )
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
                        Text("All (${bookings.size})")
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text("Pending (${bookings.filter { it.status.lowercase() == "pending" }.size})")
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text("Approved (${bookings.filter { it.status.lowercase() == "approved" }.size})")
                    }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text("Rejected (${bookings.filter { it.status.lowercase() in listOf("rejected", "cancelled") }.size})")
                    }
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
                            "No bookings found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = when (selectedTab) {
                                1 -> "You don't have any pending bookings"
                                2 -> "You don't have any approved bookings"
                                3 -> "You don't have any rejected bookings"
                                else -> "You haven't made any bookings yet"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
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
                        BookingCard(
                            booking = booking,
                            onPropertyClick = { onPropertyClick(booking.propertyId) },
                            onCancelBooking = if (booking.status.lowercase() == "pending") {
                                {
                                    coroutineScope.launch {
                                        val success = bookingRepository.updateBookingStatus(
                                            booking.bookingId,
                                            "cancelled"
                                        )
                                        if (success) {
                                            // Reload bookings
                                            val studentProfile = studentRepository.getStudentProfile(
                                                UserSession.currentUser?.userId ?: 0
                                            )
                                            if (studentProfile != null) {
                                                bookings = bookingRepository.getStudentBookings(studentProfile.studentId)
                                            }
                                            snackbarHostState.showSnackbar("Booking cancelled successfully")
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to cancel booking")
                                        }
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingCard(
    booking: Booking,
    onPropertyClick: () -> Unit,
    onCancelBooking: (() -> Unit)? = null
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
            // Status Badge and Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.propertyTitle ?: "Property #${booking.propertyId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!booking.propertyAddress.isNullOrEmpty()) {
                        Text(
                            text = booking.propertyAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(status = booking.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dates
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
                    text = if (booking.startDate != null && booking.endDate != null) {
                        "${dateFormat.format(booking.startDate)} - ${dateFormat.format(booking.endDate)}"
                    } else {
                        "Dates not specified"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Price
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Total: €${String.format("%.2f", booking.totalPrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // Message to landlord (if exists and pending)
            if (!booking.messageToLandlord.isNullOrBlank() && booking.status.lowercase() == "pending") {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Your message:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = booking.messageToLandlord,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Booking date
            booking.createdAt?.let { created ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Requested on: ${dateFormat.format(created)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Cancel button for pending bookings
            if (onCancelBooking != null && booking.status.lowercase() == "pending") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancelBooking,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel Booking")
                }
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
                text = status.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}