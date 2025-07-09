package com.finalapp.accommodationapp.data.repository.student

import android.util.Log
import com.finalapp.accommodationapp.data.DatabaseConnection
import com.finalapp.accommodationapp.data.model.Property
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoritesRepository {
    companion object {
        private const val TAG = "FavoritesRepository"
    }

    suspend fun addToFavorites(studentId: Int, propertyId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            
            // Check if already exists
            val checkQuery = "SELECT COUNT(*) as count FROM Favorites WHERE student_id = ? AND property_id = ?"
            val checkStmt = connection?.prepareStatement(checkQuery)
            checkStmt?.apply {
                setInt(1, studentId)
                setInt(2, propertyId)
            }
            
            val resultSet = checkStmt?.executeQuery()
            if (resultSet?.next() == true && resultSet.getInt("count") > 0) {
                Log.d(TAG, "Property $propertyId already in favorites for student $studentId")
                resultSet.close()
                checkStmt?.close()
                connection?.close()
                return@withContext true // Already exists, consider it success
            }
            
            resultSet?.close()
            checkStmt?.close()
            
            // Add to favorites
            val insertQuery = "INSERT INTO Favorites (student_id, property_id) VALUES (?, ?)"
            val insertStmt = connection?.prepareStatement(insertQuery)
            insertStmt?.apply {
                setInt(1, studentId)
                setInt(2, propertyId)
            }
            
            val rowsAffected = insertStmt?.executeUpdate() ?: 0
            insertStmt?.close()
            connection?.close()
            
            Log.d(TAG, "Added property $propertyId to favorites for student $studentId")
            rowsAffected > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to favorites", e)
            false
        }
    }

    suspend fun removeFromFavorites(studentId: Int, propertyId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = "DELETE FROM Favorites WHERE student_id = ? AND property_id = ?"
            
            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.apply {
                setInt(1, studentId)
                setInt(2, propertyId)
            }
            
            val rowsAffected = preparedStatement?.executeUpdate() ?: 0
            preparedStatement?.close()
            connection?.close()
            
            Log.d(TAG, "Removed property $propertyId from favorites for student $studentId")
            rowsAffected > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error removing from favorites", e)
            false
        }
    }

    suspend fun isFavorite(studentId: Int, propertyId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = "SELECT COUNT(*) as count FROM Favorites WHERE student_id = ? AND property_id = ?"
            
            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.apply {
                setInt(1, studentId)
                setInt(2, propertyId)
            }
            
            val resultSet = preparedStatement?.executeQuery()
            val count = if (resultSet?.next() == true) {
                resultSet.getInt("count")
            } else 0
            
            resultSet?.close()
            preparedStatement?.close()
            connection?.close()
            
            count > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking favorite status", e)
            false
        }
    }

    suspend fun getStudentFavorites(studentId: Int): List<Property> = withContext(Dispatchers.IO) {
        val properties = mutableListOf<Property>()
        
        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                SELECT 
                    p.*,
                    l.first_name + ' ' + l.last_name as landlord_name,
                    l.phone as landlord_phone,
                    l.rating as landlord_rating
                FROM Properties p
                JOIN Landlords l ON p.landlord_id = l.landlord_id
                JOIN Favorites f ON p.property_id = f.property_id
                WHERE f.student_id = ? AND p.is_active = 1
                ORDER BY f.created_at DESC
            """.trimIndent()
            
            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.setInt(1, studentId)
            val resultSet = preparedStatement?.executeQuery()
            
            while (resultSet?.next() == true) {
                properties.add(
                    Property(
                        propertyId = resultSet.getInt("property_id"),
                        title = resultSet.getString("title"),
                        description = resultSet.getString("description") ?: "",
                        propertyType = resultSet.getString("property_type"),
                        address = resultSet.getString("address"),
                        city = resultSet.getString("city"),
                        postalCode = resultSet.getString("postal_code") ?: "",
                        latitude = resultSet.getDouble("latitude"),
                        longitude = resultSet.getDouble("longitude"),
                        pricePerMonth = resultSet.getDouble("price_per_month"),
                        bedrooms = resultSet.getInt("bedrooms"),
                        bathrooms = resultSet.getInt("bathrooms"),
                        totalCapacity = resultSet.getInt("total_capacity"),
                        availableFrom = resultSet.getDate("available_from"),
                        availableTo = resultSet.getDate("available_to"),
                        isActive = resultSet.getBoolean("is_active"),
                        landlordName = resultSet.getString("landlord_name"),
                        landlordPhone = resultSet.getString("landlord_phone"),
                        landlordRating = resultSet.getDouble("landlord_rating")
                    )
                )
            }
            
            resultSet?.close()
            preparedStatement?.close()
            connection?.close()
            
            Log.d(TAG, "Loaded ${properties.size} favorite properties for student $studentId")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading favorite properties", e)
        }
        
        properties
    }
}