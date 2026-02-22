package com.example.snapandcook.data.model

import com.google.gson.annotations.SerializedName

data class OpenFoodFactsResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("product") val product: OpenFoodFactsProduct?
)

data class OpenFoodFactsProduct(
    @SerializedName("product_name") val productName: String?
)
