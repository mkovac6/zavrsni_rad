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
import com.finalapp.accommodationapp.navigation.AppNavigation
import com.finalapp.accommodationapp.ui.theme.AccommodationAppTheme
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Test the repositories
        testSupabaseRepositories()

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

    private fun testSupabaseRepositories() {
        lifecycleScope.launch {
            try {
                Log.d("RepoTest", "========== TESTING REPOSITORIES ==========")

                // Test 1: Direct query to users table
                Log.d("RepoTest", "\n--- Test Users Table ---")
                val users = SupabaseClient.client
                    .from("users")
                    .select()
                    .decodeList<TestUser>()
                Log.d("RepoTest", "✅ Found ${users.size} users")
                users.forEach {
                    Log.d("RepoTest", "  - ${it.email} (${it.user_type})")
                }

                // Test 2: Direct query to properties table
                Log.d("RepoTest", "\n--- Test Properties Table ---")
                val properties = SupabaseClient.client
                    .from("properties")
                    .select()
                    .decodeList<TestProperty>()
                Log.d("RepoTest", "✅ Found ${properties.size} properties")
                properties.forEach {
                    Log.d("RepoTest", "  - ${it.title} in ${it.city}")
                }

                // Test 3: Direct query to students table
                Log.d("RepoTest", "\n--- Test Students Table ---")
                val students = SupabaseClient.client
                    .from("students")
                    .select()
                    .decodeList<TestStudent>()
                Log.d("RepoTest", "✅ Found ${students.size} students")
                students.forEach {
                    Log.d("RepoTest", "  - ${it.first_name} ${it.last_name}")
                }

                // Test 4: Direct query to landlords table
                Log.d("RepoTest", "\n--- Test Landlords Table ---")
                val landlords = SupabaseClient.client
                    .from("landlords")
                    .select()
                    .decodeList<TestLandlord>()
                Log.d("RepoTest", "✅ Found ${landlords.size} landlords")
                landlords.forEach {
                    Log.d("RepoTest", "  - ${it.first_name} ${it.last_name}")
                }

                Log.d("RepoTest", "========== TESTS COMPLETED ==========")

            } catch (e: Exception) {
                Log.e("RepoTest", "❌ TEST FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}

// Simple test DTOs
@Serializable
data class TestUser(
    val user_id: Int,
    val email: String,
    val user_type: String
)

@Serializable
data class TestProperty(
    val property_id: Int,
    val title: String,
    val city: String
)

@Serializable
data class TestStudent(
    val student_id: Int,
    val first_name: String,
    val last_name: String
)

@Serializable
data class TestLandlord(
    val landlord_id: Int,
    val first_name: String,
    val last_name: String
)