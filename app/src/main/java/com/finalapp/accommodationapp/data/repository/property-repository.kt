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
                    isActive = resultSet.getBoolean("is_active"),
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
                    isActive = resultSet.getBoolean("is_active"),
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

    suspend fun updateProperty(
        propertyId: Int,
        title: String,
        description: String,
        propertyType: String,
        address: String,
        city: String,
        postalCode: String,
        pricePerMonth: Double,
        bedrooms: Int,
        bathrooms: Int,
        totalCapacity: Int,
        availableFrom: String,
        availableTo: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                UPDATE Properties SET 
                    title = ?, 
                    description = ?, 
                    property_type = ?, 
                    address = ?, 
                    city = ?, 
                    postal_code = ?, 
                    price_per_month = ?, 
                    bedrooms = ?, 
                    bathrooms = ?, 
                    total_capacity = ?, 
                    available_from = ?, 
                    available_to = ?,
                    updated_at = GETDATE()
                WHERE property_id = ?
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.apply {
                setString(1, title)
                setString(2, description)
                setString(3, propertyType)
                setString(4, address)
                setString(5, city)
                setString(6, postalCode)
                setDouble(7, pricePerMonth)
                setInt(8, bedrooms)
                setInt(9, bathrooms)
                setInt(10, totalCapacity)
                setDate(11, java.sql.Date.valueOf(availableFrom))
                if (availableTo != null) {
                    setDate(12, java.sql.Date.valueOf(availableTo))
                } else {
                    setNull(12, java.sql.Types.DATE)
                }
                setInt(13, propertyId)
            }

            val rowsAffected = statement?.executeUpdate() ?: 0

            statement?.close()
            connection?.close()

            Log.d(TAG, "Updated property $propertyId: $rowsAffected rows affected")
            rowsAffected > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error updating property", e)
            false
        }
    }

    suspend fun deleteProperty(propertyId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            connection?.autoCommit = false

            try {
                // First delete related records
                val deleteAmenitiesQuery = "DELETE FROM PropertyAmenities WHERE property_id = ?"
                val amenitiesStatement = connection?.prepareStatement(deleteAmenitiesQuery)
                amenitiesStatement?.setInt(1, propertyId)
                amenitiesStatement?.executeUpdate()
                amenitiesStatement?.close()

                // Delete property images if any
                val deleteImagesQuery = "DELETE FROM PropertyImages WHERE property_id = ?"
                val imagesStatement = connection?.prepareStatement(deleteImagesQuery)
                imagesStatement?.setInt(1, propertyId)
                imagesStatement?.executeUpdate()
                imagesStatement?.close()

                // Delete bookings if any
                val deleteBookingsQuery = "DELETE FROM Bookings WHERE property_id = ?"
                val bookingsStatement = connection?.prepareStatement(deleteBookingsQuery)
                bookingsStatement?.setInt(1, propertyId)
                bookingsStatement?.executeUpdate()
                bookingsStatement?.close()

                // Delete reviews if any
                val deleteReviewsQuery = "DELETE FROM Reviews WHERE property_id = ?"
                val reviewsStatement = connection?.prepareStatement(deleteReviewsQuery)
                reviewsStatement?.setInt(1, propertyId)
                reviewsStatement?.executeUpdate()
                reviewsStatement?.close()

                // Delete favorites if any
                val deleteFavoritesQuery = "DELETE FROM Favorites WHERE property_id = ?"
                val favoritesStatement = connection?.prepareStatement(deleteFavoritesQuery)
                favoritesStatement?.setInt(1, propertyId)
                favoritesStatement?.executeUpdate()
                favoritesStatement?.close()

                // Finally delete the property
                val deletePropertyQuery = "DELETE FROM Properties WHERE property_id = ?"
                val propertyStatement = connection?.prepareStatement(deletePropertyQuery)
                propertyStatement?.setInt(1, propertyId)
                val rowsAffected = propertyStatement?.executeUpdate() ?: 0
                propertyStatement?.close()

                connection?.commit()
                connection?.close()

                Log.d(TAG, "Deleted property $propertyId and related records")
                rowsAffected > 0
            } catch (e: Exception) {
                connection?.rollback()
                connection?.close()
                Log.e(TAG, "Error deleting property", e)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connection", e)
            false
        }
    }

    suspend fun updatePropertyAmenities(propertyId: Int, amenityIds: List<Int>): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            connection?.autoCommit = false

            try {
                // First delete existing amenities
                val deleteQuery = "DELETE FROM PropertyAmenities WHERE property_id = ?"
                val deleteStatement = connection?.prepareStatement(deleteQuery)
                deleteStatement?.setInt(1, propertyId)
                deleteStatement?.executeUpdate()
                deleteStatement?.close()

                // Insert new amenities
                if (amenityIds.isNotEmpty()) {
                    val insertQuery = "INSERT INTO PropertyAmenities (property_id, amenity_id) VALUES (?, ?)"
                    val insertStatement = connection?.prepareStatement(insertQuery)

                    amenityIds.forEach { amenityId ->
                        insertStatement?.apply {
                            setInt(1, propertyId)
                            setInt(2, amenityId)
                            addBatch()
                        }
                    }

                    insertStatement?.executeBatch()
                    insertStatement?.close()
                }

                connection?.commit()
                connection?.close()

                Log.d(TAG, "Updated amenities for property $propertyId")
                true
            } catch (e: Exception) {
                connection?.rollback()
                connection?.close()
                Log.e(TAG, "Error updating property amenities", e)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connection", e)
            false
        }
    }

    suspend fun getPropertyAmenities(propertyId: Int): List<Int> = withContext(Dispatchers.IO) {
        val amenityIds = mutableListOf<Int>()

        try {
            val connection = DatabaseConnection.getConnection()
            val query = "SELECT amenity_id FROM PropertyAmenities WHERE property_id = ?"

            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.setInt(1, propertyId)
            val resultSet = preparedStatement?.executeQuery()

            while (resultSet?.next() == true) {
                amenityIds.add(resultSet.getInt("amenity_id"))
            }

            resultSet?.close()
            preparedStatement?.close()
            connection?.close()

            Log.d(TAG, "Loaded ${amenityIds.size} amenities for property $propertyId")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading property amenities", e)
        }

        amenityIds
    }

    suspend fun getPropertyAmenitiesAsStrings(propertyId: Int): List<String> = withContext(Dispatchers.IO) {
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

    suspend fun updatePropertyStatus(propertyId: Int, isActive: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = "UPDATE Properties SET is_active = ? WHERE property_id = ?"
            val statement = connection?.prepareStatement(query)

            statement?.apply {
                setBoolean(1, isActive)
                setInt(2, propertyId)
            }

            val rowsAffected = statement?.executeUpdate() ?: 0

            statement?.close()
            connection?.close()

            Log.d(TAG, "Updated property $propertyId status to: $isActive")
            rowsAffected > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error updating property status", e)
            false
        }
    }

    suspend fun getPropertiesByLandlordId(landlordId: Int): List<Property> = withContext(Dispatchers.IO) {
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
                WHERE p.landlord_id = ?
                ORDER BY p.created_at DESC
            """.trimIndent()

            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.setInt(1, landlordId)
            val resultSet = preparedStatement?.executeQuery()

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
                    isActive = resultSet.getBoolean("is_active"),
                    landlordName = resultSet.getString("landlord_name"),
                    landlordPhone = resultSet.getString("landlord_phone"),
                    landlordRating = resultSet.getDouble("landlord_rating")
                )
                properties.add(property)
            }

            resultSet?.close()
            preparedStatement?.close()
            connection?.close()

            Log.d(TAG, "Loaded ${properties.size} properties for landlord $landlordId")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading properties for landlord", e)
        }

        properties
    }
}