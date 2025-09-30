package com.finalapp.accommodationapp.data.repository

import android.util.Log
import com.finalapp.accommodationapp.data.SupabaseClient
import com.finalapp.accommodationapp.data.model.Property
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.*

class PropertyRepository {
    companion object {
        private const val TAG = "PropertyRepository"
    }

    private val supabase = SupabaseClient.client
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

// Replace the getAllProperties method (lines 22-64) with this:

    suspend fun getAllProperties(): List<Property> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching all active properties")

            // Use SimplePropertyDto without joins
            val result = supabase.from("properties")
                .select() {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<SimplePropertyDto>()

            val properties = result.map { dto ->
                Property(
                    propertyId = dto.property_id,
                    title = dto.title,
                    description = dto.description ?: "",
                    propertyType = dto.property_type,
                    address = dto.address,
                    city = dto.city,
                    postalCode = dto.postal_code ?: "",
                    latitude = dto.latitude ?: 0.0,
                    longitude = dto.longitude ?: 0.0,
                    pricePerMonth = dto.price_per_month,
                    bedrooms = dto.bedrooms,
                    bathrooms = dto.bathrooms,
                    totalCapacity = dto.total_capacity,
                    availableFrom = dto.available_from?.let {
                        try { dateFormat.parse(it) } catch (e: Exception) { null }
                    },
                    availableTo = dto.available_to?.let {
                        try { dateFormat.parse(it) } catch (e: Exception) { null }
                    },
                    isActive = dto.is_active ?: true,
                    landlordName = "",
                    landlordPhone = "",
                    landlordRating = 0.0,
                    companyName = null
                )
            }

            Log.d(TAG, "Loaded ${properties.size} properties")
            properties
        } catch (e: Exception) {
            Log.e(TAG, "Error loading properties", e)
            emptyList()
        }
    }

    suspend fun getPropertyById(propertyId: Int): Property? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching property with ID: $propertyId")

            // Simple query without joins to avoid parsing errors
            val result = supabase.from("properties")
                .select() {
                    filter {
                        eq("property_id", propertyId)
                    }
                }
                .decodeSingleOrNull<SimplePropertyDto>()

            Log.d(TAG, "Property query result: $result")

            result?.let { dto ->
                Property(
                    propertyId = dto.property_id,
                    title = dto.title,
                    description = dto.description ?: "",
                    propertyType = dto.property_type,
                    address = dto.address,
                    city = dto.city,
                    postalCode = dto.postal_code ?: "",
                    latitude = dto.latitude ?: 0.0,
                    longitude = dto.longitude ?: 0.0,
                    pricePerMonth = dto.price_per_month,
                    bedrooms = dto.bedrooms,
                    bathrooms = dto.bathrooms,
                    totalCapacity = dto.total_capacity,
                    availableFrom = dto.available_from?.let {
                        try { dateFormat.parse(it) } catch (e: Exception) { null }
                    },
                    availableTo = dto.available_to?.let {
                        try { dateFormat.parse(it) } catch (e: Exception) { null }
                    },
                    isActive = dto.is_active ?: true,
                    // Set defaults for landlord info
                    landlordName = "",
                    landlordPhone = "",
                    landlordRating = 0.0,
                    companyName = null
                )
            }.also {
                if (it != null) {
                    Log.d(TAG, "Successfully loaded property: ${it.title}")
                } else {
                    Log.e(TAG, "Property not found for ID: $propertyId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading property $propertyId: ${e.message}", e)
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
            val updates = buildJsonObject {
                put("title", title)
                put("description", description)
                put("property_type", propertyType)
                put("address", address)
                put("city", city)
                put("postal_code", postalCode)
                put("price_per_month", pricePerMonth)
                put("bedrooms", bedrooms)
                put("bathrooms", bathrooms)
                put("total_capacity", totalCapacity)
                put("available_from", availableFrom)
                availableTo?.let { put("available_to", it) }
            }

            supabase.from("properties")
                .update(updates) {
                    filter {
                        eq("property_id", propertyId)
                    }
                }

            Log.d(TAG, "Updated property $propertyId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating property", e)
            false
        }
    }

    suspend fun deleteProperty(propertyId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // Supabase should handle cascading deletes if configured properly
            supabase.from("properties")
                .delete {
                    filter {
                        eq("property_id", propertyId)
                    }
                }

            Log.d(TAG, "Deleted property $propertyId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting property", e)
            false
        }
    }

    suspend fun updatePropertyAmenities(propertyId: Int, amenityIds: List<Int>): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete existing amenities
            supabase.from("propertyamenities")
                .delete {
                    filter {
                        eq("property_id", propertyId)
                    }
                }

            // Insert new amenities
            if (amenityIds.isNotEmpty()) {
                val amenities = amenityIds.map { amenityId ->
                    buildJsonObject {
                        put("property_id", propertyId)
                        put("amenity_id", amenityId)
                    }
                }

                supabase.from("propertyamenities")
                    .insert(amenities)
            }

            Log.d(TAG, "Updated amenities for property $propertyId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating property amenities", e)
            false
        }
    }

    suspend fun getPropertyAmenities(propertyId: Int): List<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching amenities for property: $propertyId")

            val result = supabase.from("propertyamenities")
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("amenity_id")) {
                    filter {
                        eq("property_id", propertyId)
                    }
                }
                .decodeList<PropertyAmenityDto>()

            val amenityIds = result.map { it.amenity_id }
            Log.d(TAG, "Found ${amenityIds.size} amenities for property $propertyId: $amenityIds")
            amenityIds
        } catch (e: Exception) {
            Log.e(TAG, "Error loading property amenities: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getPropertyAmenitiesAsStrings(propertyId: Int): List<String> = withContext(Dispatchers.IO) {
        try {
            // First get the amenity IDs
            val amenityIds = getPropertyAmenities(propertyId)

            if (amenityIds.isEmpty()) {
                return@withContext emptyList()
            }

            // Then fetch the amenity names separately
            val amenities = supabase.from("amenities")
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("amenity_id", "name")) {
                    filter {
                        isIn("amenity_id", amenityIds)
                    }
                }
                .decodeList<AmenitySimpleDto>()

            amenities.map { it.name }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading amenities for property $propertyId: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun updatePropertyStatus(propertyId: Int, isActive: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val updates = buildJsonObject {
                put("is_active", isActive)
            }

            supabase.from("properties")
                .update(updates) {
                    filter {
                        eq("property_id", propertyId)
                    }
                }

            Log.d(TAG, "Updated property $propertyId status to: $isActive")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating property status", e)
            false
        }
    }

    suspend fun getLandlordIdByPropertyId(propertyId: Int): Int? = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("properties")
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("landlord_id")) {
                    filter {
                        eq("property_id", propertyId)
                    }
                }
                .decodeSingleOrNull<LandlordIdDto>()

            result?.landlord_id
        } catch (e: Exception) {
            Log.e(TAG, "Error getting landlord ID for property $propertyId", e)
            null
        }
    }

    suspend fun getPropertiesByLandlordId(landlordId: Int): List<Property> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching properties for landlordId: $landlordId")

            // Simple query without joins to avoid parsing errors
            val result = supabase.from("properties")
                .select() {
                    filter {
                        eq("landlord_id", landlordId)
                    }
                }
                .decodeList<SimplePropertyDto>()

            Log.d(TAG, "Found ${result.size} properties for landlord $landlordId")

            result.map { dto ->
                Property(
                    propertyId = dto.property_id,
                    title = dto.title,
                    description = dto.description ?: "",
                    propertyType = dto.property_type,
                    address = dto.address,
                    city = dto.city,
                    postalCode = dto.postal_code ?: "",
                    latitude = dto.latitude ?: 0.0,
                    longitude = dto.longitude ?: 0.0,
                    pricePerMonth = dto.price_per_month,
                    bedrooms = dto.bedrooms,
                    bathrooms = dto.bathrooms,
                    totalCapacity = dto.total_capacity,
                    availableFrom = dto.available_from?.let {
                        try { dateFormat.parse(it) } catch (e: Exception) { null }
                    },
                    availableTo = dto.available_to?.let {
                        try { dateFormat.parse(it) } catch (e: Exception) { null }
                    },
                    isActive = dto.is_active ?: true,
                    // Since this is the landlord's own properties, we can use placeholder values
                    // or fetch the landlord info separately if needed
                    landlordName = "Your Property",
                    landlordPhone = "",
                    landlordRating = 0.0,
                    companyName = null
                )
            }.also {
                Log.d(TAG, "Successfully mapped ${it.size} properties")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading properties for landlord $landlordId: ${e.message}", e)
            emptyList()
        }
    }
}

