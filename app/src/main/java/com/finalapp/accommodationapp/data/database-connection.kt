package com.finalapp.accommodationapp.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object DatabaseConnection {
    private const val TAG = "DatabaseConnection"

    init {
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver")
            Log.d(TAG, "jTDS SQL Server Driver loaded successfully")
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "jTDS SQL Server Driver not found", e)
        }
    }

    suspend fun getConnection(): Connection? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to connect to: ${DatabaseConfig.CONNECTION_URL}")
            // Set a timeout for the connection attempt
            DriverManager.setLoginTimeout(10) // 10 seconds timeout

            val connection = DriverManager.getConnection(DatabaseConfig.CONNECTION_URL)
            Log.d(TAG, "Database connection successful!")
            connection
        } catch (e: SQLException) {
            Log.e(TAG, "Connection failed: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            null
        }
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = getConnection()
            val isValid = connection != null
            connection?.close()
            Log.d(TAG, "Connection test result: $isValid")
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed: ${e.message}", e)
            false
        }
    }

    suspend fun getUniversities(): List<String> = withContext(Dispatchers.IO) {
        val universities = mutableListOf<String>()
        try {
            val connection = getConnection()
            val statement = connection?.createStatement()
            val resultSet = statement?.executeQuery("SELECT name FROM Universities WHERE is_active = 1")

            while (resultSet?.next() == true) {
                universities.add(resultSet.getString("name"))
            }

            resultSet?.close()
            statement?.close()
            connection?.close()

            Log.d(TAG, "Found ${universities.size} universities")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching universities: ${e.message}", e)
        }
        universities
    }
}