package com.finalapp.accommodationapp.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDialog(
    propertyId: Int,
    pricePerMonth: Double,
    availableFrom: Date?,
    availableTo: Date?,
    onDismiss: () -> Unit,
    onConfirm: (startDate: String, endDate: String, totalPrice: Double, message: String) -> Unit
) {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Use current date as minimum if availableFrom is null
    val minAvailableDate = availableFrom ?: Date()

    // Calculate total price based on selected dates
    val totalPrice = remember(startDate, endDate) {
        if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
            try {
                val start = dateFormat.parse(startDate)
                val end = dateFormat.parse(endDate)
                val days = TimeUnit.DAYS.convert(
                    end.time - start.time,
                    TimeUnit.MILLISECONDS
                ) + 1
                val dailyRate = pricePerMonth / 30
                days * dailyRate
            } catch (e: Exception) {
                0.0
            }
        } else {
            0.0
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Request Booking",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Date selection
                OutlinedTextField(
                    value = if (startDate.isNotEmpty()) {
                        try {
                            displayDateFormat.format(dateFormat.parse(startDate))
                        } catch (e: Exception) {
                            startDate
                        }
                    } else "",
                    onValueChange = { },
                    label = { Text("Check-in Date") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showStartDatePicker = true }) {
                            Icon(Icons.Filled.DateRange, "Select date")
                        }
                    },
                    isError = errorMessage.isNotEmpty() && startDate.isEmpty()
                )

                OutlinedTextField(
                    value = if (endDate.isNotEmpty()) {
                        try {
                            displayDateFormat.format(dateFormat.parse(endDate))
                        } catch (e: Exception) {
                            endDate
                        }
                    } else "",
                    onValueChange = { },
                    label = { Text("Check-out Date") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showEndDatePicker = true }) {
                            Icon(Icons.Filled.DateRange, "Select date")
                        }
                    },
                    isError = errorMessage.isNotEmpty() && endDate.isEmpty()
                )

                // Message to landlord
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message to Landlord (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Introduce yourself and explain why you're interested...") }
                )

                // Total price display
                if (totalPrice > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Price",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "€${String.format("%.2f", totalPrice)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Error message
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            errorMessage = ""

                            // Validate dates
                            when {
                                startDate.isEmpty() -> errorMessage = "Please select check-in date"
                                endDate.isEmpty() -> errorMessage = "Please select check-out date"
                                else -> {
                                    try {
                                        val start = dateFormat.parse(startDate)
                                        val end = dateFormat.parse(endDate)

                                        when {
                                            start >= end -> errorMessage =
                                                "Check-out must be after check-in"

                                            start < minAvailableDate -> errorMessage =
                                                "Property not available before ${
                                                    displayDateFormat.format(minAvailableDate)
                                                }"

                                            availableTo != null && end > availableTo -> errorMessage =
                                                "Property not available after ${
                                                    displayDateFormat.format(availableTo)
                                                }"

                                            else -> {
                                                onConfirm(startDate, endDate, totalPrice, message)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Invalid date format"
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Send Request")
                    }
                }
            }
        }
    }

    // Date pickers
    if (showStartDatePicker) {
        BookingDatePickerDialog(
            initialDate = if (startDate.isNotEmpty()) startDate else dateFormat.format(Date()),
            onDateSelected = { year, month, day ->
                startDate = String.format("%04d-%02d-%02d", year, month, day)
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        BookingDatePickerDialog(
            initialDate = if (endDate.isNotEmpty()) endDate else dateFormat.format(Date()),
            onDateSelected = { year, month, day ->
                endDate = String.format("%04d-%02d-%02d", year, month, day)
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

@Composable
fun BookingDatePickerDialog(
    initialDate: String,
    onDateSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()
    try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        calendar.time = dateFormat.parse(initialDate) ?: Date()
    } catch (e: Exception) {
        calendar.time = Date()
    }

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
                    val maxDay = getMaxDayOfMonthForBooking(selectedYear, selectedMonth)
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

fun getMaxDayOfMonthForBooking(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }
}