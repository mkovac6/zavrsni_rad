package com.finalapp.accommodationapp.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.finalapp.accommodationapp.data.repository.admin.AdminRepository
import com.finalapp.accommodationapp.data.model.admin.LandlordWithUser
import com.finalapp.accommodationapp.data.model.admin.Amenity
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPropertyScreen(
    onNavigateBack: () -> Unit,
    onPropertyAdded: () -> Unit
) {
    val adminRepository = remember { AdminRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load data
    var landlords by remember { mutableStateOf<List<LandlordWithUser>>(emptyList()) }
    var amenities by remember { mutableStateOf<List<Amenity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Form states
    var selectedLandlordId by remember { mutableStateOf<Int?>(null) }
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

    var showLandlordDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Load landlords and amenities
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            landlords = adminRepository.getAllLandlords()
            amenities = adminRepository.getAllAmenities()
            isLoading = false
        }
    }

    // Validation
    val isFormValid = selectedLandlordId != null &&
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Landlord Selection
                item {
                    Text(
                        text = "Property Owner",
                        style = MaterialTheme.typography.titleMedium
                    )

                    ExposedDropdownMenuBox(
                        expanded = showLandlordDropdown,
                        onExpandedChange = { showLandlordDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = landlords.find { it.landlordId == selectedLandlordId }?.let {
                                "${it.firstName} ${it.lastName} - ${it.email}"
                            } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Landlord *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLandlordDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = showLandlordDropdown,
                            onDismissRequest = { showLandlordDropdown = false }
                        ) {
                            landlords.forEach { landlord ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${landlord.firstName} ${landlord.lastName}")
                                            Text(
                                                landlord.email,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedLandlordId = landlord.landlordId
                                        showLandlordDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Basic Information
                item {
                    Divider()
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
                        onValueChange = { }, // Make it read-only
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

                // Submit Button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isSubmitting = true

                                try {
                                    // Log the data being sent
                                    Log.d("AddPropertyScreen", "Creating property with:")
                                    Log.d("AddPropertyScreen", "Landlord ID: $selectedLandlordId")
                                    Log.d("AddPropertyScreen", "Title: $title")
                                    Log.d("AddPropertyScreen", "Price: $pricePerMonth")
                                    Log.d("AddPropertyScreen", "Available from: $availableFrom")

                                    val propertyId = adminRepository.createProperty(
                                        landlordId = selectedLandlordId!!,
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

                                    Log.d("AddPropertyScreen", "Property created with ID: $propertyId")

                                    if (propertyId > 0) {
                                        // Add amenities
                                        if (selectedAmenities.isNotEmpty()) {
                                            val amenitiesAdded = adminRepository.addPropertyAmenities(propertyId, selectedAmenities.toList())
                                            Log.d("AddPropertyScreen", "Amenities added: $amenitiesAdded")
                                        }
                                        snackbarHostState.showSnackbar("Property created successfully!")
                                        onPropertyAdded()
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to create property - check logs for details")
                                    }
                                } catch (e: Exception) {
                                    Log.e("AddPropertyScreen", "Error creating property", e)
                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                }

                                isSubmitting = false
                            }
                        },
                        enabled = isFormValid && !isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text("Create Property")
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { year, month, day ->
                availableFrom = String.format("%04d-%02d-%02d", year, month, day)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun DatePickerDialog(
    onDateSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedDay by remember { mutableStateOf(currentDay) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Year selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Year:")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (selectedYear > currentYear) selectedYear-- }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Previous year")
                        }
                        Text(
                            text = selectedYear.toString(),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(onClick = { if (selectedYear < currentYear + 5) selectedYear++ }) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Next year")
                        }
                    }
                }

                // Month selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Month:")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (selectedMonth > 1) selectedMonth-- }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Previous month")
                        }
                        Text(
                            text = getMonthName(selectedMonth),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(onClick = { if (selectedMonth < 12) selectedMonth++ }) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Next month")
                        }
                    }
                }

                // Day selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Day:")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (selectedDay > 1) selectedDay-- }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Previous day")
                        }
                        Text(
                            text = selectedDay.toString(),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(onClick = {
                            val maxDay = getMaxDayOfMonth(selectedYear, selectedMonth)
                            if (selectedDay < maxDay) selectedDay++
                        }) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Next day")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDateSelected(selectedYear, selectedMonth, selectedDay) }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> ""
    }
}

fun getMaxDayOfMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 31
    }
}