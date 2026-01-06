package com.finalapp.accommodationapp.screens.landlord

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import com.finalapp.accommodationapp.data.repository.landlord.LandlordRepository
import com.finalapp.accommodationapp.data.repository.student.BookingRepository
import com.finalapp.accommodationapp.data.model.Property
import com.finalapp.accommodationapp.data.UserSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.finalapp.accommodationapp.data.repository.student.ReviewRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordHomeScreen(
    onPropertyClick: (Int) -> Unit,
    onAddProperty: () -> Unit,
    onEditProperty: (Int) -> Unit,
    onBookingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onReviewsClick: () -> Unit,
    onLogout: () -> Unit
) {
    val propertyRepository = remember { PropertyRepository() }
    val landlordRepository = remember { LandlordRepository() }
    val bookingRepository = remember { BookingRepository() }
    val reviewRepository = remember { ReviewRepository() }

    var properties by remember { mutableStateOf<List<Property>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var landlordName by remember { mutableStateOf("") }
    var landlordId by remember { mutableStateOf<Int?>(null) }
    var pendingBookingsCount by remember { mutableStateOf(0) }
    var newReviewsCount by remember { mutableStateOf(0) }
    var selectedTab by remember { mutableStateOf(0) } // 0=Home, 1=Bookings, 2=Profile

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog states
    var showWarningDialog by remember { mutableStateOf(false) }
    var propertyToToggle by remember { mutableStateOf<Property?>(null) }

    // Function to reload properties
    fun loadProperties() {
        scope.launch {
            isLoading = true
            if (landlordId != null) {
                properties = propertyRepository.getPropertiesByLandlordId(landlordId!!)
                pendingBookingsCount = bookingRepository.countPendingBookingsForLandlord(landlordId!!)
            }
                newReviewsCount = reviewRepository.getNewReviewsCount(landlordId!!)
            isLoading = false
        }
    }

    // Load landlord's properties and bookings count on first composition
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true

            // Get landlord ID from current user
            val userId = UserSession.currentUser?.userId
            if (userId != null) {
                val landlordInfo = landlordRepository.getLandlordByUserId(userId)
                if (landlordInfo != null) {
                    landlordName = "${landlordInfo.firstName} ${landlordInfo.lastName}"
                    landlordId = landlordInfo.landlordId
                    properties = propertyRepository.getPropertiesByLandlordId(landlordInfo.landlordId)
                    pendingBookingsCount = bookingRepository.countPendingBookingsForLandlord(landlordInfo.landlordId)
                    newReviewsCount = reviewRepository.getNewReviewsCount(landlordInfo.landlordId)
                }
            }

            isLoading = false
        }
    }

    // Refresh pending bookings count when returning to home
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && landlordId != null) {
            pendingBookingsCount = bookingRepository.countPendingBookingsForLandlord(landlordId!!)
            newReviewsCount = reviewRepository.getNewReviewsCount(landlordId!!)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Welcome back!",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            landlordName.ifEmpty { "Landlord" },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = {
                        BadgedBox(
                            badge = {
                                if (pendingBookingsCount > 0) {
                                    Badge {
                                        Text(
                                            text = pendingBookingsCount.toString(),
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
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onBookingsClick()
                    }
                )
                NavigationBarItem(
                    icon = {
                        BadgedBox(
                            badge = {
                                if (newReviewsCount > 0) {
                                    Badge {
                                        Text(
                                            text = newReviewsCount.toString(),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = "Reviews")
                        }
                    },
                    label = { Text("Reviews") },
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        onReviewsClick()
                    }
                )
            }
        },
        floatingActionButton = {
            if (properties.isNotEmpty() && selectedTab == 0) {
                FloatingActionButton(
                    onClick = onAddProperty,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Property")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = properties.size.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Total Properties",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = properties.count { it.isActive }.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = pendingBookingsCount.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingBookingsCount > 0)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Pending",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Properties",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (pendingBookingsCount > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "$pendingBookingsCount new booking${if (pendingBookingsCount > 1) "s" else ""}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                if (newReviewsCount > 0) {
                    Card(
                        onClick = onReviewsClick,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "$newReviewsCount new review${if (newReviewsCount > 1) "s" else ""}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (properties.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No properties yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Add your first property to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onAddProperty) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Property")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(properties) { property ->
                        LandlordPropertyCard(
                            property = property,
                            onClick = { onPropertyClick(property.propertyId) },
                            onEditClick = { onEditProperty(property.propertyId) },
                            onToggleStatus = {
                                // Check if trying to deactivate a property before its availability date
                                val today = Date()
                                if (property.isActive && property.availableFrom != null && property.availableFrom.after(today)) {
                                    // Show warning when DEACTIVATING before availability date
                                    propertyToToggle = property
                                    showWarningDialog = true
                                } else {
                                    // Toggle directly (activating or deactivating after availability date)
                                    scope.launch {
                                        val success = propertyRepository.updatePropertyStatus(
                                            property.propertyId,
                                            !property.isActive
                                        )
                                        if (success) {
                                            loadProperties()
                                            snackbarHostState.showSnackbar(
                                                if (property.isActive) "Property deactivated" else "Property activated"
                                            )
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to update property status")
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Warning Dialog
    if (showWarningDialog && propertyToToggle != null) {
        AlertDialog(
            onDismissRequest = {
                showWarningDialog = false
                propertyToToggle = null
            },
            title = { Text("Warning") },
            text = {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                Text(
                    "This property is scheduled to be available on ${dateFormat.format(propertyToToggle!!.availableFrom)}. " +
                            "Are you sure you want to deactivate it before this date?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            propertyToToggle?.let { property ->
                                val success = propertyRepository.updatePropertyStatus(
                                    property.propertyId,
                                    false
                                )
                                if (success) {
                                    loadProperties()
                                    snackbarHostState.showSnackbar("Property deactivated")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to update property status")
                                }
                            }
                            showWarningDialog = false
                            propertyToToggle = null
                        }
                    }
                ) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showWarningDialog = false
                        propertyToToggle = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// LandlordPropertyCard remains the same as before
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordPropertyCard(
    property: Property,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onToggleStatus: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = property.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${property.address}, ${property.city}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Edit button
                IconButton(
                    onClick = { onEditClick() }
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit Property",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Property details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "€${property.pricePerMonth.toInt()}/month",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${property.bedrooms} bed • ${property.bathrooms} bath",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = property.propertyType.capitalize(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Available from
            property.availableFrom?.let { date ->
                Spacer(modifier = Modifier.height(8.dp))
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Available from ${dateFormat.format(date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (property.isActive) "Active" else "Inactive",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (property.isActive)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = property.isActive,
                        onCheckedChange = { onToggleStatus() }
                    )
                }
            }
        }
    }
}