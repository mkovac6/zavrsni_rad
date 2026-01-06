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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finalapp.accommodationapp.data.repository.admin.AdminRepository
import com.finalapp.accommodationapp.data.repository.landlord.LandlordRepository
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import com.finalapp.accommodationapp.data.repository.PropertyImageRepository
import com.finalapp.accommodationapp.data.model.admin.Amenity
import com.finalapp.accommodationapp.data.UserSession
import com.finalapp.accommodationapp.screens.components.MultiImagePicker
import com.finalapp.accommodationapp.ui.viewmodels.landlord.LandlordAddPropertyViewModel
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordAddPropertyScreen(
    onNavigateBack: () -> Unit,
    onPropertyAdded: () -> Unit,
    viewModel: LandlordAddPropertyViewModel = viewModel {
        LandlordAddPropertyViewModel(
            adminRepository = AdminRepository(),
            landlordRepository = LandlordRepository()
        )
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Load landlord info and uiState.amenities
    LaunchedEffect(Unit) {
        val userId = UserSession.currentUser?.userId
        if (userId != null) {
            viewModel.loadLandlordData(userId)
        }
    }

    // Handle snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.snackbarShown()
        }
    }

    // Handle navigation after successful property creation
    LaunchedEffect(uiState.propertyCreated) {
        if (uiState.propertyCreated) {
            onPropertyAdded()
        }
    }

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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.landlordId == null) {
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
                                text = "Adding as: $uiState.landlordName",
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
                        value = uiState.formState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        label = { Text("Property Title *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.formState.description,
                        onValueChange = { viewModel.updateDescription(it) },
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
                                selected = uiState.formState.propertyType == type,
                                onClick = { viewModel.updatePropertyType(type) },
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
                        value = uiState.formState.address,
                        onValueChange = { viewModel.updateAddress(it) },
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
                            value = uiState.formState.city,
                            onValueChange = { viewModel.updateCity(it) },
                            label = { Text("City *") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.formState.postalCode,
                            onValueChange = { viewModel.updatePostalCode(it) },
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
                        value = uiState.formState.pricePerMonth,
                        onValueChange = { viewModel.updatePricePerMonth(it) },
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
                            value = uiState.formState.bedrooms,
                            onValueChange = { viewModel.updateBedrooms(it) },
                            label = { Text("Bedrooms *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.formState.bathrooms,
                            onValueChange = { viewModel.updateBathrooms(it) },
                            label = { Text("Bathrooms *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.formState.totalCapacity,
                            onValueChange = { viewModel.updateTotalCapacity(it) },
                            label = { Text("Capacity *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = uiState.formState.availableFrom,
                        onValueChange = { },
                        label = { Text("Available From") },
                        leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.showDatePicker() }) {
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

                items(uiState.amenities.chunked(2)) { amenityPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        amenityPair.forEach { amenity ->
                            FilterChip(
                                selected = uiState.formState.selectedAmenities.contains(amenity.amenityId),
                                onClick = {
                                    val newAmenities = if (uiState.formState.selectedAmenities.contains(amenity.amenityId)) {
                                        uiState.formState.selectedAmenities - amenity.amenityId
                                    } else {
                                        uiState.formState.selectedAmenities + amenity.amenityId
                                    }
                                    viewModel.updateSelectedAmenities(newAmenities)
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
                        selectedImages = uiState.formState.selectedImageUris,
                        onImagesSelected = { viewModel.updateSelectedImageUris(it) },
                        maxImages = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Submit Button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.submitProperty(context, onPropertyAdded)
                        },
                        enabled = viewModel.isFormValid && !uiState.isSubmitting && !uiState.isUploadingImages,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when {
                            uiState.isUploadingImages -> {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading images...")
                            }
                            uiState.isSubmitting -> {
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
    if (uiState.showDatePicker) {
        LandlordDatePickerDialog(
            onDateSelected = { year, month, day ->
                viewModel.updateAvailableFrom(String.format("%04d-%02d-%02d", year, month, day))
                viewModel.hideDatePicker()
            },
            onDismiss = { viewModel.hideDatePicker() }
        )
    }

    // Past Date Warning Dialog
    if (uiState.showPastDateWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPastDateWarning() },
            title = { Text("Past Availability Date") },
            text = {
                Text(
                    "The availability date you selected ($uiState.formState.availableFrom) is in the past. " +
                            "The property will be created as INACTIVE. You can activate it later when ready.\n\n" +
                            "Do you want to continue?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.submitPropertyInactive(context, onPropertyAdded)
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissPastDateWarning() }
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