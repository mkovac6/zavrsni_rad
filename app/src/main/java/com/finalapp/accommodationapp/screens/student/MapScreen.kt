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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import com.finalapp.accommodationapp.data.repository.UniversityRepository
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import com.finalapp.accommodationapp.data.model.Property
import com.finalapp.accommodationapp.data.model.University
import com.finalapp.accommodationapp.data.UserSession
import kotlin.math.*

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
    val universityRepository = remember { UniversityRepository() }
    val studentRepository = remember { StudentRepository() }

    var properties by remember { mutableStateOf<List<Property>>(emptyList()) }
    var allProperties by remember { mutableStateOf<List<Property>>(emptyList()) }
    var university by remember { mutableStateOf<University?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedProperty by remember { mutableStateOf<Property?>(null) }
    var selectedDistance by remember { mutableStateOf<Float?>(null) }
    var showUniversityMarker by remember { mutableStateOf(true) }
    var showFilters by remember { mutableStateOf(false) }


    val scope = rememberCoroutineScope()

    val distanceOptions = listOf(2f, 5f, 10f, null)
    val distanceLabels = listOf("2 km", "5 km", "10 km", "All")

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(45.8150, 15.9819),
            10f
        )
    }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true

            val currentUser = UserSession.currentUser
            if (currentUser?.userType == "student") {
                val studentProfile = studentRepository.getStudentProfile(currentUser.userId)
                if (studentProfile != null) {
                    val loadedUniversity =
                        universityRepository.getUniversityById(studentProfile.universityId)
                    university = loadedUniversity

                    loadedUniversity?.let { uni ->
                        if (uni.latitude != 0.0 && uni.longitude != 0.0) {
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                LatLng(uni.latitude, uni.longitude),
                                12f
                            )
                        }
                    }
                }
            }

            allProperties = propertyRepository.getAllProperties()
            properties = allProperties

            isLoading = false
        }
    }

    LaunchedEffect(selectedDistance, allProperties, university) {
        val uni = university
        val distFilter = selectedDistance // Fix smart cast for selectedDistance

        properties = if (distFilter == null || uni == null) {
            allProperties
        } else {
            allProperties.filter { property ->
                val distance = calculateDistance(
                    uni.latitude,
                    uni.longitude,
                    property.latitude,
                    property.longitude
                )
                distance <= distFilter // Use local val
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Property Map",
                            style = MaterialTheme.typography.titleMedium
                        )
                        university?.let {
                            Text(
                                it.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {

                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "Filters",
                            tint = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (university != null) {
                        IconButton(onClick = { showUniversityMarker = !showUniversityMarker }) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = if (showUniversityMarker) "Hide University" else "Show University",
                                tint = if (showUniversityMarker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                    university?.let { uni ->
                        if (showUniversityMarker && uni.latitude != 0.0 && uni.longitude != 0.0) {
                            Marker(
                                state = MarkerState(
                                    position = LatLng(uni.latitude, uni.longitude)
                                ),
                                title = uni.name,
                                snippet = "Your University",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                            )
                        }
                    }

                    properties.forEach { property ->
                        val uni = university
                        val distance = if (uni != null) {
                            calculateDistance(
                                uni.latitude,
                                uni.longitude,
                                property.latitude,
                                property.longitude
                            )
                        } else null

                        Marker(
                            state = MarkerState(
                                position = LatLng(property.latitude, property.longitude)
                            ),
                            title = property.title,
                            snippet = buildString {
                                append("€${property.pricePerMonth.toInt()}/month")
                                if (distance != null && distance > 0) {
                                    append(" • ${String.format("%.1f", distance)} km from uni")
                                }
                            },
                            onClick = {
                                selectedProperty = property
                                false
                            },
                            onInfoWindowClick = {
                                onPropertyClick(property.propertyId)
                            }
                        )
                    }
                }

                if (university != null && showFilters) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .widthIn(max = 200.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Menu,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Filters",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            distanceOptions.forEachIndexed { index, distance ->
                                FilterChip(
                                    selected = selectedDistance == distance,
                                    onClick = { selectedDistance = distance },
                                    label = {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(distanceLabels[index])
                                            if (selectedDistance == distance) {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            HorizontalDivider()

                            Text(
                                text = "${properties.size} properties shown",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
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

                selectedProperty?.let { property ->
                    val uni = university
                    val distance = if (uni != null) {
                        calculateDistance(
                            uni.latitude,
                            uni.longitude,
                            property.latitude,
                            property.longitude
                        )
                    } else null

                    PropertyInfoCard(
                        property = property,
                        distanceFromUniversity = distance,
                        onViewDetails = { onPropertyClick(property.propertyId) },
                        onDismiss = { selectedProperty = null },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
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
    distanceFromUniversity: Float?,
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

                    if (distanceFromUniversity != null && distanceFromUniversity > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocationOn, // Changed from School
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${
                                    String.format(
                                        "%.1f",
                                        distanceFromUniversity
                                    )
                                } km from your university",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
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