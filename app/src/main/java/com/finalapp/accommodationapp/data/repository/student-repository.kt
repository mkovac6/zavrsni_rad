package com.finalapp.accommodationapp.data.repository

import android.util.Log
import com.finalapp.accommodationapp.data.DatabaseConnection
import com.finalapp.accommodationapp.data.model.StudentProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Date

class StudentRepository {
    companion object {
        private const val TAG = "StudentRepository"
    }
    
    suspend fun createStudentProfile(
        userId: Int,
        universityId: Int,
        firstName: String,
        lastName: String,
        phone: String,
        studentNumber: String?,
        yearOfStudy: Int?,
        program: String?,
        preferredMoveInDate: String?,
        budgetMin: Double?,
        budgetMax: Double?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            
            // First check if profile already exists
            val checkQuery = "SELECT COUNT(*) as count FROM Students WHERE user_id = ?"
            val checkStmt = connection?.prepareStatement(checkQuery)
            checkStmt?.setInt(1, userId)
            val resultSet = checkStmt?.executeQuery()
            
            if (resultSet?.next() == true && resultSet.getInt("count") > 0) {
                Log.d(TAG, "Student profile already exists for user $userId")
                resultSet.close()
                checkStmt?.close()
                connection?.close()
                return@withContext false
            }
            
            resultSet?.close()
            checkStmt?.close()
            
            // Insert new student profile
            val insertQuery = """
                INSERT INTO Students (
                    user_id, university_id, first_name, last_name, phone,
                    student_number, year_of_study, program, preferred_move_in_date,
                    budget_min, budget_max
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            
            val preparedStatement = connection?.prepareStatement(insertQuery)
            preparedStatement?.apply {
                setInt(1, userId)
                setInt(2, universityId)
                setString(3, firstName)
                setString(4, lastName)
                setString(5, phone)
                setString(6, studentNumber)
                if (yearOfStudy != null) setInt(7, yearOfStudy) else setNull(7, java.sql.Types.INTEGER)
                setString(8, program)
                if (preferredMoveInDate != null) setDate(9, Date.valueOf(preferredMoveInDate)) else setNull(9, java.sql.Types.DATE)
                if (budgetMin != null) setDouble(10, budgetMin) else setNull(10, java.sql.Types.DECIMAL)
                if (budgetMax != null) setDouble(11, budgetMax) else setNull(11, java.sql.Types.DECIMAL)
            }
            
            val rowsAffected = preparedStatement?.executeUpdate() ?: 0
            preparedStatement?.close()
            
            // Update user profile completion status
            if (rowsAffected > 0) {
                val updateUserQuery = "UPDATE Users SET is_profile_complete = 1 WHERE user_id = ?"
                val updateStmt = connection?.prepareStatement(updateUserQuery)
                updateStmt?.setInt(1, userId)
                updateStmt?.executeUpdate()
                updateStmt?.close()
                
                Log.d(TAG, "Student profile created successfully for user $userId")
            }
            
            connection?.close()
            rowsAffected > 0
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating student profile: ${e.message}", e)
            false
        }
    }
    
    suspend fun getStudentProfile(userId: Int): StudentProfile? = withContext(Dispatchers.IO) {
        try {
            val connection = DatabaseConnection.getConnection()
            val query = """
                SELECT s.*, u.name as university_name
                FROM Students s
                JOIN Universities u ON s.university_id = u.university_id
                WHERE s.user_id = ?
            """.trimIndent()
            
            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.setInt(1, userId)
            val resultSet = preparedStatement?.executeQuery()
            
            var profile: StudentProfile? = null
            if (resultSet?.next() == true) {
                profile = StudentProfile(
                    studentId = resultSet.getInt("student_id"),
                    userId = resultSet.getInt("user_id"),
                    universityId = resultSet.getInt("university_id"),
                    universityName = resultSet.getString("university_name"),
                    firstName = resultSet.getString("first_name"),
                    lastName = resultSet.getString("last_name"),
                    phone = resultSet.getString("phone"),
                    studentNumber = resultSet.getString("student_number"),
                    yearOfStudy = resultSet.getInt("year_of_study"),
                    program = resultSet.getString("program"),
                    budgetMin = resultSet.getDouble("budget_min"),
                    budgetMax = resultSet.getDouble("budget_max")
                )
            }
            
            resultSet?.close()
            preparedStatement?.close()
            connection?.close()
            
            profile
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching student profile: ${e.message}", e)
            null
        }
    }
}