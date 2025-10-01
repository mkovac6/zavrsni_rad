package com.finalapp.accommodationapp.screens.student

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import com.finalapp.accommodationapp.data.repository.landlord.LandlordRepository
import com.finalapp.accommodationapp.data.repository.student.FavoritesRepository
import com.finalapp.accommodationapp.data.repository.student.BookingRepository
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import com.finalapp.accommodationapp.data.model.Property
import com.finalapp.accommodationapp.data.UserSession
import com.finalapp.accommodationapp.screens.components.BookingDialog
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    propertyId: Int,
    onNavigateBack: () -> Unit,
    onBookingClick: () -> Unit,
    onEditClick: ((Int) -> Unit)? = null
) {
    val propertyRepository = remember { PropertyRepository() }
    val landlordRepository = remember { LandlordRepository() }
    val favoritesRepository = remember { FavoritesRepository() }
    val bookingRepository = remember { BookingRepository() }
    val studentRepository = remember { StudentRepository() }

    var property by remember { mutableStateOf<Property?>(null) }
    var amenities by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLandlordOwner by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var studentId by remember { mutableStateOf(0) }
    var showBookingDialog by remember { mutableStateOf(false) }
    var isProcessingBooking by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load property details and check ownership/favorites
    LaunchedEffect(propertyId) {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                // Load property details
                val loadedProperty = propertyRepository.getPropertyById(propertyId)
                if (loadedProperty != null) {
                    property = loadedProperty

                    // Load amenities
                    amenities = propertyRepository.getPropertyAmenitiesAsStrings(propertyId)

                    // Check if current user is the landlord owner
                    val currentUser = UserSession.currentUser
                    if (currentUser?.userType == "landlord") {
                        try {
                            val landlord = landlordRepository.getLandlordByUserId(currentUser.userId)
                            if (landlord != null) {
                                // Get the property's landlord ID
                                val propertyLandlordId = propertyRepository.getLandlordIdByPropertyId(propertyId)
                                isLandlordOwner = landlord.landlordId == propertyLandlordId
                            }
                        } catch (e: Exception) {
                            Log.e("PropertyDetail", "Error checking landlord ownership: ${e.message}")
                        }
                    } else if (currentUser?.userType == "student") {
                        Log.d("PropertyDetail", "Current user is student with userId: ${currentUser.userId}")

                        // Get student profile
                        try {
                            val student = studentRepository.getStudentProfile(currentUser.userId)
                            if (student != null) {
                                studentId = student.studentId
                                Log.d("PropertyDetail", "Found student profile with studentId: $studentId")
                                isFavorite = favoritesRepository.isFavorite(student.studentId, propertyId)
                            } else {
                                Log.e("PropertyDetail", "No student profile found for userId: ${currentUser.userId}")
                            }
                        } catch (e: Exception) {
                            Log.e("PropertyDetail", "Error loading student profile: ${e.message}")
                        }
                    }
                } else {
                    errorMessage = "Property not found"
                }
            } catch (e: Exception) {
                Log.e("PropertyDetail", "Error loading property: ${e.message}", e)
                errorMessage = "Error loading property: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Property Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Show edit button if user is the landlord owner
                    if (isLandlordOwner && onEditClick != null) {
                        IconButton(onClick = { onEditClick(propertyId) }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Edit Property",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Show favorite button for students
                    if (UserSession.currentUser?.userType == "student" && studentId > 0) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        if (isFavorite) {
                                            val removed = favoritesRepository.removeFromFavorites(studentId, propertyId)
                                            if (removed) {
                                                isFavorite = false
                                                snackbarHostState.showSnackbar("Removed from favorites")
                                            }
                                        } else {
                                            val added = favoritesRepository.addToFavorites(studentId, propertyId)
                                            if (added) {
                                                isFavorite = true
                                                snackbarHostState.showSnackbar("Added to favorites")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error updating favorites")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            property?.let { prop ->
                // Only show booking bar for students
                if (UserSession.currentUser?.userType == "student" && studentId > 0) {
                    BottomAppBar {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "€${prop.pricePerMonth.toInt()}/month",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                prop.availableFrom?.let { date ->
                                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    val isAvailable = date.before(Date()) || date == Date()
                                    Text(
                                        text = if (isAvailable) "Available Now" else "Available from ${dateFormat.format(date)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isAvailable) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = { showBookingDialog = true },
                                modifier = Modifier.height(48.dp),
                                enabled = !isProcessingBooking
                            ) {
                                if (isProcessingBooking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Filled.DateRange, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Book Now")
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
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
                            Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }
            }
            property != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Property Title and Type
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = property!!.title,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AssistChip(
                                            onClick = { },
                                            label = { Text(property!!.propertyType.replaceFirstChar { it.uppercase() }) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.Home,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                        // Show active/inactive status for landlords
                                        if (isLandlordOwner) {
                                            AssistChip(
                                                onClick = { },
                                                label = {
                                                    Text(if (property!!.isActive) "Active" else "Inactive")
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = if (property!!.isActive)
                                                        Color(0xFF4CAF50).copy(alpha = 0.2f)
                                                    else
                                                        Color(0xFFFF5252).copy(alpha = 0.2f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Location
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = property!!.address,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${property!!.city} ${property!!.postalCode}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    // Key Features
                    item {
                        Text(
                            text = "Key Features",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FeatureChip(
                                icon = Icons.Filled.Person,
                                label = "${property!!.bedrooms} Bedroom${if (property!!.bedrooms > 1) "s" else ""}"
                            )
                            FeatureChip(
                                icon = Icons.Filled.Person,
                                label = "${property!!.bathrooms} Bathroom${if (property!!.bathrooms > 1) "s" else ""}"
                            )
                            FeatureChip(
                                icon = Icons.Filled.Person,
                                label = "${property!!.totalCapacity} People"
                            )
                        }
                    }

                    // Description
                    if (property!!.description.isNotEmpty()) {
                        item {
                            Text(
                                text = "Description",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = property!!.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    // Amenities
                    if (amenities.isNotEmpty()) {
                        item {
                            Text(
                                text = "Amenities",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                amenities.forEach { amenity ->
                                    AssistChip(
                                        onClick = { },
                                        label = { Text(amenity) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Availability dates
                    item {
                        Text(
                            text = "Availability",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                property!!.availableFrom?.let { date ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.DateRange,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Available from",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = dateFormat.format(date),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                property!!.availableTo?.let { date ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Clear,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Available until",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = dateFormat.format(date),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pricing information card
                    item {
                        Text(
                            text = "Pricing",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Monthly Rent",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "€${property!!.pricePerMonth.toInt()}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Booking Dialog
    if (showBookingDialog && property != null && studentId > 0) {
        BookingDialog(
            propertyId = propertyId,
            pricePerMonth = property!!.pricePerMonth,
            availableFrom = property!!.availableFrom,
            availableTo = property!!.availableTo,
            onDismiss = { showBookingDialog = false },
            onConfirm = { startDate, endDate, totalPrice, message ->
                scope.launch {
                    isProcessingBooking = true
                    showBookingDialog = false

                    try {
                        // Check availability first
                        val isAvailable = bookingRepository.checkDateAvailability(propertyId, startDate, endDate)

                        if (isAvailable) {
                            // Create booking with the studentId
                            val success = bookingRepository.createBooking(
                                propertyId = propertyId,
                                studentId = studentId,
                                startDate = startDate,
                                endDate = endDate,
                                totalPrice = totalPrice,
                                messageToLandlord = message
                            )

                            if (success) {
                                snackbarHostState.showSnackbar("Booking request sent successfully!")
                            } else {
                                snackbarHostState.showSnackbar("Failed to send booking request")
                            }
                        } else {
                            snackbarHostState.showSnackbar("Selected dates are not available")
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error creating booking: ${e.message}")
                    } finally {
                        isProcessingBooking = false
                    }
                }
            }
        )
    }
}

@Composable
fun FeatureChip(
    icon: ImageVector,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        content()
    }
}