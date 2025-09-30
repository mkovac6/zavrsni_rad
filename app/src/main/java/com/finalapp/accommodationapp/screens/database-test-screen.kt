package com.finalapp.accommodationapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finalapp.accommodationapp.data.SupabaseClient
import com.finalapp.accommodationapp.data.SupabaseConfig
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

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
            text = "Supabase Connection Test",
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

                Spacer(modifier = Modifier.height(8.dp))

                Text("Supabase URL:", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = SupabaseConfig.SUPABASE_URL,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    connectionStatus = "Testing connection..."

                    try {
                        // Test connection by fetching universities
                        val result = SupabaseClient.client
                            .from("universities")
                            .select {
                                filter {
                                    eq("is_active", true)
                                }
                            }
                            .decodeList<TestUniversityDto>()

                        universities = result.map { it.name }
                        connectionStatus = "Connected successfully!"

                    } catch (e: Exception) {
                        connectionStatus = "Connection failed: ${e.message?.take(100)}"
                        universities = emptyList()
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
                Text("Test Supabase Connection")
            }
        }

        if (universities.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Universities in Supabase (${universities.size}):",
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

// Simple DTO for testing
@Serializable
private data class TestUniversityDto(
    val university_id: Int,
    val name: String,
    val is_active: Boolean? = true
)