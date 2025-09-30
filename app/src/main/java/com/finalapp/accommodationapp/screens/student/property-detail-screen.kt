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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    propertyId: Int,
    onNavigateBack: () -> Unit,
    onBookingClick: () -> Unit,
    onEditClick: ((Int) -> Unit)? = null  // Add optional edit navigation
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
    var bookingMessage by remember { mutableStateOf("") }
    var isProcessingBooking by remember { mutableStateOf(false) }
    var hasStudentProfile by remember { mutableStateOf(false) }

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
                        val landlord = landlordRepository.getLandlordByUserId(currentUser.userId)
                        if (landlord != null) {
                            // Get the property's landlord ID
                            val propertyLandlordId = propertyRepository.getLandlordIdByPropertyId(propertyId)
                            isLandlordOwner = landlord.landlordId == propertyLandlordId
                        }
                    } else if (currentUser?.userType == "student") {
                        // Check if property is favorite
                        val student = studentRepository.getStudentProfile(currentUser.userId)
                        if (student != null) {
                            studentId = student.studentId
                            hasStudentProfile = true
                            isFavorite = favoritesRepository.isFavorite(student.studentId, propertyId)
                        } else {
                            // Student profile doesn't exist
                            hasStudentProfile = false
                            Log.e("PropertyDetail", "No student profile found for userId: ${currentUser.userId}")
                        }
                    }
                } else {
                    errorMessage = "Property not found"
                }
            } catch (e: Exception) {
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

                    // Show favorite button for students with profiles
                    if (UserSession.currentUser?.userType == "student" && hasStudentProfile && studentId > 0) {
                        IconButton(
                            onClick = {
                                scope.launch {
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
                if (UserSession.currentUser?.userType == "student") {
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
                                    Text(
                                        text = "Available from ${dateFormat.format(date)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    if (hasStudentProfile) {
                                        showBookingDialog = true
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Please complete your profile to make bookings")
                                        }
                                    }
                                },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Book Now")
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
                            color = MaterialTheme.colorScheme.error
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
                    // Show profile warning for students without profiles
                    if (UserSession.currentUser?.userType == "student" && !hasStudentProfile) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Complete your profile to book properties and save favorites",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

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
                                            label = { Text(property!!.propertyType.capitalize()) },
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
                            Text(
                                text = property!!.description,
                                style = MaterialTheme.typography.bodyLarge
                            )
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
                        }

                        item {
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

                    // Landlord Information (only show for students)
                    if (UserSession.currentUser?.userType == "student") {
                        item {
                            Text(
                                text = "Landlord Information",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = property!!.landlordName.ifEmpty { "Landlord" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        property!!.companyName?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        if (property!!.landlordPhone.isNotEmpty()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.Phone,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = property!!.landlordPhone,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                        if (property!!.landlordRating > 0) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.Star,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = Color(0xFFFFC107)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${property!!.landlordRating}/5.0",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
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
                                    Row {
                                        Text(
                                            text = "Available from: ",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = dateFormat.format(date),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                property!!.availableTo?.let { date ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row {
                                        Text(
                                            text = "Available until: ",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = dateFormat.format(date),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Booking Dialog
    if (showBookingDialog && property != null) {
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

                    // Make sure we have a valid student ID
                    if (studentId <= 0) {
                        snackbarHostState.showSnackbar("Please complete your student profile to make bookings")
                        isProcessingBooking = false
                        return@launch
                    }

                    // Check availability first
                    val isAvailable = bookingRepository.checkDateAvailability(propertyId, startDate, endDate)

                    if (isAvailable) {
                        // Create booking with the correct studentId
                        val success = bookingRepository.createBooking(
                            propertyId = propertyId,
                            studentId = studentId,  // This will now be valid
                            startDate = startDate,
                            endDate = endDate,
                            totalPrice = totalPrice,
                            messageToLandlord = message
                        )

                        if (success) {
                            snackbarHostState.showSnackbar("Booking request sent successfully!")
                            bookingMessage = ""
                        } else {
                            snackbarHostState.showSnackbar("Failed to send booking request")
                        }
                    } else {
                        snackbarHostState.showSnackbar("Selected dates are not available")
                    }

                    isProcessingBooking = false
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