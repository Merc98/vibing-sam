package com.example.vibingsam

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface HuggingFaceApiService {
    @GET("models/{model_id}")
    suspend fun getModel(@Path("model_id") modelId: String): Response<String>
}

object ApiClient {
    val service: HuggingFaceApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://huggingface.co/api/")
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(HuggingFaceApiService::class.java)
    }
}