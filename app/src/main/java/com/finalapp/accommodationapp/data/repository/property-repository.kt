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
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

class PropertyRepository {
    companion object {
        private const val TAG = "PropertyRepository"
    }

    private val supabase = SupabaseClient.client
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun getAllProperties(includeInactive: Boolean = false): List<Property> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, if (includeInactive) "Fetching all properties (including inactive)" else "Fetching all active properties")

            // First fetch properties based on includeInactive flag
            val properties = if (includeInactive) {
                supabase.from("properties")
                    .select()
                    .decodeList<SimplePropertyDto>()
            } else {
                supabase.from("properties")
                    .select() {
                        filter {
                            eq("is_active", true)
                        }
                    }
                    .decodeList<SimplePropertyDto>()
            }

            // Get unique landlord IDs
            val landlordIds = properties.map { it.landlord_id }.distinct()

            // Fetch landlord information
            val landlordMap = if (landlordIds.isNotEmpty()) {
                val landlords = supabase.from("landlords")
                    .select() {
                        filter {
                            isIn("landlord_id", landlordIds)
                        }
                    }
                    .decodeList<SimpleLandlordDto>()

                landlords.associateBy { it.landlord_id }
            } else {
                emptyMap()
            }

            // Map properties with landlord info
            val result = properties.map { dto ->
                val landlord = landlordMap[dto.landlord_id]
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
                    landlordName = if (landlord != null) {
                        "${landlord.first_name} ${landlord.last_name}"
                    } else {
                        "Landlord"
                    },
                    landlordPhone = landlord?.phone ?: "",
                    landlordRating = landlord?.rating ?: 0.0,
                    companyName = landlord?.company_name
                )
            }

            Log.d(TAG, "Loaded ${result.size} properties with landlord info")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error loading properties", e)
            emptyList()
        }
    }

    suspend fun getPropertyById(propertyId: Int): Property? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching property with ID: $propertyId")

            // Fetch property
            val property = supabase.from("properties")
                .select() {
                    filter {
                        eq("property_id", propertyId)
                    }
                }
                .decodeSingleOrNull<SimplePropertyDto>()

            if (property == null) {
                Log.e(TAG, "Property not found for ID: $propertyId")
                return@withContext null
            }

            // Fetch landlord info
            val landlord = try {
                supabase.from("landlords")
                    .select() {
                        filter {
                            eq("landlord_id", property.landlord_id)
                        }
                    }
                    .decodeSingleOrNull<SimpleLandlordDto>()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching landlord info: ${e.message}")
                null
            }

            Property(
                propertyId = property.property_id,
                title = property.title,
                description = property.description ?: "",
                propertyType = property.property_type,
                address = property.address,
                city = property.city,
                postalCode = property.postal_code ?: "",
                latitude = property.latitude ?: 0.0,
                longitude = property.longitude ?: 0.0,
                pricePerMonth = property.price_per_month,
                bedrooms = property.bedrooms,
                bathrooms = property.bathrooms,
                totalCapacity = property.total_capacity,
                availableFrom = property.available_from?.let {
                    try { dateFormat.parse(it) } catch (e: Exception) { null }
                },
                availableTo = property.available_to?.let {
                    try { dateFormat.parse(it) } catch (e: Exception) { null }
                },
                isActive = property.is_active ?: true,
                landlordName = if (landlord != null) {
                    "${landlord.first_name} ${landlord.last_name}"
                } else {
                    "Landlord"
                },
                landlordPhone = landlord?.phone ?: "",
                landlordRating = landlord?.rating ?: 0.0,
                companyName = landlord?.company_name
            ).also {
                Log.d(TAG, "Successfully loaded property: ${it.title} with landlord: ${it.landlordName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading property $propertyId: ${e.message}", e)
            null
        }
    }

    suspend fun geocodeAddress(address: String, city: String): Pair<Double, Double> {
        return withContext(Dispatchers.IO) {
            try {
                // Create full address
                val fullAddress = "$address, $city, Croatia"
                val encodedAddress = URLEncoder.encode(fullAddress, "UTF-8")

                val apiKey = "AIzaSyDR8rQvnIoGF7igmA4C_P1dlFnD6lUhveE"
                val url = "https://maps.googleapis.com/maps/api/geocode/json?address=$encodedAddress&key=$apiKey"

                val response = URL(url).readText()
                val jsonObject = JSONObject(response)

                val status = jsonObject.getString("status")
                if (status == "OK") {
                    val results = jsonObject.getJSONArray("results")
                    if (results.length() > 0) {
                        val location = results.getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONObject("location")

                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")

                        return@withContext Pair(lat, lng)
                    }
                }

                // If API fails or no results, use fallback
                getCityCoordinatesFallback(city)

            } catch (e: Exception) {
                // If any error occurs, use fallback coordinates
                getCityCoordinatesFallback(city)
            }
        }
    }

    private fun getCityCoordinatesFallback(city: String): Pair<Double, Double> {
        return when(city.lowercase()) {
            "zagreb" -> Pair(45.8150, 15.9819)
            "split" -> Pair(43.5081, 16.4402)
            "rijeka" -> Pair(45.3271, 14.4422)
            "osijek" -> Pair(45.5550, 18.6955)
            "zadar" -> Pair(44.1194, 15.2314)
            "pula" -> Pair(44.8666, 13.8496)
            "varaždin" -> Pair(46.3044, 16.3366)
            "dubrovnik" -> Pair(42.6507, 18.0944)
            "karlovac" -> Pair(45.4929, 15.5553)
            "sisak" -> Pair(45.4616, 16.3783)
            else -> Pair(45.8150, 15.9819) //Zagreb
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

            // Fetch properties
            val properties = supabase.from("properties")
                .select() {
                    filter {
                        eq("landlord_id", landlordId)
                    }
                }
                .decodeList<SimplePropertyDto>()

            // Get landlord info
            val landlord = try {
                supabase.from("landlords")
                    .select() {
                        filter {
                            eq("landlord_id", landlordId)
                        }
                    }
                    .decodeSingleOrNull<SimpleLandlordDto>()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching landlord info: ${e.message}")
                null
            }

            Log.d(TAG, "Found ${properties.size} properties for landlord $landlordId")

            properties.map { dto ->
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
                    landlordName = if (landlord != null) {
                        "${landlord.first_name} ${landlord.last_name}"
                    } else {
                        "Your Property"
                    },
                    landlordPhone = landlord?.phone ?: "",
                    landlordRating = landlord?.rating ?: 0.0,
                    companyName = landlord?.company_name
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
data class SimpleLandlordDto(
    val landlord_id: Int,
    val user_id: Int? = null,
    val first_name: String,
    val last_name: String,
    val phone: String,
    val is_verified: Boolean? = null,  // Added this field
    val rating: Double? = null,
    val company_name: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class AmenitySimpleDto(
    val amenity_id: Int,
    val name: String
)

@Serializable
data class PropertyAmenityDto(val amenity_id: Int)

@Serializable
data class LandlordIdDto(val landlord_id: Int)