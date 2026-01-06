package com.finalapp.accommodationapp.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.finalapp.accommodationapp.data.SupabaseClient
import com.finalapp.accommodationapp.data.model.PropertyImage
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class PropertyImageRepository(private val context: Context) {
    companion object {
        private const val TAG = "PropertyImageRepository"
        private const val BUCKET_NAME = "property-images"
        private const val MAX_IMAGE_SIZE_MB = 5
        private const val MAX_IMAGES_PER_PROPERTY = 10
    }

    private val supabase = SupabaseClient.client
    private val storage = supabase.storage

    @Serializable
    data class PropertyImageDto(
        val image_id: Int,
        val property_id: Int,
        val image_url: String,
        val is_primary: Boolean? = false,
        val display_order: Int? = 0,
        val uploaded_at: String? = null
    )

    /**
     * Upload a single image for a property
     * Returns the public URL if successful, null otherwise
     */
    suspend fun uploadPropertyImage(
        propertyId: Int,
        imageUri: Uri,
        isPrimary: Boolean = false,
        displayOrder: Int = 0
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Check current image count
            val currentImages = getPropertyImages(propertyId)
            if (currentImages.size >= MAX_IMAGES_PER_PROPERTY) {
                Log.e(TAG, "Property $propertyId already has maximum images")
                return@withContext null
            }

            // Generate unique filename
            val timestamp = System.currentTimeMillis()
            val uuid = UUID.randomUUID().toString().substring(0, 8)
            val extension = getFileExtension(context, imageUri)
            val fileName = "${propertyId}/${timestamp}_${uuid}.${extension}"

            // Read file bytes
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes == null || bytes.isEmpty()) {
                Log.e(TAG, "Failed to read image bytes")
                return@withContext null
            }

            // Check file size
            val sizeInMB = bytes.size / (1024.0 * 1024.0)
            if (sizeInMB > MAX_IMAGE_SIZE_MB) {
                Log.e(TAG, "Image too large: ${sizeInMB}MB (max ${MAX_IMAGE_SIZE_MB}MB)")
                return@withContext null
            }

            // Upload to Supabase Storage
            storage.from(BUCKET_NAME).upload(fileName, bytes)

            // Get public URL
            val publicUrl = storage.from(BUCKET_NAME).publicUrl(fileName)

            // Insert metadata into database
            val insertDto = buildJsonObject {
                put("property_id", propertyId)
                put("image_url", publicUrl)
                put("is_primary", isPrimary)
                put("display_order", displayOrder)
            }

            supabase.from("propertyimages").insert(insertDto)

            Log.d(TAG, "Uploaded image for property $propertyId: $publicUrl")
            publicUrl

        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image: ${e.message}", e)
            null
        }
    }

    /**
     * Upload multiple images for a property
     * Returns list of successful upload URLs
     */
    suspend fun uploadMultipleImages(
        propertyId: Int,
        imageUris: List<Uri>,
        startOrder: Int = 0
    ): List<String> = withContext(Dispatchers.IO) {
        val uploadedUrls = mutableListOf<String>()

        imageUris.forEachIndexed { index, uri ->
            val url = uploadPropertyImage(
                propertyId = propertyId,
                imageUri = uri,
                isPrimary = (index == 0 && startOrder == 0), // First image is primary
                displayOrder = startOrder + index
            )
            if (url != null) {
                uploadedUrls.add(url)
            }
        }

        uploadedUrls
    }

    /**
     * Get all images for a property, ordered by display_order
     */
    suspend fun getPropertyImages(propertyId: Int): List<PropertyImage> = withContext(Dispatchers.IO) {
        try {
            val images = supabase.from("propertyimages")
                .select() {
                    filter {
                        eq("property_id", propertyId)
                    }
                }
                .decodeList<PropertyImageDto>()

            // Sort by display_order
            images.sortedBy { it.display_order ?: 0 }.map { dto ->
                PropertyImage(
                    imageId = dto.image_id,
                    propertyId = dto.property_id,
                    imageUrl = dto.image_url,
                    isPrimary = dto.is_primary ?: false,
                    displayOrder = dto.display_order ?: 0,
                    uploadedAt = dto.uploaded_at
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading images for property $propertyId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Delete a specific image
     */
    suspend fun deletePropertyImage(imageId: Int, imageUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Extract file path from URL
            val filePath = extractFilePathFromUrl(imageUrl)
            if (filePath.isNotEmpty()) {
                // Delete from storage
                storage.from(BUCKET_NAME).delete(filePath)
            }

            // Delete from database
            supabase.from("propertyimages")
                .delete {
                    filter {
                        eq("image_id", imageId)
                    }
                }

            Log.d(TAG, "Deleted image $imageId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image $imageId: ${e.message}")
            false
        }
    }

    /**
     * Delete all images for a property (used when deleting property)
     */
    suspend fun deleteAllPropertyImages(propertyId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val images = getPropertyImages(propertyId)

            // Delete from storage
            images.forEach { image ->
                val filePath = extractFilePathFromUrl(image.imageUrl)
                if (filePath.isNotEmpty()) {
                    try {
                        storage.from(BUCKET_NAME).delete(filePath)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting file $filePath: ${e.message}")
                    }
                }
            }

            // Delete from database
            supabase.from("propertyimages")
                .delete {
                    filter {
                        eq("property_id", propertyId)
                    }
                }

            Log.d(TAG, "Deleted all images for property $propertyId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting images for property $propertyId: ${e.message}")
            false
        }
    }

    /**
     * Set primary image for a property
     */
    suspend fun setPrimaryImage(propertyId: Int, imageId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // Unset all primary flags for this property
            val unsetUpdate = buildJsonObject {
                put("is_primary", false)
            }
            supabase.from("propertyimages")
                .update(unsetUpdate) {
                    filter {
                        eq("property_id", propertyId)
                    }
                }

            // Set new primary
            val setPrimaryUpdate = buildJsonObject {
                put("is_primary", true)
            }
            supabase.from("propertyimages")
                .update(setPrimaryUpdate) {
                    filter {
                        eq("image_id", imageId)
                    }
                }

            Log.d(TAG, "Set image $imageId as primary for property $propertyId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting primary image: ${e.message}")
            false
        }
    }

    /**
     * Reorder images for a property
     */
    suspend fun reorderImages(imageOrderMap: Map<Int, Int>): Boolean = withContext(Dispatchers.IO) {
        try {
            imageOrderMap.forEach { (imageId, newOrder) ->
                val update = buildJsonObject {
                    put("display_order", newOrder)
                }
                supabase.from("propertyimages")
                    .update(update) {
                        filter {
                            eq("image_id", imageId)
                        }
                    }
            }
            Log.d(TAG, "Reordered ${imageOrderMap.size} images")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error reordering images: ${e.message}")
            false
        }
    }

    // Helper functions
    private fun extractFilePathFromUrl(url: String): String {
        // Extract path after /storage/v1/object/public/property-images/
        val prefix = "/storage/v1/object/public/$BUCKET_NAME/"
        return if (url.contains(prefix)) {
            url.substringAfter(prefix)
        } else {
            ""
        }
    }

    private fun getFileExtension(context: Context, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        return when (mimeType) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    }
}
