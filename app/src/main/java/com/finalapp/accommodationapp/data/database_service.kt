package com.finalapp.accommodationapp.data

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

interface DatabaseService {
    suspend fun getConnection(): Any?
    suspend fun getUniversities(): List<String>
}

class LocalDatabaseService : DatabaseService {
    override suspend fun getConnection() = DatabaseConnection.getConnection()
    override suspend fun getUniversities() = DatabaseConnection.getUniversities()
}

class SupabaseDatabaseService : DatabaseService {
    override suspend fun getConnection() = SupabaseClient.client
    override suspend fun getUniversities(): List<String> {
        return try {
            val response = SupabaseClient.client
                .from("universities")
                .select {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<UniversityDto>()

            response.map { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@Serializable
data class UniversityDto(val name: String)