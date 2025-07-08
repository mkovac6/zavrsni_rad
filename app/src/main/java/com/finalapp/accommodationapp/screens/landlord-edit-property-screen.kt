package com.finalapp.accommodationapp.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finalapp.accommodationapp.data.repository.AdminRepository
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordEditPropertyScreen(
    propertyId: Int,
    onNavigateBack: () -> Unit,
    onPropertyUpdated: () -> Unit,
    onPropertyDeleted: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val propertyRepository = remember { PropertyRepository() }
    val adminRepository = remember { AdminRepository() }

    // States
    var isLoading by remember { mutableStateOf(true) }
    var property by remember { mutableStateOf<com.finalapp.accommodationapp.data.model.Property?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var propertyType by remember { mutableStateOf("apartment") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var pricePerMonth by remember { mutableStateOf("") }
    var bedrooms by remember { mutableStateOf("") }
    var bathrooms by remember { mutableStateOf("") }
    var totalCapacity by remember { mutableStateOf("") }
    var availableFrom by remember { mutableStateOf("") }
    var availableTo by remember { mutableStateOf("") }

    var amenities by remember { mutableStateOf(listOf<com.finalapp.accommodationapp.data.model.Amenity>()) }
    var selectedAmenities by remember { mutableStateOf(setOf<Int>()) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerTarget by remember { mutableStateOf("from") }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Load property data
    LaunchedEffect(propertyId) {
        try {
            isLoading = true
            Log.d("LandlordEditProperty", "Loading property with ID: $propertyId")

            // Load property
            val loadedProperty = propertyRepository.getPropertyById(propertyId)
            Log.d("LandlordEditProperty", "Loaded property: $loadedProperty")

            if (loadedProperty != null) {
                property = loadedProperty
                title = loadedProperty.title
                description = loadedProperty.description
                propertyType = loadedProperty.propertyType
                address = loadedProperty.address
                city = loadedProperty.city
                postalCode = loadedProperty.postalCode
                pricePerMonth = loadedProperty.pricePerMonth.toString()
                bedrooms = loadedProperty.bedrooms.toString()
                bathrooms = loadedProperty.bathrooms.toString()
                totalCapacity = loadedProperty.totalCapacity.toString()
                availableFrom = dateFormat.format(loadedProperty.availableFrom)
                availableTo = loadedProperty.availableTo?.let { dateFormat.format(it) } ?: ""
            } else {
                Log.e("LandlordEditProperty", "Property not found for ID: $propertyId")
                errorMessage = "Property not found"
            }

            // Load amenities
            try {
                amenities = adminRepository.getAllAmenities()
                Log.d("LandlordEditProperty", "Loaded ${amenities.size} amenities")
            } catch (e: Exception) {
                Log.e("LandlordEditProperty", "Error loading amenities", e)
            }

            // Load selected amenities
            try {
                val propertyAmenityIds = propertyRepository.getPropertyAmenities(propertyId)
                selectedAmenities = propertyAmenityIds.toSet()
                Log.d("LandlordEditProperty", "Loaded ${selectedAmenities.size} selected amenities")
            } catch (e: Exception) {
                Log.e("LandlordEditProperty", "Error loading property amenities", e)
            }

            isLoading = false
        } catch (e: Exception) {
            Log.e("LandlordEditProperty", "Error in LaunchedEffect", e)
            errorMessage = "Error loading property: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Property") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = !isUpdating
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            "Delete Property",
                            tint = MaterialTheme.colorScheme.error
                        )
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
        } else if (property == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Property not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error/Success messages
                if (errorMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                if (successMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = successMessage,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Property Type
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = propertyType,
                        onValueChange = { },
                        label = { Text("Property Type") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("apartment", "house", "room", "studio", "shared").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.capitalize()) },
                                onClick = {
                                    propertyType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Property Title*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = title.isEmpty()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                // Location fields
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = address.isEmpty()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City*") },
                        modifier = Modifier.weight(1f),
                        isError = city.isEmpty()
                    )

                    OutlinedTextField(
                        value = postalCode,
                        onValueChange = { postalCode = it },
                        label = { Text("Postal Code") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Property details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = bedrooms,
                        onValueChange = { bedrooms = it },
                        label = { Text("Bedrooms*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        isError = bedrooms.isEmpty()
                    )

                    OutlinedTextField(
                        value = bathrooms,
                        onValueChange = { bathrooms = it },
                        label = { Text("Bathrooms*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        isError = bathrooms.isEmpty()
                    )

                    OutlinedTextField(
                        value = totalCapacity,
                        onValueChange = { totalCapacity = it },
                        label = { Text("Capacity*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        isError = totalCapacity.isEmpty()
                    )
                }

                // Price
                OutlinedTextField(
                    value = pricePerMonth,
                    onValueChange = { pricePerMonth = it },
                    label = { Text("Price per Month (€)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = pricePerMonth.isEmpty()
                )

                // Availability dates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = availableFrom,
                        onValueChange = { },
                        label = { Text("Available From*") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                datePickerTarget = "from"
                                showDatePicker = true
                            }) {
                                Icon(Icons.Filled.DateRange, "Select date")
                            }
                        },
                        isError = availableFrom.isEmpty()
                    )

                    OutlinedTextField(
                        value = availableTo,
                        onValueChange = { },
                        label = { Text("Available To") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                datePickerTarget = "to"
                                showDatePicker = true
                            }) {
                                Icon(Icons.Filled.DateRange, "Select date")
                            }
                        }
                    )
                }

                // Amenities
                Text(
                    "Amenities",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                amenities.forEach { amenity ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedAmenities.contains(amenity.amenityId),
                            onCheckedChange = { isChecked ->
                                selectedAmenities = if (isChecked) {
                                    selectedAmenities + amenity.amenityId
                                } else {
                                    selectedAmenities - amenity.amenityId
                                }
                            }
                        )
                        Text(amenity.name)
                    }
                }

                // Update button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            errorMessage = ""
                            successMessage = ""

                            // Validate
                            when {
                                title.isEmpty() -> errorMessage = "Title is required"
                                address.isEmpty() -> errorMessage = "Address is required"
                                city.isEmpty() -> errorMessage = "City is required"
                                bedrooms.toIntOrNull() == null -> errorMessage = "Valid number of bedrooms required"
                                bathrooms.toIntOrNull() == null -> errorMessage = "Valid number of bathrooms required"
                                totalCapacity.toIntOrNull() == null -> errorMessage = "Valid capacity required"
                                pricePerMonth.toDoubleOrNull() == null -> errorMessage = "Valid price required"
                                availableFrom.isEmpty() -> errorMessage = "Available from date is required"
                                else -> {
                                    isUpdating = true

                                    val success = propertyRepository.updateProperty(
                                        propertyId = propertyId,
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
                                        availableFrom = availableFrom,
                                        availableTo = availableTo.ifEmpty { null }
                                    )

                                    if (success) {
                                        // Update amenities
                                        propertyRepository.updatePropertyAmenities(
                                            propertyId,
                                            selectedAmenities.toList()
                                        )

                                        successMessage = "Property updated successfully!"
                                        kotlinx.coroutines.delay(1000)
                                        onPropertyUpdated()
                                    } else {
                                        errorMessage = "Failed to update property"
                                    }

                                    isUpdating = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Update Property")
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Property") },
            text = { Text("Are you sure you want to delete this property? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            showDeleteDialog = false
                            isUpdating = true

                            val success = propertyRepository.deleteProperty(propertyId)

                            if (success) {
                                onPropertyDeleted()
                            } else {
                                errorMessage = "Failed to delete property"
                                isUpdating = false
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Date picker dialog
    if (showDatePicker) {
        EditPropertyDatePickerDialog(
            onDateSelected = { year, month, day ->
                val formattedDate = String.format("%04d-%02d-%02d", year, month, day)
                if (datePickerTarget == "from") {
                    availableFrom = formattedDate
                } else {
                    availableTo = formattedDate
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun EditPropertyDatePickerDialog(
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        java.text.DateFormatSymbols().months[selectedMonth - 1],
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    IconButton(
                        onClick = {
                            selectedMonth = if (selectedMonth < 12) selectedMonth + 1 else 1
                            if (selectedMonth == 1) selectedYear++
                        }
                    ) {
                        Icon(Icons.Filled.ArrowForward, "Next month")
                    }
                }

                // Day selector
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val maxDay = getMaxDayOfMonthEdit(selectedYear, selectedMonth)
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

fun getMaxDayOfMonthEdit(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }
}