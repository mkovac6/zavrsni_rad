package com.finalapp.accommodationapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UniversitySelectionScreen(
    onUniversitySelected: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedUniversity by remember { mutableStateOf("") }
    
    // Sample universities - later will come from database
    val universities = listOf(
        "University of Zagreb",
        "Zagreb School of Economics and Management",
        "University of Split",
        "University of Rijeka"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Select Your University",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Choose your university to see nearby accommodations",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(universities) { university ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectedUniversity = university }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(university)
                        if (selectedUniversity == university) {
                            Text("✓", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onUniversitySelected() },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedUniversity.isNotEmpty()
        ) {
            Text("Continue")
        }
    }
}