package com.stargallery.app.data.remote

import android.content.Context
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Builds the OkHttp/Retrofit stack.
 *
 * Security notes:
 *  - There is deliberately NO logging interceptor, so credentials never reach Logcat.
 *  - Credentials are attached only here, only when [TmdbConfig] contains them, and
 *    never when a backend proxy is configured.
 */
object NetworkModule {

    private class AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            if (TmdbConfig.usesProxy) return chain.proceed(original)

            val builder = original.newBuilder().header("Accept", "application/json")
            val token = TmdbConfig.API_READ_ACCESS_TOKEN.trim()
            val key = TmdbConfig.API_KEY.trim()
            when {
                token.isNotEmpty() -> builder.header("Authorization", "Bearer $token")
                key.isNotEmpty() -> {
                    val url = original.url.newBuilder().addQueryParameter("api_key", key).build()
                    builder.url(url)
                }
            }
            return chain.proceed(builder.build())
        }
    }

    fun provideApi(context: Context): TmdbApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .cache(Cache(File(context.cacheDir, "http_cache"), 10L * 1024 * 1024))
            .addInterceptor(AuthInterceptor())
            .build()

        return Retrofit.Builder()
            .baseUrl(TmdbConfig.baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }
}
