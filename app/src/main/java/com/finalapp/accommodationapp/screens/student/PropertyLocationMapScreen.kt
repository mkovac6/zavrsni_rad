package com.finalapp.accommodationapp.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import com.finalapp.accommodationapp.data.repository.UniversityRepository
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import com.finalapp.accommodationapp.data.model.Property
import com.finalapp.accommodationapp.data.model.University
import com.finalapp.accommodationapp.data.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyLocationMapScreen(
    propertyId: Int,
    onNavigateBack: () -> Unit
) {
    val propertyRepository = remember { PropertyRepository() }
    val universityRepository = remember { UniversityRepository() }
    val studentRepository = remember { StudentRepository() }

    var property by remember { mutableStateOf<Property?>(null) }
    var university by remember { mutableStateOf<University?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(45.8150, 15.9819), // Default Zagreb
            13f
        )
    }

// Load property and student's university
    LaunchedEffect(propertyId) {
        scope.launch {
            isLoading = true

            // Load property first
            val loadedProperty = propertyRepository.getPropertyById(propertyId)
            property = loadedProperty

            if (loadedProperty != null) {
                // Load student's university
                val currentUser = UserSession.currentUser
                if (currentUser?.userType == "student") {
                    val studentProfile = studentRepository.getStudentProfile(currentUser.userId)
                    if (studentProfile != null) {
                        val loadedUniversity = universityRepository.getUniversityById(studentProfile.universityId)
                        university = loadedUniversity

                        // Calculate camera position to show both markers
                        loadedUniversity?.let { uni ->
                            if (uni.latitude != 0.0 && uni.longitude != 0.0) {
                                // Calculate center point between property and university
                                val centerLat = (loadedProperty.latitude + uni.latitude) / 2
                                val centerLng = (loadedProperty.longitude + uni.longitude) / 2

                                // Calculate appropriate zoom level based on distance
                                val distance = calculateDistance(
                                    loadedProperty.latitude,
                                    loadedProperty.longitude,
                                    uni.latitude,
                                    uni.longitude
                                )
                                val zoom = when {
                                    distance < 2f -> 14f
                                    distance < 5f -> 12f
                                    distance < 10f -> 11f
                                    else -> 10f
                                }

                                // Set camera position
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                    LatLng(centerLat, centerLng),
                                    zoom
                                )
                            } else {
                                // Just show property if no university coordinates
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                    LatLng(loadedProperty.latitude, loadedProperty.longitude),
                                    13f
                                )
                            }
                        } ?: run {
                            // Just show property if no university
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                LatLng(loadedProperty.latitude, loadedProperty.longitude),
                                13f
                            )
                        }
                    }
                } else {
                    // Not a student, just show property
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                        LatLng(loadedProperty.latitude, loadedProperty.longitude),
                        13f
                    )
                }
            }

            isLoading = false
        }
    }

// Calculate distance
    val distance = remember(property, university) {
        if (property != null && university != null) {
            if (university!!.latitude != 0.0 && university!!.longitude != 0.0) {
                calculateDistance(
                    university!!.latitude,
                    university!!.longitude,
                    property!!.latitude,
                    property!!.longitude
                )
            } else null
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Location", style = MaterialTheme.typography.titleMedium)
                        if (distance != null) {
                            Text(
                                "${String.format("%.1f", distance)} km from your university",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading || property == null) {
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
                        compassEnabled = true
                    )
                ) {
                    // Property marker (red)
                    Marker(
                        state = MarkerState(
                            position = LatLng(property!!.latitude, property!!.longitude)
                        ),
                        title = property!!.title,
                        snippet = "${property!!.address}, ${property!!.city}",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )

                    // University marker (blue)
                    university?.let { uni ->
                        if (uni.latitude != 0.0 && uni.longitude != 0.0) {
                            Marker(
                                state = MarkerState(
                                    position = LatLng(uni.latitude, uni.longitude)
                                ),
                                title = uni.name,
                                snippet = "Your University",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                            )
                            // Draw polyline between property and university
                            /*Polyline(
                                points = listOf(
                                    LatLng(property!!.latitude, property!!.longitude),
                                    LatLng(uni.latitude, uni.longitude)
                                ),
                                color = androidx.compose.ui.graphics.Color(0xFF2196F3),
                                width = 5f
                            )*/
                        }
                    }
                }

                // Info card at bottom
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Property info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = property!!.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${property!!.address}, ${property!!.city}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Distance info (if university exists)
                        val uni = university
                        val dist = distance
                        if (uni != null && dist != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            // Distance display
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Distance from your university",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${String.format("%.1f", dist)} km",
                                        style = MaterialTheme.typography.titleMedium,
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
}

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val R = 6371.0

    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)

    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

    return (R * c).toFloat()
}