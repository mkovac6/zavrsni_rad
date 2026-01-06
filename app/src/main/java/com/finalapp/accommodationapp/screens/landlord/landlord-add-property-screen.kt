package com.finalapp.accommodationapp.screens.landlord

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.finalapp.accommodationapp.data.repository.admin.AdminRepository
import com.finalapp.accommodationapp.data.repository.landlord.LandlordRepository
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import com.finalapp.accommodationapp.data.repository.PropertyImageRepository
import com.finalapp.accommodationapp.data.model.admin.Amenity
import com.finalapp.accommodationapp.data.UserSession
import com.finalapp.accommodationapp.screens.components.MultiImagePicker
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordAddPropertyScreen(
    onNavigateBack: () -> Unit,
    onPropertyAdded: () -> Unit
) {
    val adminRepository = remember { AdminRepository() }
    val landlordRepository = remember { LandlordRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Get current landlord
    var landlordId by remember { mutableStateOf<Int?>(null) }
    var landlordName by remember { mutableStateOf("") }

    // Load data
    var amenities by remember { mutableStateOf<List<Amenity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Form states
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var propertyType by remember { mutableStateOf("apartment") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var pricePerMonth by remember { mutableStateOf("") }
    var bedrooms by remember { mutableStateOf("1") }
    var bathrooms by remember { mutableStateOf("1") }
    var totalCapacity by remember { mutableStateOf("1") }
    var availableFrom by remember { mutableStateOf("") }
    var selectedAmenities by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showPastDateWarning by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isUploadingImages by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Load landlord info and amenities
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true

            // Debug: Check UserSession
            val currentUser = UserSession.currentUser
            Log.d("LandlordAddProperty", "Current user from session: ${currentUser?.email}")
            Log.d("LandlordAddProperty", "Current userId: ${currentUser?.userId}")
            Log.d("LandlordAddProperty", "User type: ${currentUser?.userType}")

            val userId = currentUser?.userId
            if (userId != null) {
                Log.d("LandlordAddProperty", "Attempting to fetch landlord for userId: $userId")

                val landlord = landlordRepository.getLandlordByUserId(userId)

                if (landlord != null) {
                    landlordId = landlord.landlordId
                    landlordName = "${landlord.firstName} ${landlord.lastName}"
                    Log.d("LandlordAddProperty", "Successfully loaded landlord: $landlordName (ID: $landlordId)")
                } else {
                    Log.e("LandlordAddProperty", "getLandlordByUserId returned null for userId: $userId")

                    // Let's check what the actual issue is
                    // This shouldn't happen since we know the profile exists
                    Log.e("LandlordAddProperty", "This user should have a landlord profile in the database!")
                }
            } else {
                Log.e("LandlordAddProperty", "UserSession.currentUser?.userId is null!")
                Log.e("LandlordAddProperty", "Full UserSession.currentUser: ${UserSession.currentUser}")
            }

            // Load amenities
            try {
                amenities = adminRepository.getAllAmenities()
                Log.d("LandlordAddProperty", "Loaded ${amenities.size} amenities")
            } catch (e: Exception) {
                Log.e("LandlordAddProperty", "Failed to load amenities: ${e.message}")
            }

            isLoading = false
        }
    }

    // Validation
    val isFormValid = landlordId != null &&
            title.isNotBlank() &&
            address.isNotBlank() &&
            city.isNotBlank() &&
            pricePerMonth.toDoubleOrNull() != null &&
            bedrooms.toIntOrNull() != null &&
            bathrooms.toIntOrNull() != null &&
            totalCapacity.toIntOrNull() != null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add New Property") },
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
        } else if (landlordId == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: Could not load landlord information")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show landlord info
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
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Adding as: $landlordName",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // Basic Information
                item {
                    Text(
                        text = "Basic Information",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Property Title *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }

                // Property Type
                item {
                    Text("Property Type", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("apartment", "house", "room", "studio", "shared").forEach { type ->
                            FilterChip(
                                selected = propertyType == type,
                                onClick = { propertyType = type },
                                label = { Text(type.capitalize()) }
                            )
                        }
                    }
                }

                // Location
                item {
                    Divider()
                    Text(
                        text = "Location",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address *") },
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City *") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = postalCode,
                            onValueChange = { postalCode = it },
                            label = { Text("Postal Code") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Details
                item {
                    Divider()
                    Text(
                        text = "Property Details",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    OutlinedTextField(
                        value = pricePerMonth,
                        onValueChange = { pricePerMonth = it },
                        label = { Text("Price per Month (€) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bedrooms,
                            onValueChange = { bedrooms = it },
                            label = { Text("Bedrooms *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bathrooms,
                            onValueChange = { bathrooms = it },
                            label = { Text("Bathrooms *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = totalCapacity,
                            onValueChange = { totalCapacity = it },
                            label = { Text("Capacity *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = availableFrom,
                        onValueChange = { },
                        label = { Text("Available From") },
                        leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Filled.DateRange, contentDescription = "Select date")
                            }
                        },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Amenities
                item {
                    Divider()
                    Text(
                        text = "Amenities",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(amenities.chunked(2)) { amenityPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        amenityPair.forEach { amenity ->
                            FilterChip(
                                selected = selectedAmenities.contains(amenity.amenityId),
                                onClick = {
                                    selectedAmenities = if (selectedAmenities.contains(amenity.amenityId)) {
                                        selectedAmenities - amenity.amenityId
                                    } else {
                                        selectedAmenities + amenity.amenityId
                                    }
                                },
                                label = { Text(amenity.name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (amenityPair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Property Images
                item {
                    Divider()
                    Text(
                        text = "Property Images",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Add 5-10 high-quality images (optional but recommended)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    MultiImagePicker(
                        selectedImages = selectedImageUris,
                        onImagesSelected = { selectedImageUris = it },
                        maxImages = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Submit Button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Check if available date is in the past
                            val today = Calendar.getInstance()
                            val todayDate = Date(today.timeInMillis)

                            val selectedDate = if (availableFrom.isNotEmpty()) {
                                try {
                                    val parts = availableFrom.split("-")
                                    if (parts.size == 3) {
                                        val cal = Calendar.getInstance()
                                        cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                                        cal.set(Calendar.MILLISECOND, 0)
                                        cal.time
                                    } else null
                                } catch (e: Exception) {
                                    null
                                }
                            } else null

                            if (selectedDate != null && selectedDate.before(todayDate)) {
                                // Show warning dialog for past date
                                showPastDateWarning = true
                            } else {
                                // Create property normally (will be active)
                                scope.launch {
                                    isSubmitting = true

                                    try {
                                        Log.d("LandlordAddProperty", "Creating property for landlord: $landlordId")

                                        val propertyId = adminRepository.createProperty(
                                            landlordId = landlordId!!,
                                            title = title.trim(),
                                            description = description.trim(),
                                            propertyType = propertyType,
                                            address = address.trim(),
                                            city = city.trim(),
                                            postalCode = postalCode.trim(),
                                            pricePerMonth = pricePerMonth.toDouble(),
                                            bedrooms = bedrooms.toInt(),
                                            bathrooms = bathrooms.toInt(),
                                            totalCapacity = totalCapacity.toInt(),
                                            availableFrom = availableFrom.ifEmpty {
                                                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                                formatter.format(Date())
                                            }
                                        )

                                        if (propertyId > 0) {
                                            // Add amenities
                                            if (selectedAmenities.isNotEmpty()) {
                                                adminRepository.addPropertyAmenities(propertyId, selectedAmenities.toList())
                                            }

                                            // Upload images
                                            if (selectedImageUris.isNotEmpty()) {
                                                isUploadingImages = true
                                                val imageRepository = PropertyImageRepository(context)
                                                imageRepository.uploadMultipleImages(propertyId, selectedImageUris)
                                                isUploadingImages = false
                                            }

                                            snackbarHostState.showSnackbar("Property created successfully!")
                                            onPropertyAdded()
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to create property")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("LandlordAddProperty", "Error creating property", e)
                                        snackbarHostState.showSnackbar("Error: ${e.message}")
                                    }

                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = isFormValid && !isSubmitting && !isUploadingImages,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when {
                            isUploadingImages -> {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading images...")
                            }
                            isSubmitting -> {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Creating property...")
                            }
                            else -> Text("Create Property")
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        LandlordDatePickerDialog(
            onDateSelected = { year, month, day ->
                availableFrom = String.format("%04d-%02d-%02d", year, month, day)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Past Date Warning Dialog
    if (showPastDateWarning) {
        AlertDialog(
            onDismissRequest = { showPastDateWarning = false },
            title = { Text("Past Availability Date") },
            text = {
                Text(
                    "The availability date you selected ($availableFrom) is in the past. " +
                            "The property will be created as INACTIVE. You can activate it later when ready.\n\n" +
                            "Do you want to continue?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Create property as inactive
                        scope.launch {
                            isSubmitting = true

                            try {
                                Log.d("LandlordAddProperty", "Creating inactive property for landlord: $landlordId")

                                val propertyId = adminRepository.createProperty(
                                    landlordId = landlordId!!,
                                    title = title.trim(),
                                    description = description.trim(),
                                    propertyType = propertyType,
                                    address = address.trim(),
                                    city = city.trim(),
                                    postalCode = postalCode.trim(),
                                    pricePerMonth = pricePerMonth.toDouble(),
                                    bedrooms = bedrooms.toInt(),
                                    bathrooms = bathrooms.toInt(),
                                    totalCapacity = totalCapacity.toInt(),
                                    availableFrom = availableFrom
                                )

                                if (propertyId > 0) {
                                    // Immediately set property as inactive
                                    val propertyRepository = PropertyRepository()
                                    propertyRepository.updatePropertyStatus(propertyId, false)

                                    // Add amenities
                                    if (selectedAmenities.isNotEmpty()) {
                                        adminRepository.addPropertyAmenities(propertyId, selectedAmenities.toList())
                                    }

                                    // Upload images
                                    if (selectedImageUris.isNotEmpty()) {
                                        isUploadingImages = true
                                        val imageRepository = PropertyImageRepository(context)
                                        imageRepository.uploadMultipleImages(propertyId, selectedImageUris)
                                        isUploadingImages = false
                                    }

                                    snackbarHostState.showSnackbar("Property created as INACTIVE due to past availability date")
                                    onPropertyAdded()
                                } else {
                                    snackbarHostState.showSnackbar("Failed to create property")
                                }
                            } catch (e: Exception) {
                                Log.e("LandlordAddProperty", "Error creating property", e)
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }

                            isSubmitting = false
                            showPastDateWarning = false
                        }
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPastDateWarning = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Date Picker Dialog Component
@Composable
fun LandlordDatePickerDialog(
    onDateSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH) + 1) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Year selector
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.Filled.ArrowBack, "Previous year")
                    }
                    Text(
                        selectedYear.toString(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.Filled.ArrowForward, "Next year")
                    }
                }

                // Month selector
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            selectedMonth = if (selectedMonth > 1) selectedMonth - 1 else 12
                            if (selectedMonth == 12) selectedYear--
                        }
                    ) {
                        Icon(Icons.Filled.ArrowBack, "Previous month")
                    }
                    Text(
                        DateFormatSymbols().months[selectedMonth - 1],
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = {
                            selectedMonth = if (selectedMonth < 12) selectedMonth + 1 else 1
                            if (selectedMonth == 1) selectedYear++
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next month")
                    }
                }

                // Day selector
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val maxDay = getMaxDayOfMonthForAdd(selectedYear, selectedMonth)
                    IconButton(
                        onClick = {
                            selectedDay = if (selectedDay > 1) selectedDay - 1 else maxDay
                        }
                    ) {
                        Icon(Icons.Filled.ArrowBack, "Previous day")
                    }
                    Text(
                        selectedDay.toString(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = {
                            selectedDay = if (selectedDay < maxDay) selectedDay + 1 else 1
                        }
                    ) {
                        Icon(Icons.Filled.ArrowForward, "Next day")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(selectedYear, selectedMonth, selectedDay)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getMaxDayOfMonthForAdd(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }
}