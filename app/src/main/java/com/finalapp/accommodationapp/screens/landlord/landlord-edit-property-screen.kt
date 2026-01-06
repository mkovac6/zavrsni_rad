package com.finalapp.accommodationapp.screens.landlord

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.finalapp.accommodationapp.data.repository.admin.AdminRepository
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import com.finalapp.accommodationapp.screens.components.MultiImagePicker
import com.finalapp.accommodationapp.ui.viewmodels.landlord.LandlordEditPropertyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordEditPropertyScreen(
    propertyId: Int,
    onNavigateBack: () -> Unit,
    onPropertyUpdated: () -> Unit,
    onPropertyDeleted: () -> Unit,
    viewModel: LandlordEditPropertyViewModel = viewModel {
        LandlordEditPropertyViewModel(
            propertyRepository = PropertyRepository(),
            adminRepository = AdminRepository()
        )
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Load uiState.property data
    LaunchedEffect(propertyId) {
        viewModel.loadProperty(propertyId)
        viewModel.loadPropertyImages(context, propertyId)
    }

    // Handle navigation after successful update
    LaunchedEffect(uiState.propertyUpdated) {
        if (uiState.propertyUpdated) {
            onPropertyUpdated()
            viewModel.navigationHandled()
        }
    }

    // Handle navigation after successful deletion
    LaunchedEffect(uiState.propertyDeleted) {
        if (uiState.propertyDeleted) {
            onPropertyDeleted()
            viewModel.navigationHandled()
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
                        onClick = { viewModel.showDeleteDialog() },
                        enabled = !uiState.isUpdating
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.property == null) {
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
                if (uiState.errorMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                if (uiState.successMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = uiState.successMessage,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Existing Images Section
                Text(
                    "Current Images (${uiState.existingImages.size})",
                    style = MaterialTheme.typography.titleMedium
                )

                if (uiState.existingImages.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.existingImages) { image ->
                            Card(
                                modifier = Modifier.size(120.dp)
                            ) {
                                Box {
                                    AsyncImage(
                                        model = image.imageUrl + "?width=400&quality=75",
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Delete button
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteExistingImage(context, image.imageId)
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    // Primary badge
                                    if (image.isPrimary) {
                                        Card(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(4.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Text(
                                                "Primary",
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    } else {
                                        // Set as primary button
                                        TextButton(
                                            onClick = {
                                                viewModel.setPrimaryImage(context, propertyId, image.imageId)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(4.dp)
                                        ) {
                                            Text(
                                                "Set Primary",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "No images yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Add new images section
                Text(
                    "Add New Images",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                val totalImages = uiState.existingImages.size + uiState.formState.newImageUris.size
                val canAddMore = totalImages < 10

                MultiImagePicker(
                    selectedImages = uiState.formState.newImageUris,
                    onImagesSelected = { uris ->
                        val availableSlots = 10 - uiState.existingImages.size
                        viewModel.updateNewImageUris(uris.take(availableSlots))
                        if (uris.size > availableSlots) {
                            viewModel.clearMessages()
                            // Note: Cannot set error message directly, would need to add method to ViewModel
                        }
                    },
                    maxImages = 10 - uiState.existingImages.size,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!canAddMore) {
                    Text(
                        "Maximum 10 images reached",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Property Type
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = uiState.formState.propertyType,
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
                                    viewModel.updatePropertyType(type)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Title
                OutlinedTextField(
                    value = uiState.formState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("Property Title*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.formState.title.isEmpty()
                )

                // Description
                OutlinedTextField(
                    value = uiState.formState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                // Location fields
                OutlinedTextField(
                    value = uiState.formState.address,
                    onValueChange = { viewModel.updateAddress(it) },
                    label = { Text("Address*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.formState.address.isEmpty()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.formState.city,
                        onValueChange = { viewModel.updateCity(it) },
                        label = { Text("City*") },
                        modifier = Modifier.weight(1f),
                        isError = uiState.formState.city.isEmpty()
                    )

                    OutlinedTextField(
                        value = uiState.formState.postalCode,
                        onValueChange = { viewModel.updatePostalCode(it) },
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
                        value = uiState.formState.bedrooms,
                        onValueChange = { viewModel.updateBedrooms(it) },
                        label = { Text("Bedrooms*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        isError = uiState.formState.bedrooms.isEmpty()
                    )

                    OutlinedTextField(
                        value = uiState.formState.bathrooms,
                        onValueChange = { viewModel.updateBathrooms(it) },
                        label = { Text("Bathrooms*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        isError = uiState.formState.bathrooms.isEmpty()
                    )

                    OutlinedTextField(
                        value = uiState.formState.totalCapacity,
                        onValueChange = { viewModel.updateTotalCapacity(it) },
                        label = { Text("Capacity*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        isError = uiState.formState.totalCapacity.isEmpty()
                    )
                }

                // Price
                OutlinedTextField(
                    value = uiState.formState.pricePerMonth,
                    onValueChange = { viewModel.updatePricePerMonth(it) },
                    label = { Text("Price per Month (€)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.formState.pricePerMonth.isEmpty()
                )

                // Availability dates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.formState.availableFrom,
                        onValueChange = { },
                        label = { Text("Available From*") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                viewModel.showDatePicker("from")
                            }) {
                                Icon(Icons.Filled.DateRange, "Select date")
                            }
                        },
                        isError = uiState.formState.availableFrom.isEmpty()
                    )

                    OutlinedTextField(
                        value = uiState.formState.availableTo,
                        onValueChange = { },
                        label = { Text("Available To") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                viewModel.showDatePicker("to")
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

                uiState.amenities.forEach { amenity ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.formState.selectedAmenities.contains(amenity.amenityId),
                            onCheckedChange = { isChecked ->
                                val newAmenities = if (isChecked) {
                                    uiState.formState.selectedAmenities + amenity.amenityId
                                } else {
                                    uiState.formState.selectedAmenities - amenity.amenityId
                                }
                                viewModel.updateSelectedAmenities(newAmenities)
                            }
                        )
                        Text(amenity.name)
                    }
                }

                // Update button
                Button(
                    onClick = {
                        viewModel.updateProperty(context, propertyId, onPropertyUpdated)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isUpdating && !uiState.isUploadingImages
                ) {
                    when {
                        uiState.isUploadingImages -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uploading images...")
                        }
                        uiState.isUpdating -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Updating...")
                        }
                        else -> Text("Update Property")
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text("Delete Property") },
            text = { Text("Are you sure you want to delete this property? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProperty(propertyId, onPropertyDeleted)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Date picker dialog
    if (uiState.showDatePicker) {
        EditPropertyDatePickerDialog(
            onDateSelected = { year, month, day ->
                val formattedDate = String.format("%04d-%02d-%02d", year, month, day)
                if (uiState.datePickerTarget == "from") {
                    viewModel.updateAvailableFrom(formattedDate)
                } else {
                    viewModel.updateAvailableTo(formattedDate)
                }
                viewModel.hideDatePicker()
            },
            onDismiss = { viewModel.hideDatePicker() }
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

fun getMaxDayOfMonthEdit(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }
}
