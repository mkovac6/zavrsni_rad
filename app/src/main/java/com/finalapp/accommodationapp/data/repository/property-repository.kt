package com.finalapp.accommodationapp.data.repository

import android.util.Log
import com.finalapp.accommodationapp.data.DatabaseConnection
import com.finalapp.accommodationapp.data.model.Property
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PropertyRepository {
    companion object {
        private const val TAG = "PropertyRepository"
    }

    suspend fun getAllProperties(): List<Property> = withContext(Dispatchers.IO) {
        val properties = mutableListOf<Property>()

        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                SELECT 
                    p.property_id,
                    p.title,
                    p.description,
                    p.property_type,
                    p.address,
                    p.city,
                    p.postal_code,
                    p.latitude,
                    p.longitude,
                    p.price_per_month,
                    p.bedrooms,
                    p.bathrooms,
                    p.total_capacity,
                    p.available_from,
                    p.available_to,
                    p.is_active,
                    l.first_name + ' ' + l.last_name as landlord_name,
                    l.phone as landlord_phone,
                    l.rating as landlord_rating
                FROM Properties p
                JOIN Landlords l ON p.landlord_id = l.landlord_id
                WHERE p.is_active = 1
                ORDER BY p.created_at DESC
            """.trimIndent()

            val statement = connection?.createStatement()
            val resultSet = statement?.executeQuery(query)

            while (resultSet?.next() == true) {
                val property = Property(
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
                    landlordName = resultSet.getString("landlord_name"),
                    landlordPhone = resultSet.getString("landlord_phone"),
                    landlordRating = resultSet.getDouble("landlord_rating")
                )
                properties.add(property)
            }

            resultSet?.close()
            statement?.close()
            connection?.close()

            Log.d(TAG, "Loaded ${properties.size} properties")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading properties", e)
        }

        properties
    }

    suspend fun getPropertyById(propertyId: Int): Property? = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                SELECT 
                    p.*,
                    l.first_name + ' ' + l.last_name as landlord_name,
                    l.phone as landlord_phone,
                    l.rating as landlord_rating,
                    l.company_name
                FROM Properties p
                JOIN Landlords l ON p.landlord_id = l.landlord_id
                WHERE p.property_id = ?
            """.trimIndent()

            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.setInt(1, propertyId)
            val resultSet = preparedStatement?.executeQuery()

            val property = if (resultSet?.next() == true) {
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
                    landlordName = resultSet.getString("landlord_name"),
                    landlordPhone = resultSet.getString("landlord_phone"),
                    landlordRating = resultSet.getDouble("landlord_rating"),
                    companyName = resultSet.getString("company_name")
                )
            } else null

            resultSet?.close()
            preparedStatement?.close()
            connection?.close()

            property
        } catch (e: Exception) {
            Log.e(TAG, "Error loading property $propertyId", e)
            null
        }
    }

    suspend fun getPropertyAmenities(propertyId: Int): List<String> = withContext(Dispatchers.IO) {
        val amenities = mutableListOf<String>()

        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                SELECT a.name
                FROM PropertyAmenities pa
                JOIN Amenities a ON pa.amenity_id = a.amenity_id
                WHERE pa.property_id = ?
                ORDER BY a.name
            """.trimIndent()

            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.setInt(1, propertyId)
            val resultSet = preparedStatement?.executeQuery()

            while (resultSet?.next() == true) {
                amenities.add(resultSet.getString("name"))
            }

            resultSet?.close()
            preparedStatement?.close()
            connection?.close()

        } catch (e: Exception) {
            Log.e(TAG, "Error loading amenities for property $propertyId", e)
        }

        amenities
    }
}