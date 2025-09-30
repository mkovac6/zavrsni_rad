package com.finalapp.accommodationapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.finalapp.accommodationapp.data.SupabaseClient
import com.finalapp.accommodationapp.data.SupabaseConfig
import com.finalapp.accommodationapp.navigation.AppNavigation
import com.finalapp.accommodationapp.ui.theme.AccommodationAppTheme
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Test with the corrected URL
        testSupabaseConnection()

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

    private fun testSupabaseConnection() {
        lifecycleScope.launch {
            try {
                Log.d("SupabaseTest", "Testing Supabase connection...")
                Log.d("SupabaseTest", "URL: ${SupabaseConfig.SUPABASE_URL}")

                // Simple test - fetch universities
                val universities = SupabaseClient.client
                    .from("universities")
                    .select()
                    .decodeList<UniversityTest>()

                Log.d("SupabaseTest", "✅ SUPABASE CONNECTED! Found ${universities.size} universities:")
                universities.forEach {
                    Log.d("SupabaseTest", "  - ${it.name} (${it.city ?: "no city"})")
                }

            } catch (e: Exception) {
                Log.e("SupabaseTest", "❌ FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}

// Correct DTO matching your database schema
@Serializable
data class UniversityTest(
    val university_id: Int,
    val name: String,
    val city: String? = null,
    val country: String? = null,
    val is_active: Boolean? = true,
    val created_at: String? = null
)