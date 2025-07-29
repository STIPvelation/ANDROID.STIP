package com.stip.api.repository

import android.util.Log
import com.stip.stip.api.RetrofitClient
import com.stip.api.model.PortfolioAsset
import com.stip.api.model.PortfolioResponse
import com.stip.api.model.PortfolioWalletItemDto
import com.stip.api.service.PortfolioService
import com.stip.ipasset.api.WalletAddressService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * 포트폴리오 데이터 처리를 위한 Repository
 * API 통신을 통해 포트폴리오 데이터를 가져오고 앱에서 사용할 수 있는 형태로 제공
 */
class PortfolioRepository {
    
    private val portfolioService: PortfolioService by lazy {
        RetrofitClient.createTapiService(PortfolioService::class.java)
    }
    
    private val walletAddressService: WalletAddressService by lazy {
        RetrofitClient.createTapiService(WalletAddressService::class.java)
    }

    /**
     * 사용자의 포트폴리오 데이터 조회
     * @param userId 사용자 ID
     * @return 포트폴리오 응답
     */
    suspend fun getPortfolioResponse(userId: String): PortfolioResponse? = withContext(Dispatchers.IO) {
        try {
            Log.d("PortfolioRepository", "포트폴리오 API 호출 시작: userId=$userId")
            
            val response = portfolioService.getPortfolio(userId)
            
            Log.d("PortfolioRepository", "API 응답 수신:")
            Log.d("PortfolioRepository", "wallets 개수: ${response.wallets.size}")
            Log.d("PortfolioRepository", "usdBalance: ${response.usdBalance}")
            Log.d("PortfolioRepository", "totalEval: ${response.totalEval}")
            
            if (response.wallets.isNotEmpty()) {
                response.wallets.forEachIndexed { index, asset ->
                    Log.d("PortfolioRepository", "자산 ${index + 1}: ${asset.symbol} = ${asset.balance} (available: ${asset.availableBalance})")
                }
                
                Log.d("PortfolioRepository", "포트폴리오 API 성공 - ${response.wallets.size}개 자산 반환")
            } else {
                Log.w("PortfolioRepository", "포트폴리오 API 빈 데이터")
            }
            
            response
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "포트폴리오 API 호출 예외 발생: ${e.message}", e)
            null
        }
    }

    /**
     * 사용자의 포트폴리오 지갑 목록 조회
     * @param userId 사용자 ID
     * @return 포트폴리오 지갑 아이템 목록
     */
    suspend fun getPortfolioWallets(userId: String): List<PortfolioWalletItemDto> = withContext(Dispatchers.IO) {
        try {
            val response = getPortfolioResponse(userId)
            response?.wallets ?: emptyList()
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "포트폴리오 지갑 조회 실패: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 사용자의 총 보유자산 조회 (USD 잔고)
     * @param userId 사용자 ID
     * @return USD 잔고 (usdBalance)
     */
    suspend fun getUsdBalance(userId: String): BigDecimal = withContext(Dispatchers.IO) {
        try {
            val response = getPortfolioResponse(userId)
            response?.usdBalance ?: BigDecimal.ZERO
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "USD 잔고 조회 실패: ${e.message}", e)
            BigDecimal.ZERO
        }
    }

    /**
     * 사용자의 총 보유자산 조회 (totalAmount)
     * @param userId 사용자 ID
     * @return 총 보유자산 (totalAmount)
     */
    suspend fun getTotalAmount(userId: String): BigDecimal = withContext(Dispatchers.IO) {
        try {
            val response = getPortfolioResponse(userId)
            response?.totalAmount ?: BigDecimal.ZERO
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "총 보유자산 조회 실패: ${e.message}", e)
            BigDecimal.ZERO
        }
    }

    /**
     * 사용자의 총 평가금액 조회
     * @param userId 사용자 ID
     * @return 총 평가금액 (totalEval)
     */
    suspend fun getTotalEvalAmount(userId: String): BigDecimal = withContext(Dispatchers.IO) {
        try {
            val response = getPortfolioResponse(userId)
            response?.totalEval ?: BigDecimal.ZERO
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "총 평가금액 조회 실패: ${e.message}", e)
            BigDecimal.ZERO
        }
    }

    /**
     * 사용자의 총 매수금액 조회
     * @param userId 사용자 ID
     * @return 총 매수금액 (totalBuy)
     */
    suspend fun getTotalBuyAmount(userId: String): BigDecimal = withContext(Dispatchers.IO) {
        try {
            val response = getPortfolioResponse(userId)
            response?.totalBuy ?: BigDecimal.ZERO
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "총 매수금액 조회 실패: ${e.message}", e)
            BigDecimal.ZERO
        }
    }

    /**
     * 사용자의 총 평가손익 조회
     * @param userId 사용자 ID
     * @return 총 평가손익 (profit)
     */
    suspend fun getTotalProfit(userId: String): BigDecimal = withContext(Dispatchers.IO) {
        try {
            val response = getPortfolioResponse(userId)
            response?.profit ?: BigDecimal.ZERO
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "총 평가손익 조회 실패: ${e.message}", e)
            BigDecimal.ZERO
        }
    }

    /**
     * 사용자의 총 수익률 조회
     * @param userId 사용자 ID
     * @return 총 수익률 (profitRate)
     */
    suspend fun getTotalProfitRate(userId: String): BigDecimal = withContext(Dispatchers.IO) {
        try {
            val response = getPortfolioResponse(userId)
            response?.profitRate ?: BigDecimal.ZERO
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "총 수익률 조회 실패: ${e.message}", e)
            BigDecimal.ZERO
        }
    }

    /**
     * 사용자의 포트폴리오 차트 데이터 조회
     * @param userId 사용자 ID
     * @return 포트폴리오 차트 아이템 목록
     */
    suspend fun getPortfolioChart(userId: String): List<com.stip.api.model.PortfolioChartItemDto> = withContext(Dispatchers.IO) {
        try {
            val response = getPortfolioResponse(userId)
            response?.portfolioChart ?: emptyList()
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "포트폴리오 차트 조회 실패: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 기존 호환성을 위한 메서드 (deprecated)
     * @deprecated getPortfolioWallets()를 사용하세요
     */
    @Deprecated("getPortfolioWallets()를 사용하세요")
    suspend fun getPortfolio(userId: String): List<PortfolioAsset> = withContext(Dispatchers.IO) {
        try {
            val wallets = getPortfolioWallets(userId)
            wallets.map { wallet ->
                PortfolioAsset(
                    symbol = wallet.symbol,
                    walletId = wallet.walletId ?: "",
                    balance = wallet.balance.toDouble(),
                    availableBalance = wallet.availableBalance.toDouble(),
                    address = wallet.address ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "기존 포트폴리오 조회 실패: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 특정 심볼의 지갑 주소 조회
     * @param userId 사용자 ID
     * @param symbol 암호화폐 심볼
     * @return 지갑 주소
     */
    suspend fun getWalletAddress(userId: String, symbol: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("PortfolioRepository", "지갑 주소 조회 시작: userId=$userId, symbol=$symbol")
            
            val response = walletAddressService.getWalletAddress(userId, symbol)
            
            Log.d("PortfolioRepository", "지갑 주소 API 응답: $response")
            
            response.walletAddress
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "지갑 주소 조회 예외 발생: ${e.message}", e)
            null
        }
    }
} 