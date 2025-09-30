package com.finalapp.accommodationapp.data.repository.student

import android.util.Log
import com.finalapp.accommodationapp.data.SupabaseClient
import com.finalapp.accommodationapp.data.model.Booking
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.*

class BookingRepository {
    companion object {
        private const val TAG = "BookingRepository"
    }

    private val supabase = SupabaseClient.client
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun createBooking(
        propertyId: Int,
        studentId: Int,
        startDate: String,
        endDate: String,
        totalPrice: Double,
        messageToLandlord: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val newBooking = buildJsonObject {
                put("property_id", propertyId)
                put("student_id", studentId)
                put("start_date", startDate)
                put("end_date", endDate)
                put("status", "pending")
                put("total_price", totalPrice)
                put("message_to_landlord", messageToLandlord)
            }

            supabase.from("bookings")
                .insert(newBooking)

            Log.d(TAG, "Booking created successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating booking", e)
            false
        }
    }

    suspend fun getStudentBookings(studentId: Int): List<Booking> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching bookings for student: $studentId")

            // First get bookings for this student
            val bookings = supabase.from("bookings")
                .select() {
                    filter {
                        eq("student_id", studentId)
                    }
                }
                .decodeList<SimpleBookingDto>()

            Log.d(TAG, "Found ${bookings.size} bookings for student")

            // Get unique property IDs
            val propertyIds = bookings.map { it.property_id }.distinct()

            if (propertyIds.isEmpty()) {
                return@withContext emptyList()
            }

            // Fetch property details
            val properties = supabase.from("properties")
                .select() {
                    filter {
                        isIn("property_id", propertyIds)
                    }
                }
                .decodeList<SimplePropertyDto>()

            // Create a map for quick lookup
            val propertyMap = properties.associateBy { it.property_id }

            // Map bookings with property details
            bookings.map { dto ->
                val property = propertyMap[dto.property_id]
                Booking(
                    bookingId = dto.booking_id,
                    propertyId = dto.property_id,
                    studentId = dto.student_id,
                    startDate = dto.start_date?.let {
                        try { dateFormat.parse(it) } catch (e: Exception) { null }
                    },
                    endDate = dto.end_date?.let {
                        try { dateFormat.parse(it) } catch (e: Exception) { null }
                    },
                    status = dto.status,
                    totalPrice = dto.total_price ?: 0.0,
                    messageToLandlord = dto.message_to_landlord,
                    createdAt = dto.created_at?.let {
                        try { dateFormat.parse(it) } catch (e: Exception) { null }
                    },
                    updatedAt = dto.updated_at?.let {
                        try { dateFormat.parse(it) } catch (e: Exception) { null }
                    },
                    propertyTitle = property?.title,
                    propertyAddress = property?.address,
                    landlordName = "" // We'll skip fetching landlord names for now to keep it simple
                )
            }.also {
                Log.d(TAG, "Successfully loaded ${it.size} bookings for student $studentId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading student bookings: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getLandlordBookings(landlordId: Int): List<Booking> = withContext(Dispatchers.IO) {
        try {
            Log.d("BookingRepository", "Fetching bookings for landlord: $landlordId")

            // get properties for this landlord
            val properties = supabase
                .from("properties")
                .select(columns = Columns.list("property_id", "title", "address")) {
                    filter {
                        eq("landlord_id", landlordId)
                    }
                }
                .decodeList<SimplePropertyDto>()

            val propertyIds = properties.map { it.property_id }

            if (propertyIds.isEmpty()) {
                Log.d("BookingRepository", "No properties found for landlord $landlordId")
                return@withContext emptyList()
            }

            // Then get bookings for those properties
            val bookings = supabase
                .from("bookings")
                .select() {
                    filter {
                        isIn("property_id", propertyIds)
                    }
                }
                .decodeList<SimpleBookingDto>()

            Log.d("BookingRepository", "Found ${bookings.size} bookings")

            // Map properties for quick lookup
            val propertyMap = properties.associateBy { it.property_id }

            bookings.map { dto ->
                val property = propertyMap[dto.property_id]
                Booking(
                    bookingId = dto.booking_id,
                    propertyId = dto.property_id,
                    studentId = dto.student_id,
                    startDate = dto.start_date?.let {
                        try { dateFormat.parse(it) } catch (e: Exception) { null }
                    },
                    endDate = dto.end_date?.let {
                        try { dateFormat.parse(it) } catch (e: Exception) { null }
                    },
                    status = dto.status,
                    totalPrice = dto.total_price ?: 0.0,
                    messageToLandlord = dto.message_to_landlord,
                    propertyTitle = property?.title ?: "Unknown Property",
                    propertyAddress = property?.address ?: "",
                    studentName = "Student #${dto.student_id}"
                )
            }
        } catch (e: Exception) {
            Log.e("BookingRepository", "Error loading landlord bookings: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun updateBookingStatus(bookingId: Int, status: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val updates = buildJsonObject {
                put("status", status)
            }

            supabase.from("bookings")
                .update(updates) {
                    filter {
                        eq("booking_id", bookingId)
                    }
                }

            Log.d(TAG, "Updated booking $bookingId status to $status")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating booking status", e)
            false
        }
    }

    suspend fun countPendingBookingsForLandlord(landlordId: Int): Int = withContext(Dispatchers.IO) {
        try {
            //get property IDs for this landlord
            val properties = supabase
                .from("properties")
                .select(columns = Columns.list("property_id")) {
                    filter {
                        eq("landlord_id", landlordId)
                    }
                }
                .decodeList<PropertyIdOnlyDto>()

            val propertyIds = properties.map { it.property_id }

            if (propertyIds.isEmpty()) {
                return@withContext 0
            }

            // Then count pending bookings for those properties
            val bookings = supabase
                .from("bookings")
                .select(columns = Columns.list("booking_id")) {
                    filter {
                        isIn("property_id", propertyIds)
                        eq("status", "pending")
                    }
                }
                .decodeList<BookingIdOnlyDto>()

            bookings.size
        } catch (e: Exception) {
            Log.e("BookingRepository", "Error counting pending bookings: ${e.message}", e)
            0
        }
    }

    suspend fun checkDateAvailability(propertyId: Int, startDate: String, endDate: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // Get all bookings for this property that might overlap
                val result = supabase.from("bookings")
                    .select {
                        filter {
                            eq("property_id", propertyId)
                            or {
                                eq("status", "approved")
                                eq("status", "pending")
                            }
                        }
                    }
                    .decodeList<BookingDateDto>()

                // Check for overlaps manually
                val requestStart = dateFormat.parse(startDate)!!
                val requestEnd = dateFormat.parse(endDate)!!

                val hasOverlap = result.any { booking ->
                    val bookingStart = booking.start_date?.let { dateFormat.parse(it) } ?: return@any false
                    val bookingEnd = booking.end_date?.let { dateFormat.parse(it) } ?: return@any false

                    // Check if dates overlap
                    !(bookingEnd.before(requestStart) || bookingStart.after(requestEnd))
                }

                Log.d(TAG, "Property $propertyId availability check: ${!hasOverlap}")
                !hasOverlap // Available if no overlapping bookings
            } catch (e: Exception) {
                Log.e(TAG, "Error checking date availability", e)
                false
            }
        }
}

