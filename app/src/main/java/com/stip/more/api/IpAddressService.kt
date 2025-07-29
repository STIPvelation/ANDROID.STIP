package com.stip.stip.more.api

import retrofit2.Call
import retrofit2.http.GET

data class IpInfoResponse(
    val ip: String,
    val country_code: String,
    val country_name: String,
    val region_code: String,
    val region_name: String,
    val city: String,
    val zip_code: String,
    val time_zone: String,
    val latitude: Double,
    val longitude: Double
)

interface IpAddressService {
    @GET("https://ipapi.co/json/")
    fun getIpInfo(): Call<IpInfoResponse>
}
