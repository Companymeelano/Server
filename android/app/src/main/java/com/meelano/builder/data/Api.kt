package com.meelano.builder.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface Api {
    @GET("api/v1/health")
    suspend fun health(): Health

    @GET("api/v1/templates")
    suspend fun templates(): TemplatesResp

    @POST("api/v1/jobs")
    suspend fun createJob(@Body body: CreateJobReq): Job

    @GET("api/v1/jobs")
    suspend fun jobs(@Query("limit") limit: Int = 50): JobsResp

    @GET("api/v1/jobs/{id}")
    suspend fun job(@Path("id") id: String): Job
}
