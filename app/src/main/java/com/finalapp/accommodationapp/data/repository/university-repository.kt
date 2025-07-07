package com.finalapp.accommodationapp.data.repository

import android.util.Log
import com.finalapp.accommodationapp.data.DatabaseConnection
import com.finalapp.accommodationapp.data.model.University
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UniversityRepository {
    companion object {
        private const val TAG = "UniversityRepository"
    }
    
    suspend fun getAllUniversities(): List<University> = withContext(Dispatchers.IO) {
        val universities = mutableListOf<University>()
        try {
            val connection = DatabaseConnection.getConnection()
            val statement = connection?.createStatement()
            val query = "SELECT university_id, name, city, country FROM Universities WHERE is_active = 1 ORDER BY name"
            val resultSet = statement?.executeQuery(query)
            
            while (resultSet?.next() == true) {
                universities.add(
                    University(
                        universityId = resultSet.getInt("university_id"),
                        name = resultSet.getString("name"),
                        city = resultSet.getString("city"),
                        country = resultSet.getString("country")
                    )
                )
            }
            
            resultSet?.close()
            statement?.close()
            connection?.close()
            
            Log.d(TAG, "Loaded ${universities.size} universities")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching universities: ${e.message}", e)
        }
        universities
    }
    
    suspend fun getUniversityById(id: Int): University? = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = "SELECT university_id, name, city, country FROM Universities WHERE university_id = ?"
            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.setInt(1, id)
            
            val resultSet = preparedStatement?.executeQuery()
            
            var university: University? = null
            if (resultSet?.next() == true) {
                university = University(
                    universityId = resultSet.getInt("university_id"),
                    name = resultSet.getString("name"),
                    city = resultSet.getString("city"),
                    country = resultSet.getString("country")
                )
            }
            
            resultSet?.close()
            preparedStatement?.close()
            connection?.close()
            
            university
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching university by id: ${e.message}", e)
            null
        }
    }
}