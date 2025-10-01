package com.finalapp.accommodationapp.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finalapp.accommodationapp.data.UserSession
import com.finalapp.accommodationapp.data.model.Property
import com.finalapp.accommodationapp.data.repository.student.FavoritesRepository
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFavoritesScreen(
    onNavigateBack: () -> Unit,
    onPropertyClick: (Int) -> Unit,
    onHomeClick: () -> Unit,
    onBookingsClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val favoritesRepository = remember { FavoritesRepository() }
    val studentRepository = remember { StudentRepository() }
    val snackbarHostState = remember { SnackbarHostState() }

    var favoriteProperties by remember { mutableStateOf<List<Property>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var studentId by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Function to load favorites
    fun loadFavorites() {
        coroutineScope.launch {
            try {
                isRefreshing = true
                // Get student ID
                val userId = UserSession.currentUser?.userId ?: 0
                val studentProfile = studentRepository.getStudentProfile(userId)

                if (studentProfile != null) {
                    studentId = studentProfile.studentId
                    favoriteProperties = favoritesRepository.getStudentFavorites(studentProfile.studentId)
                } else {
                    snackbarHostState.showSnackbar("Unable to load student profile")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error loading favorites: ${e.message}")
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    // Load favorite properties
    LaunchedEffect(Unit) {
        isLoading = true
        loadFavorites()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("My Favorites")
                        if (favoriteProperties.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge {
                                Text(favoriteProperties.size.toString())
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (favoriteProperties.isNotEmpty()) {
                        IconButton(
                            onClick = { loadFavorites() },
                            enabled = !isRefreshing
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                            }
                        }
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
                    icon = {
                        BadgedBox(
                            badge = {
                                if (favoriteProperties.isNotEmpty()) {
                                    Badge { Text(favoriteProperties.size.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Favorites")
                        }
                    },
                    label = { Text("Favorites") },
                    selected = true,
                    onClick = { }
                )
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
            favoriteProperties.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No favorites yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Properties you favorite will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onHomeClick) {
                            Icon(Icons.Filled.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse Properties")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = favoriteProperties,
                        key = { it.propertyId }
                    ) { property ->
                        FavoritePropertyCard(
                            property = property,
                            onClick = { onPropertyClick(property.propertyId) },
                            onRemoveFavorite = {
                                coroutineScope.launch {
                                    val removed = favoritesRepository.removeFromFavorites(
                                        studentId,
                                        property.propertyId
                                    )
                                    if (removed) {
                                        favoriteProperties = favoriteProperties.filter {
                                            it.propertyId != property.propertyId
                                        }
                                        snackbarHostState.showSnackbar(
                                            message = "Removed from favorites",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        ).let { result ->
                                            if (result == SnackbarResult.ActionPerformed) {
                                                // Undo the removal
                                                val added = favoritesRepository.addToFavorites(
                                                    studentId,
                                                    property.propertyId
                                                )
                                                if (added) {
                                                    loadFavorites() // Reload to get the property back
                                                }
                                            }
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to remove from favorites")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritePropertyCard(
    property: Property,
    onClick: () -> Unit,
    onRemoveFavorite: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    var showRemoveDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${property.address}, ${property.city}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                // Remove favorite button with long press for confirmation
                IconButton(
                    onClick = { showRemoveDialog = true }
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Remove from favorites",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Property type badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(property.propertyType.replaceFirstChar { it.uppercase() }) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                // Show availability status
                property.availableFrom?.let { availableDate ->
                    val isAvailable = availableDate.before(Date()) || availableDate == Date()
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(if (isAvailable) "Available Now" else "Coming Soon")
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isAvailable)
                                Color(0xFF4CAF50).copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Property details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FavouritesPropertyDetail(
                    icon = Icons.Filled.Person,
                    text = "${property.bedrooms} bed${if (property.bedrooms != 1) "s" else ""}"
                )
                FavouritesPropertyDetail(
                    icon = Icons.Filled.Person,
                    text = "${property.bathrooms} bath${if (property.bathrooms != 1) "s" else ""}"
                )
                FavouritesPropertyDetail(
                    icon = Icons.Filled.Person,
                    text = "${property.totalCapacity} ${if (property.totalCapacity != 1) "people" else "person"}"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price and availability
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "€${property.pricePerMonth.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "per month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                property.availableFrom?.let { date ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Available from",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormat.format(date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove from Favorites") },
            text = {
                Text("Are you sure you want to remove \"${property.title}\" from your favorites?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        onRemoveFavorite()
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            },
            icon = {
                Icon(
                    Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}

@Composable
fun FavouritesPropertyDetail(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}