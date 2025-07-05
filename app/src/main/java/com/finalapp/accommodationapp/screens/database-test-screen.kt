package com.finalapp.accommodationapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finalapp.accommodationapp.data.DatabaseConnection
import kotlinx.coroutines.launch

@Composable
fun DatabaseTestScreen() {
    var connectionStatus by remember { mutableStateOf("Not tested") }
    var universities by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Database Connection Test",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Connection Status:", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = connectionStatus,
                    color = when (connectionStatus) {
                        "Connected successfully!" -> MaterialTheme.colorScheme.primary
                        "Connection failed" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    connectionStatus = "Testing connection..."
                    
                    val isConnected = DatabaseConnection.testConnection()
                    connectionStatus = if (isConnected) {
                        "Connected successfully!"
                    } else {
                        "Connection failed"
                    }
                    
                    if (isConnected) {
                        universities = DatabaseConnection.getUniversities()
                    }
                    
                    isLoading = false
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Test Database Connection")
            }
        }
        
        if (universities.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Universities in Database:",
                style = MaterialTheme.typography.titleMedium
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(universities) { university ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = university,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}