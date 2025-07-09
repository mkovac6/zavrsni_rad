package com.finalapp.accommodationapp.data.repository.student

import android.util.Log
import com.finalapp.accommodationapp.data.DatabaseConnection
import com.finalapp.accommodationapp.data.model.Booking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Date
import java.sql.Statement

class BookingRepository {
    companion object {
        private const val TAG = "BookingRepository"
    }

    suspend fun createBooking(
        propertyId: Int,
        studentId: Int,
        startDate: String,
        endDate: String,
        totalPrice: Double,
        messageToLandlord: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            
            val query = """
                INSERT INTO Bookings (
                    property_id, student_id, start_date, end_date, 
                    status, total_price, message_to_landlord
                ) VALUES (?, ?, ?, ?, 'pending', ?, ?)
            """.trimIndent()
            
            val preparedStatement = connection?.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
            preparedStatement?.apply {
                setInt(1, propertyId)
                setInt(2, studentId)
                setDate(3, Date.valueOf(startDate))
                setDate(4, Date.valueOf(endDate))
                setDouble(5, totalPrice)
                setString(6, messageToLandlord)
            }
            
            val rowsAffected = preparedStatement?.executeUpdate() ?: 0
            
            preparedStatement?.close()
            connection?.close()
            
            Log.d(TAG, "Booking created: $rowsAffected rows affected")
            rowsAffected > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error creating booking", e)
            false
        }
    }

    suspend fun getStudentBookings(studentId: Int): List<Booking> = withContext(Dispatchers.IO) {
        val bookings = mutableListOf<Booking>()
        
        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                SELECT 
                    b.*,
                    p.title as property_title,
                    p.address as property_address,
                    l.first_name + ' ' + l.last_name as landlord_name
                FROM Bookings b
                JOIN Properties p ON b.property_id = p.property_id
                JOIN Landlords l ON p.landlord_id = l.landlord_id
                WHERE b.student_id = ?
                ORDER BY b.created_at DESC
            """.trimIndent()
            
            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.setInt(1, studentId)
            val resultSet = preparedStatement?.executeQuery()
            
            while (resultSet?.next() == true) {
                bookings.add(
                    Booking(
                        bookingId = resultSet.getInt("booking_id"),
                        propertyId = resultSet.getInt("property_id"),
                        studentId = resultSet.getInt("student_id"),
                        startDate = resultSet.getDate("start_date"),
                        endDate = resultSet.getDate("end_date"),
                        status = resultSet.getString("status"),
                        totalPrice = resultSet.getDouble("total_price"),
                        messageToLandlord = resultSet.getString("message_to_landlord"),
                        createdAt = resultSet.getDate("created_at"),
                        updatedAt = resultSet.getDate("updated_at"),
                        propertyTitle = resultSet.getString("property_title"),
                        propertyAddress = resultSet.getString("property_address"),
                        landlordName = resultSet.getString("landlord_name")
                    )
                )
            }
            
            resultSet?.close()
            preparedStatement?.close()
            connection?.close()
            
            Log.d(TAG, "Loaded ${bookings.size} bookings for student $studentId")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading student bookings", e)
        }
        
        bookings
    }

    suspend fun getLandlordBookings(landlordId: Int): List<Booking> = withContext(Dispatchers.IO) {
        val bookings = mutableListOf<Booking>()
        
        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                SELECT 
                    b.*,
                    p.title as property_title,
                    p.address as property_address,
                    s.first_name + ' ' + s.last_name as student_name
                FROM Bookings b
                JOIN Properties p ON b.property_id = p.property_id
                JOIN Students s ON b.student_id = s.student_id
                WHERE p.landlord_id = ?
                ORDER BY b.created_at DESC
            """.trimIndent()
            
            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.setInt(1, landlordId)
            val resultSet = preparedStatement?.executeQuery()
            
            while (resultSet?.next() == true) {
                bookings.add(
                    Booking(
                        bookingId = resultSet.getInt("booking_id"),
                        propertyId = resultSet.getInt("property_id"),
                        studentId = resultSet.getInt("student_id"),
                        startDate = resultSet.getDate("start_date"),
                        endDate = resultSet.getDate("end_date"),
                        status = resultSet.getString("status"),
                        totalPrice = resultSet.getDouble("total_price"),
                        messageToLandlord = resultSet.getString("message_to_landlord"),
                        createdAt = resultSet.getDate("created_at"),
                        updatedAt = resultSet.getDate("updated_at"),
                        propertyTitle = resultSet.getString("property_title"),
                        propertyAddress = resultSet.getString("property_address"),
                        studentName = resultSet.getString("student_name")
                    )
                )
            }
            
            resultSet?.close()
            preparedStatement?.close()
            connection?.close()
            
            Log.d(TAG, "Loaded ${bookings.size} bookings for landlord $landlordId")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading landlord bookings", e)
        }
        
        bookings
    }

    suspend fun updateBookingStatus(bookingId: Int, status: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                UPDATE Bookings 
                SET status = ?, updated_at = GETDATE() 
                WHERE booking_id = ?
            """.trimIndent()
            
            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.apply {
                setString(1, status)
                setInt(2, bookingId)
            }
            
            val rowsAffected = preparedStatement?.executeUpdate() ?: 0
            
            preparedStatement?.close()
            connection?.close()
            
            Log.d(TAG, "Updated booking $bookingId status to $status")
            rowsAffected > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error updating booking status", e)
            false
        }
    }

    suspend fun checkDateAvailability(propertyId: Int, startDate: String, endDate: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                SELECT COUNT(*) as count
                FROM Bookings
                WHERE property_id = ?
                AND status IN ('approved', 'pending')
                AND NOT (end_date < ? OR start_date > ?)
            """.trimIndent()
            
            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.apply {
                setInt(1, propertyId)
                setDate(2, Date.valueOf(startDate))
                setDate(3, Date.valueOf(endDate))
            }
            
            val resultSet = preparedStatement?.executeQuery()
            val count = if (resultSet?.next() == true) {
                resultSet.getInt("count")
            } else 0
            
            resultSet?.close()
            preparedStatement?.close()
            connection?.close()
            
            Log.d(TAG, "Property $propertyId has $count overlapping bookings")
            count == 0 // Available if no overlapping bookings
        } catch (e: Exception) {
            Log.e(TAG, "Error checking date availability", e)
            false
        }
    }
}