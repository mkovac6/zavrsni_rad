package com.finalapp.accommodationapp.data.repository.student

import android.util.Log
import com.finalapp.accommodationapp.data.SupabaseClient
import com.finalapp.accommodationapp.data.model.Review
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.*

class ReviewRepository {
    companion object {
        private const val TAG = "ReviewRepository"
    }

    private val supabase = SupabaseClient.client

    @Serializable
    data class ReviewDto(
        val review_id: Int,
        val booking_id: Int,
        val property_id: Int,
        val student_id: Int,
        val landlord_id: Int,
        val property_rating: Int,
        val landlord_rating: Int,
        val comment: String? = null,
        val created_at: String? = null
    )

    @Serializable
    data class ReviewWithDetailsDto(
        val review_id: Int,
        val booking_id: Int,
        val property_id: Int,
        val student_id: Int,
        val landlord_id: Int,
        val property_rating: Int,
        val landlord_rating: Int,
        val comment: String? = null,
        val created_at: String? = null,
        val property_title: String? = null,
        val student_name: String? = null,
        val student_email: String? = null
    )

    /**
     * Submit a review for a booking
     */
    suspend fun submitReview(
        bookingId: Int,
        propertyId: Int,
        studentId: Int,
        landlordId: Int,
        propertyRating: Int,
        landlordRating: Int,
        comment: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if review already exists
            val existingReview = supabase.from("reviews")
                .select {
                    filter {
                        eq("booking_id", bookingId)
                    }
                }
                .decodeSingleOrNull<ReviewDto>()

            if (existingReview != null) {
                Log.e(TAG, "Review already exists for booking $bookingId")
                return@withContext false
            }

            // Insert review
            val reviewData = buildJsonObject {
                put("booking_id", bookingId)
                put("property_id", propertyId)
                put("student_id", studentId)
                put("landlord_id", landlordId)
                put("property_rating", propertyRating)
                put("landlord_rating", landlordRating)
                put("comment", comment)
            }

            supabase.from("reviews").insert(reviewData)

            // Update property average rating
            updatePropertyAverageRating(propertyId)

            // Update landlord average rating
            updateLandlordAverageRating(landlordId)

            Log.d(TAG, "Review submitted successfully for booking $bookingId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting review: ${e.message}", e)
            false
        }
    }

    /**
     * Get reviews for a property
     */
    suspend fun getReviewsForProperty(propertyId: Int): List<Review> = withContext(Dispatchers.IO) {
        try {
            val reviews = supabase.from("reviews")
                .select {
                    filter {
                        eq("property_id", propertyId)
                    }
                }
                .decodeList<ReviewDto>()

            reviews.map { dto -> mapReviewDtoToReview(dto) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting reviews for property: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get reviews by landlord ID
     */
    suspend fun getReviewsForLandlord(landlordId: Int): List<Review> = withContext(Dispatchers.IO) {
        try {
            val reviews = supabase.from("reviews")
                .select {
                    filter {
                        eq("landlord_id", landlordId)
                    }
                }
                .decodeList<ReviewDto>()

            reviews.map { dto -> mapReviewDtoToReview(dto) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting reviews for landlord: ${e.message}")
            emptyList()
        }
    }

    /**
     * Check if a booking has been reviewed
     */
    suspend fun isBookingReviewed(bookingId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val review = supabase.from("reviews")
                .select {
                    filter {
                        eq("booking_id", bookingId)
                    }
                }
                .decodeSingleOrNull<ReviewDto>()

            review != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if booking is reviewed: ${e.message}")
            false
        }
    }

    /**
     * Get completed bookings without reviews for a student
     */
    suspend fun getUnreviewedCompletedBookings(studentId: Int): List<Int> = withContext(Dispatchers.IO) {
        try {
            // Get all completed bookings for student
            @Serializable
            data class BookingIdDto(val booking_id: Int)

            val completedBookings = supabase.from("bookings")
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("booking_id")) {
                    filter {
                        eq("student_id", studentId)
                        eq("status", "completed")
                    }
                }
                .decodeList<BookingIdDto>()

            val bookingIds = completedBookings.map { it.booking_id }

            if (bookingIds.isEmpty()) {
                return@withContext emptyList()
            }

            // Get reviewed booking IDs
            @Serializable
            data class ReviewedBookingDto(val booking_id: Int)

            val reviewedBookings = supabase.from("reviews")
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("booking_id")) {
                    filter {
                        isIn("booking_id", bookingIds)
                    }
                }
                .decodeList<ReviewedBookingDto>()

            val reviewedBookingIds = reviewedBookings.map { it.booking_id }.toSet()

            // Return booking IDs that are completed but not reviewed
            bookingIds.filter { it !in reviewedBookingIds }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unreviewed bookings: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get count of new reviews for landlord (created in last 24 hours)
     */
    suspend fun getNewReviewsCount(landlordId: Int): Int = withContext(Dispatchers.IO) {
        try {
            val reviews = supabase.from("reviews")
                .select {
                    filter {
                        eq("landlord_id", landlordId)
                    }
                }
                .decodeList<ReviewDto>()

            // Count reviews from last 24 hours
            val oneDayAgo = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }.time

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

            reviews.count { dto ->
                dto.created_at?.let {
                    try {
                        val createdDate = dateFormat.parse(it)
                        createdDate?.after(oneDayAgo) == true
                    } catch (e: Exception) {
                        false
                    }
                } ?: false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting new reviews count: ${e.message}")
            0
        }
    }

    /**
     * Update property average rating
     */
    private suspend fun updatePropertyAverageRating(propertyId: Int) {
        try {
            val reviews = supabase.from("reviews")
                .select {
                    filter {
                        eq("property_id", propertyId)
                    }
                }
                .decodeList<ReviewDto>()

            if (reviews.isEmpty()) return

            val averageRating = reviews.map { it.property_rating }.average()

            val updateData = buildJsonObject {
                put("rating", averageRating)
            }

            supabase.from("properties")
                .update(updateData) {
                    filter {
                        eq("property_id", propertyId)
                    }
                }

            Log.d(TAG, "Updated property $propertyId average rating to $averageRating")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating property rating: ${e.message}")
        }
    }

    /**
     * Update landlord average rating
     */
    private suspend fun updateLandlordAverageRating(landlordId: Int) {
        try {
            val reviews = supabase.from("reviews")
                .select {
                    filter {
                        eq("landlord_id", landlordId)
                    }
                }
                .decodeList<ReviewDto>()

            if (reviews.isEmpty()) return

            val averageRating = reviews.map { it.landlord_rating }.average()

            val updateData = buildJsonObject {
                put("rating", averageRating)
            }

            supabase.from("landlordprofiles")
                .update(updateData) {
                    filter {
                        eq("landlord_id", landlordId)
                    }
                }

            Log.d(TAG, "Updated landlord $landlordId average rating to $averageRating")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating landlord rating: ${e.message}")
        }
    }

    /**
     * Map ReviewDto to Review model
     */
    private fun mapReviewDtoToReview(dto: ReviewDto): Review {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val createdAt = dto.created_at?.let {
            try {
                dateFormat.parse(it)
            } catch (e: Exception) {
                null
            }
        }

        return Review(
            reviewId = dto.review_id,
            bookingId = dto.booking_id,
            propertyId = dto.property_id,
            studentId = dto.student_id,
            landlordId = dto.landlord_id,
            propertyRating = dto.property_rating,
            landlordRating = dto.landlord_rating,
            comment = dto.comment,
            createdAt = createdAt
        )
    }
}
