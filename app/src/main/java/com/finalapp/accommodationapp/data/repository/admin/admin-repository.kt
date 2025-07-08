package com.finalapp.accommodationapp.data.repository.admin

import android.util.Log
import com.finalapp.accommodationapp.data.DatabaseConnection
import com.finalapp.accommodationapp.data.model.admin.StudentWithUser
import com.finalapp.accommodationapp.data.model.admin.LandlordWithUser
import com.finalapp.accommodationapp.data.model.admin.Amenity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Date
import java.sql.Statement

class AdminRepository {
    companion object {
        private const val TAG = "AdminRepository"
    }

    // Student Management
    suspend fun getAllStudents(): List<StudentWithUser> = withContext(Dispatchers.IO) {
        val students = mutableListOf<StudentWithUser>()

        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                SELECT 
                    s.*,
                    u.email,
                    u.is_profile_complete,
                    uni.name as university_name
                FROM Students s
                JOIN Users u ON s.user_id = u.user_id
                JOIN Universities uni ON s.university_id = uni.university_id
                ORDER BY s.created_at DESC
            """.trimIndent()

            val statement = connection?.createStatement()
            val resultSet = statement?.executeQuery(query)

            while (resultSet?.next() == true) {
                val student = StudentWithUser(
                    studentId = resultSet.getInt("student_id"),
                    userId = resultSet.getInt("user_id"),
                    email = resultSet.getString("email"),
                    firstName = resultSet.getString("first_name"),
                    lastName = resultSet.getString("last_name"),
                    phone = resultSet.getString("phone"),
                    studentNumber = resultSet.getString("student_number"),
                    yearOfStudy = resultSet.getInt("year_of_study"),
                    program = resultSet.getString("program"),
                    universityName = resultSet.getString("university_name"),
                    isProfileComplete = resultSet.getBoolean("is_profile_complete")
                )
                students.add(student)
            }

            resultSet?.close()
            statement?.close()
            connection?.close()

            Log.d(TAG, "Loaded ${students.size} students")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading students", e)
        }

        students
    }

    suspend fun deleteStudent(userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            connection?.autoCommit = false

            try {
                // Delete from Students table first (due to foreign key)
                val deleteStudentQuery = "DELETE FROM Students WHERE user_id = ?"
                val studentStatement = connection?.prepareStatement(deleteStudentQuery)
                studentStatement?.setInt(1, userId)
                studentStatement?.executeUpdate()

                // Delete from Users table
                val deleteUserQuery = "DELETE FROM Users WHERE user_id = ?"
                val userStatement = connection?.prepareStatement(deleteUserQuery)
                userStatement?.setInt(1, userId)
                userStatement?.executeUpdate()

                connection?.commit()

                studentStatement?.close()
                userStatement?.close()
                connection?.close()

                Log.d(TAG, "Deleted student with userId: $userId")
                true
            } catch (e: Exception) {
                connection?.rollback()
                Log.e(TAG, "Error deleting student", e)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connection", e)
            false
        }
    }

    // Landlord Management
    suspend fun getAllLandlords(): List<LandlordWithUser> = withContext(Dispatchers.IO) {
        val landlords = mutableListOf<LandlordWithUser>()

        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                SELECT 
                    l.*,
                    u.email,
                    u.is_profile_complete,
                    (SELECT COUNT(*) FROM Properties WHERE landlord_id = l.landlord_id) as property_count
                FROM Landlords l
                JOIN Users u ON l.user_id = u.user_id
                ORDER BY l.created_at DESC
            """.trimIndent()

            val statement = connection?.createStatement()
            val resultSet = statement?.executeQuery(query)

            while (resultSet?.next() == true) {
                val landlord = LandlordWithUser(
                    landlordId = resultSet.getInt("landlord_id"),
                    userId = resultSet.getInt("user_id"),
                    email = resultSet.getString("email"),
                    firstName = resultSet.getString("first_name"),
                    lastName = resultSet.getString("last_name"),
                    companyName = resultSet.getString("company_name"),
                    phone = resultSet.getString("phone"),
                    isVerified = resultSet.getBoolean("is_verified"),
                    rating = resultSet.getDouble("rating"),
                    propertyCount = resultSet.getInt("property_count")
                )
                landlords.add(landlord)
            }

            resultSet?.close()
            statement?.close()
            connection?.close()

            Log.d(TAG, "Loaded ${landlords.size} landlords")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading landlords", e)
        }