// DTOs
@Serializable
data class PropertyDto(
    val property_id: Int,
    val landlord_id: Int,
    val title: String,
    val description: String? = null,
    val property_type: String,
    val address: String,
    val city: String,
    val postal_code: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val price_per_month: Double,
    val bedrooms: Int,
    val bathrooms: Int,
    val total_capacity: Int,
    val available_from: String? = null,
    val available_to: String? = null,
    val is_active: Boolean? = true,
    val landlords: LandlordInfoDto? = null
)

@Serializable
data class SimplePropertyDto(
    val property_id: Int,
    val landlord_id: Int,
    val title: String,
    val description: String? = null,
    val property_type: String,
    val address: String,
    val city: String,
    val postal_code: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val price_per_month: Double,
    val bedrooms: Int,
    val bathrooms: Int,
    val total_capacity: Int,
    val available_from: String? = null,
    val available_to: String? = null,
    val is_active: Boolean? = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class AmenitySimpleDto(
    val amenity_id: Int,
    val name: String
)

@Serializable
data class LandlordInfoDto(
    val first_name: String,
    val last_name: String,
    val phone: String,
    val rating: Double? = null,
    val company_name: String? = null
)

@Serializable
data class PropertyAmenityDto(val amenity_id: Int)

@Serializable
data class PropertyAmenityWithNameDto(
    val amenities: AmenityNameDto? = null
)

@Serializable
data class AmenityNameDto(val name: String)

@Serializable
data class LandlordIdDto(val landlord_id: Int)