package com.example.snapandcook.data.remote

import com.example.snapandcook.data.model.OpenFoodFactsResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json?fields=product_name")
    suspend fun getProduct(@Path("barcode") barcode: String): OpenFoodFactsResponse
}
