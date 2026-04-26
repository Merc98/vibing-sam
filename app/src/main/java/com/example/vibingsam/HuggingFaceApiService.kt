package com.example.vibingsam

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface HuggingFaceApiService {
    @GET("models/{model_id}")
    suspend fun getModel(@Path("model_id") modelId: String): Response<String>

    @Streaming
    @GET("resolve/main/{model_id}/{file}")
    suspend fun downloadModelFile(@Path("model_id") modelId: String, @Path("file") file: String): ResponseBody
}

object ApiClient {
    val service: HuggingFaceApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://huggingface.co/")
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(HuggingFaceApiService::class.java)
    }
}