// DTOs for Supabase
@Serializable
data class BookingDto(
    val booking_id: Int,
    val property_id: Int,
    val student_id: Int,
    val start_date: String? = null,
    val end_date: String? = null,
    val status: String? = "pending",
    val total_price: Double? = 0.0,
    val message_to_landlord: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val properties: PropertyInfoDto? = null
)

@Serializable
data class BookingWithStudentDto(
    val booking_id: Int,
    val property_id: Int,
    val student_id: Int,
    val start_date: String? = null,
    val end_date: String? = null,
    val status: String? = "pending",
    val total_price: Double? = 0.0,
    val message_to_landlord: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val properties: PropertyInfoDto? = null,
    val students: StudentInfoDto? = null
)

@Serializable
data class PropertyInfoDto(
    val property_id: Int? = null,
    val title: String? = null,
    val address: String? = null,
    val landlord_id: Int? = null,
    val landlords: LandlordNameDto? = null
)

@Serializable
data class LandlordNameDto(
    val first_name: String,
    val last_name: String
)

@Serializable
data class StudentInfoDto(
    val first_name: String,
    val last_name: String
)

@Serializable
data class BookingCountDto(
    val booking_id: Int
)

@Serializable
data class BookingDateDto(
    val booking_id: Int,
    val start_date: String? = null,
    val end_date: String? = null,
    val status: String? = null
)

@Serializable
data class SimplePropertyDto(
    val property_id: Int,
    val title: String? = null,
    val address: String? = null
)

@Serializable
data class PropertyIdOnlyDto(
    val property_id: Int
)

@Serializable
data class BookingIdOnlyDto(
    val booking_id: Int
)

@Serializable
data class SimpleBookingDto(
    val booking_id: Int,
    val property_id: Int,
    val student_id: Int,
    val start_date: String? = null,
    val end_date: String? = null,
    val status: String,
    val total_price: Double? = null,
    val message_to_landlord: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)