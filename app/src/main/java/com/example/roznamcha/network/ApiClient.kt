package com.example.roznamcha.network

// <<< NO BuildConfig import is needed >>>

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton object to configure and provide a single Retrofit instance for the app.
 * This version DOES NOT use BuildConfig to avoid build errors.
 */
object ApiClient {

    // <<< IMPORTANT: Make sure this is your correct, deployed Worker URL >>>
    private const val BASE_URL = "https://ketabat-activation-backend.hesab.workers.dev/"

    // Create a logger to see network requests/responses in Logcat (for debugging)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // --- REMOVED BuildConfig ---
        // We are hardcoding the logging level to BODY.
        // This means network requests will always be logged, which is useful for testing.
        // Before a final public release, you would change this to Level.NONE.
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor) // Add the logger
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}