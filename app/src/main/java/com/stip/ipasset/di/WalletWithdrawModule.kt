package com.stip.ipasset.di

import android.util.Log
import com.stip.stip.api.RetrofitClient
import com.stip.ipasset.api.WalletWithdrawService
import com.stip.ipasset.repository.WalletWithdrawRepository
import com.stip.ipasset.repository.WalletWithdrawRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 출금 관련 DI 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WalletWithdrawModule {

    /**
     * 출금 리포지토리 구현체 바인딩
     */
    @Binds
    @Singleton
    abstract fun bindWalletWithdrawRepository(
        walletWithdrawRepositoryImpl: WalletWithdrawRepositoryImpl
    ): WalletWithdrawRepository

    companion object {
        /**
         * 출금 API
         */
        @Provides
        @Singleton
        fun provideWalletWithdrawService(): WalletWithdrawService {
            return try {
                val service = RetrofitClient.createTapiService(WalletWithdrawService::class.java)
                service
            } catch (e: Exception) {
                throw e
            }
        }
    }
} 