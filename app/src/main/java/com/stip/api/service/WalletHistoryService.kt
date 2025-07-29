package com.stip.api.service

import com.stip.api.model.WalletHistoryRecord
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface WalletHistoryService {
    @GET("api/wallet/history")
    suspend fun getWalletHistory(
        @Header("Authorization") authorization: String,
        @Query("marketPairId") marketPairId: String? = null,
        @Query("type") type: String? = null, // DEPOSIT(입금) / WITHDRAW(출금)
        @Query("status") status: String? = null // REQUEST(진행 중) / REJECTED(반환)
    ): List<WalletHistoryRecord>
} 