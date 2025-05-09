package com.example.roznamcha.network

import com.google.gson.annotations.SerializedName

/**
 * Defines the structure of the JSON data sent to and received from the activation server.
 */

data class ActivationRequest(
    @SerializedName("activationCode")
    val activationCode: String
)

data class ActivationResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("message")
    val message: String?
)