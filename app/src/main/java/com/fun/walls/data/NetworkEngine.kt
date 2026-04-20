package com.`fun`.walls.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkEngine {

    // This is the core factory that generates our API connections
    fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}