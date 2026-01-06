package com.finalapp.accommodationapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.finalapp.accommodationapp.navigation.AppNavigation
import com.finalapp.accommodationapp.ui.theme.AccommodationAppTheme
// import dagger.hilt.android.AndroidEntryPoint

// @AndroidEntryPoint // Temporarily disabled - will re-enable when Hilt plugin is fixed
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AccommodationAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}