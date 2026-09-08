package com.meelano.builder.data

import java.io.File
import java.io.FileOutputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Repository {
    private var base = ""
    private var api: Api? = null

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build()
    }

    @Synchronized
    fun apiFor(serverUrl: String): Api {
        val b = serverUrl.trim().trimEnd('/') + "/"
        if (api == null || b != base) {
            base = b
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

    /** Stream-download with progress callback (0..100, -1 = unknown). */
    fun download(url: String, dest: File, onProgress: (Int) -> Unit) {
        val req = Request.Builder().url(url).build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
            val body = resp.body ?: throw Exception("empty body")
            val total = body.contentLength()
            dest.parentFile?.mkdirs()
            body.byteStream().use { inp ->
                FileOutputStream(dest).use { out ->
                    val buf = ByteArray(32 * 1024)
                    var done = 0L
                    while (true) {
                        val n = inp.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        onProgress(if (total > 0) (done * 100 / total).toInt() else -1)
                    }
                }
            }
        }
        onProgress(100)
    }
}
