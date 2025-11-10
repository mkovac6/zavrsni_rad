package com.finalapp.accommodationapp.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import com.finalapp.accommodationapp.data.model.Property
import com.finalapp.accommodationapp.data.UserSession
import com.google.android.gms.maps.CameraUpdateFactory
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onPropertyClick: (Int) -> Unit,
    onHomeClick: () -> Unit,
    onBookingsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit
) {
    val propertyRepository = remember { PropertyRepository() }
    var properties by remember { mutableStateOf<List<Property>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedProperty by remember { mutableStateOf<Property?>(null) }
    val scope = rememberCoroutineScope()

    // Camera position state - centering on Croatia (Zagreb as default)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(45.8150, 15.9819), // Zagreb coordinates
            7f // Zoom level to show most of Croatia
        )
    }

    // Load properties
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            properties = propertyRepository.getAllProperties()
            isLoading = false

            // If we have properties, adjust camera to show first property or center of all
            if (properties.isNotEmpty()) {
                // Calculate center point of all properties
                val avgLat = properties.map { it.latitude }.average()
                val avgLng = properties.map { it.longitude }.average()

                // Move camera to center of all properties
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(avgLat, avgLng),
                    10f // Zoom level - adjust as needed
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Property Map",
                        style = MaterialTheme.typography.titleMedium
                    )
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
                    selected = false,
                    onClick = onHomeClick
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = "Bookings") },
                    label = { Text("Bookings") },
                    selected = false,
                    onClick = onBookingsClick
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
                    label = { Text("Favorites") },
                    selected = false,
                    onClick = onFavoritesClick
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.LocationOn, contentDescription = "Map") },
                    label = { Text("Map") },
                    selected = true,
                    onClick = { }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        mapType = MapType.NORMAL,
                        isMyLocationEnabled = false
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        zoomGesturesEnabled = true,
                        scrollGesturesEnabled = true,
                        tiltGesturesEnabled = true,
                        rotationGesturesEnabled = true,
                        myLocationButtonEnabled = false,
                        compassEnabled = true
                    )
                ) {
                    // Add markers for each property
                    properties.forEach { property ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(property.latitude, property.longitude)
                            ),
                            title = property.title,
                            snippet = "€${property.pricePerMonth.toInt()}/month - ${property.city}",
                            onClick = {
                                selectedProperty = property
                                false // Return false to show the info window
                            },
                            onInfoWindowClick = {
                                // Navigate to property detail when info window is clicked
                                onPropertyClick(property.propertyId)
                            },
                            onInfoWindowLongClick = {
                                // Optional: Add functionality for long click
                            }
                        )
                    }
                }

                // Property info card overlay
                selectedProperty?.let { property ->
                    PropertyInfoCard(
                        property = property,
                        onViewDetails = { onPropertyClick(property.propertyId) },
                        onDismiss = { selectedProperty = null },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }

            // Map legend/info
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${properties.size} properties available",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyInfoCard(
    property: Property,
    onViewDetails: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${property.address}, ${property.city}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "€${property.pricePerMonth.toInt()}/month",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${property.totalCapacity} people",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${property.bedrooms} bed",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
                Button(onClick = onViewDetails) {
                    Text("View Details")
                }
            }
        }
    }
}