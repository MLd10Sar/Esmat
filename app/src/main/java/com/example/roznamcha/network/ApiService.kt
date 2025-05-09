package com.example.roznamcha.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Defines the API endpoints using Retrofit annotations.
 */
interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("/") // The Cloudflare worker script responds to the root path of the BASE_URL
    suspend fun verifyActivationCode(
        @Body request: ActivationRequest
    ): Response<ActivationResponse>
}