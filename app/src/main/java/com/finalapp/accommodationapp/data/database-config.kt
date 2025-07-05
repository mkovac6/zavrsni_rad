package com.finalapp.accommodationapp.data

object DatabaseConfig {
    const val SERVER = "10.0.2.2"
    const val DATABASE = "Accommodation"
    const val USERNAME = "sa"
    const val PASSWORD = "Student@2025!"
    const val PORT = "1433"
    const val INSTANCE = "SQLEXPRESS"

    // Try this format with instance name
    val CONNECTION_URL = "jdbc:jtds:sqlserver://$SERVER:$PORT;databaseName=$DATABASE;instance=$INSTANCE;user=$USERNAME;password=$PASSWORD"
}