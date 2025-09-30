package com.finalapp.accommodationapp.config

object AppConfig {
    // Toggle this to switch between local and Supabase
    const val USE_SUPABASE = false // Set to true when ready to test Supabase
    
    // You can also use BuildConfig for different build variants
    fun isDevelopment(): Boolean = true
    fun isProduction(): Boolean = false
}