package com.netatmo.gitlabplugin.retrofit

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor


/**
 * Get okHttp client with OAuth interceptor
 */

fun okHttpClient(token: String): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(OAuthInterceptor(token))
        .addInterceptor(loggingInterceptor())
        .build()
}

private class OAuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val newBuilder = request.newBuilder()
        newBuilder.header("Authorization", "Bearer $token")
        request = newBuilder.build()
        return chain.proceed(request)
    }
}

private fun loggingInterceptor(): Interceptor {
    return HttpLoggingInterceptor { message -> println("OKHttp: $message") }.apply {
        setLevel(HttpLoggingInterceptor.Level.BASIC)
    }
}