        landlords
    }

    suspend fun deleteLandlord(userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            connection?.autoCommit = false

            try {
                // First, get the landlord_id
                val getLandlordIdQuery = "SELECT landlord_id FROM Landlords WHERE user_id = ?"
                val prepStatement = connection?.prepareStatement(getLandlordIdQuery)
                prepStatement?.setInt(1, userId)
                val resultSet = prepStatement?.executeQuery()

                if (resultSet?.next() == true) {
                    val landlordId = resultSet.getInt("landlord_id")

                    // Delete all property amenities for this landlord's properties
                    val deleteAmenitiesQuery = """
                        DELETE FROM PropertyAmenities 
                        WHERE property_id IN (SELECT property_id FROM Properties WHERE landlord_id = ?)
                    """.trimIndent()
                    val amenitiesStatement = connection.prepareStatement(deleteAmenitiesQuery)
                    amenitiesStatement.setInt(1, landlordId)
                    amenitiesStatement.executeUpdate()

                    // Delete all properties
                    val deletePropertiesQuery = "DELETE FROM Properties WHERE landlord_id = ?"
                    val propertiesStatement = connection.prepareStatement(deletePropertiesQuery)
                    propertiesStatement.setInt(1, landlordId)
                    propertiesStatement.executeUpdate()

                    // Delete landlord
                    val deleteLandlordQuery = "DELETE FROM Landlords WHERE user_id = ?"
                    val landlordStatement = connection.prepareStatement(deleteLandlordQuery)
                    landlordStatement.setInt(1, userId)
                    landlordStatement.executeUpdate()

                    // Delete user
                    val deleteUserQuery = "DELETE FROM Users WHERE user_id = ?"
                    val userStatement = connection.prepareStatement(deleteUserQuery)
                    userStatement.setInt(1, userId)
                    userStatement.executeUpdate()

                    connection.commit()

                    amenitiesStatement.close()
                    propertiesStatement.close()
                    landlordStatement.close()
                    userStatement.close()
                }

                resultSet?.close()
                prepStatement?.close()
                connection?.close()

                Log.d(TAG, "Deleted landlord with userId: $userId")
                true
            } catch (e: Exception) {
                connection?.rollback()
                Log.e(TAG, "Error deleting landlord", e)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connection", e)
            false
        }
    }

    suspend fun createLandlordProfile(
        userId: Int,
        firstName: String,
        lastName: String,
        companyName: String?,
        phone: String,
        isVerified: Boolean,
        rating: Double = 0.0
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                INSERT INTO Landlords (
                    user_id, first_name, last_name, company_name, 
                    phone, is_verified, rating
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.apply {
                setInt(1, userId)
                setString(2, firstName)
                setString(3, lastName)
                setString(4, companyName)
                setString(5, phone)
                setBoolean(6, isVerified)
                setDouble(7, rating)
            }

            val rowsAffected = statement?.executeUpdate() ?: 0

            statement?.close()
            connection?.close()

            Log.d(TAG, "Created landlord profile for userId: $userId")
            rowsAffected > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error creating landlord profile", e)
            false
        }
    }

    // Property Management
    suspend fun deleteProperty(propertyId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            connection?.autoCommit = false

            try {
                // Delete property amenities
                val deleteAmenitiesQuery = "DELETE FROM PropertyAmenities WHERE property_id = ?"
                val amenitiesStatement = connection?.prepareStatement(deleteAmenitiesQuery)
                amenitiesStatement?.setInt(1, propertyId)
                amenitiesStatement?.executeUpdate()

                // Delete property
                val deletePropertyQuery = "DELETE FROM Properties WHERE property_id = ?"
                val propertyStatement = connection?.prepareStatement(deletePropertyQuery)
                propertyStatement?.setInt(1, propertyId)
                propertyStatement?.executeUpdate()

                connection?.commit()

                amenitiesStatement?.close()
                propertyStatement?.close()
                connection?.close()

                Log.d(TAG, "Deleted property with id: $propertyId")
                true
            } catch (e: Exception) {
                connection?.rollback()
                Log.e(TAG, "Error deleting property", e)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connection", e)
            false
        }
    }

    suspend fun createProperty(
        landlordId: Int,
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
        availableFrom: String
    ): Int = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                INSERT INTO Properties (
                    landlord_id, title, description, property_type,
                    address, city, postal_code, price_per_month,
                    bedrooms, bathrooms, total_capacity, available_from,
                    is_active
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
            """.trimIndent()

            val statement = connection?.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
            statement?.apply {
                setInt(1, landlordId)
                setString(2, title)
                setString(3, description)
                setString(4, propertyType)
                setString(5, address)
                setString(6, city)
                setString(7, postalCode)
                setDouble(8, pricePerMonth)
                setInt(9, bedrooms)
                setInt(10, bathrooms)
                setInt(11, totalCapacity)
                setDate(12, Date.valueOf(availableFrom))
            }

            statement?.executeUpdate()

            // Get the generated property ID
            val generatedKeys = statement?.generatedKeys
            val propertyId = if (generatedKeys?.next() == true) {
                generatedKeys.getInt(1)
            } else {
                0
            }

            generatedKeys?.close()
            statement?.close()
            connection?.close()

            Log.d(TAG, "Created property with ID: $propertyId")
            propertyId
        } catch (e: Exception) {
            Log.e(TAG, "Error creating property", e)
            0
        }
    }

    suspend fun addPropertyAmenities(propertyId: Int, amenityIds: List<Int>): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = "INSERT INTO PropertyAmenities (property_id, amenity_id) VALUES (?, ?)"
            val statement = connection?.prepareStatement(query)

            amenityIds.forEach { amenityId ->
                statement?.apply {
                    setInt(1, propertyId)
                    setInt(2, amenityId)
                    addBatch()
                }
            }

            statement?.executeBatch()

            statement?.close()
            connection?.close()

            Log.d(TAG, "Added ${amenityIds.size} amenities to property $propertyId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding property amenities", e)
            false
        }
    }

    // University Management
    suspend fun deleteUniversity(universityId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()

            // Check if any students are enrolled
            val checkQuery = "SELECT COUNT(*) as count FROM Students WHERE university_id = ?"
            val checkStatement = connection?.prepareStatement(checkQuery)
            checkStatement?.setInt(1, universityId)
            val resultSet = checkStatement?.executeQuery()

            if (resultSet?.next() == true && resultSet.getInt("count") > 0) {
                Log.w(TAG, "Cannot delete university with enrolled students")
                false
            } else {
                // Safe to delete
                val deleteQuery = "DELETE FROM Universities WHERE university_id = ?"
                val deleteStatement = connection?.prepareStatement(deleteQuery)
                deleteStatement?.setInt(1, universityId)
                val rowsAffected = deleteStatement?.executeUpdate() ?: 0

                deleteStatement?.close()
                checkStatement?.close()
                connection?.close()

                Log.d(TAG, "Deleted university with id: $universityId")
                rowsAffected > 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting university", e)
            false
        }
    }

    suspend fun addUniversity(name: String, city: String, country: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = "INSERT INTO Universities (name, city, country, is_active) VALUES (?, ?, ?, 1)"
            val statement = connection?.prepareStatement(query)

            statement?.apply {
                setString(1, name)
                setString(2, city)
                setString(3, country)
            }

            val rowsAffected = statement?.executeUpdate() ?: 0

            statement?.close()
            connection?.close()

            Log.d(TAG, "Added new university: $name")
            rowsAffected > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error adding university", e)
            false
        }
    }

    // Utility functions
    suspend fun updateUserProfileStatus(userId: Int, isComplete: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = "UPDATE Users SET is_profile_complete = ? WHERE user_id = ?"
            val statement = connection?.prepareStatement(query)

            statement?.apply {
                setBoolean(1, isComplete)
                setInt(2, userId)
            }

            val rowsAffected = statement?.executeUpdate() ?: 0

            statement?.close()
            connection?.close()

            Log.d(TAG, "Updated profile status for userId: $userId")
            rowsAffected > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile status", e)
            false
        }
    }

    // Add this method to AdminRepository.kt:
    suspend fun createLandlordWithAccount(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        companyName: String?,
        phone: String,
        isVerified: Boolean,
        rating: Double = 0.0
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            connection?.autoCommit = false

            try {
                // First create the user account
                val userQuery = "INSERT INTO Users (email, password_hash, user_type, is_profile_complete) VALUES (?, ?, 'landlord', 1)"
                val userStatement = connection?.prepareStatement(userQuery, Statement.RETURN_GENERATED_KEYS)
                userStatement?.setString(1, email)
                userStatement?.setString(2, password)
                userStatement?.executeUpdate()

                // Get the generated user ID
                val generatedKeys = userStatement?.generatedKeys
                val userId = if (generatedKeys?.next() == true) {
                    generatedKeys.getInt(1)
                } else {
                    throw Exception("Failed to get user ID")
                }

                // Create the landlord profile
                val landlordQuery = """
                INSERT INTO Landlords (
                    user_id, first_name, last_name, company_name, 
                    phone, is_verified, rating
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

                val landlordStatement = connection?.prepareStatement(landlordQuery)
                landlordStatement?.apply {
                    setInt(1, userId)
                    setString(2, firstName)
                    setString(3, lastName)
                    setString(4, companyName)
                    setString(5, phone)
                    setBoolean(6, isVerified)
                    setDouble(7, rating)
                }
                landlordStatement?.executeUpdate()

                connection?.commit()

                generatedKeys?.close()
                userStatement?.close()
                landlordStatement?.close()
                connection?.close()

                Log.d(TAG, "Created landlord with userId: $userId")
                true
            } catch (e: Exception) {
                connection?.rollback()
                Log.e(TAG, "Error creating landlord with account", e)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connection", e)
            false
        }
    }

    suspend fun getAllAmenities(): List<Amenity> = withContext(Dispatchers.IO) {
        val amenities = mutableListOf<Amenity>()

        try {
            val connection = DatabaseConnection.getConnection()
            val query = "SELECT * FROM Amenities ORDER BY name"
            val statement = connection?.createStatement()
            val resultSet = statement?.executeQuery(query)

            while (resultSet?.next() == true) {
                val amenity = Amenity(
                    amenityId = resultSet.getInt("amenity_id"),
                    name = resultSet.getString("name"),
                    category = resultSet.getString("category")
                )
                amenities.add(amenity)
            }

            resultSet?.close()
            statement?.close()
            connection?.close()

            Log.d(TAG, "Loaded ${amenities.size} amenities")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading amenities", e)
        }

        amenities
    }
}