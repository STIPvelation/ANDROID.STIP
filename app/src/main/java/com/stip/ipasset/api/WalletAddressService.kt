package com.stip.ipasset.api

import com.stip.ipasset.model.WalletAddressResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WalletAddressService {
    @GET("api/wallet/address")
    suspend fun getWalletAddress(
        @Query("memberId") memberId: String,
        @Query("symbol") symbol: String
    ): WalletAddressResponse
} 