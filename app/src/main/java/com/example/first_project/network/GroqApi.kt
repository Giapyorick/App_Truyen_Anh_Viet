package com.example.first_project.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

data class GroqRequest(
    val model: String = "llama-3.1-8b-instant",
    val messages: List<Message>,
    val temperature: Double = 0.7
)

data class Message(
    val role: String,
    val content: String
)

data class GroqResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

interface GroqApi {
    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("chat/completions")
    suspend fun getCompletion(
        @Header("Authorization") token: String,
        @Header("Groq-Organization") orgId: String? = null,
        @Body request: GroqRequest
    ): GroqResponse

    companion object {
        private const val BASE_URL = "https://api.groq.com/openai/v1/"

        fun create(): GroqApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GroqApi::class.java)
        }
    }
}
