package com.meelano.builder.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Repository {
    private var key = ""
    private var api: Api? = null

    /** Cached Retrofit client; rebuilt when server URL or token changes. */
    @Synchronized
    fun apiFor(serverUrl: String, token: String = ""): Api {
        val b = serverUrl.trim().trimEnd('/') + "/"
        val k = "$b|$token"
        if (api == null || k != key) {
            key = k
            val http = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(
                    HttpLoggingInterceptor.Level.BASIC))
                .addInterceptor(Interceptor { chain ->
                    val req = chain.request().newBuilder().apply {
                        if (token.isNotEmpty()) header("X-Builder-Token", token)
                    }.build()
                    chain.proceed(req)
                })
                .build()
            api = Retrofit.Builder()
                .baseUrl(b)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(Api::class.java)
        }
        return api!!
    }

    fun absolute(serverUrl: String, path: String): String {
        if (path.startsWith("http")) return path
        return serverUrl.trim().trimEnd('/') + path
    }

    /** Plain one-shot GET (used by the update checker). */
    fun fetchText(url: String): String {
        val req = Request.Builder().url(url)
            .header("User-Agent", "MeeLano-Builder").build()
        OkHttpClient().newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw Exception("HTTP ${r.code}")
            return r.body?.string() ?: throw Exception("empty body")
        }
    }
}
