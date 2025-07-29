package com.stip.dummy

import com.stip.ipasset.model.IpAsset
import android.graphics.Color

/**
 * 앱 전체에서 일관된 자산 및 IP 정보를 제공하는 중앙화된 더미 데이터 클래스
 * - IP 홀딩, 입출금 및 거래 화면 등에서 사용되는 모든 데이터의 일관성 유지
 * - 테스트 및 개발 목적으로만 사용
 */
object AssetDummyData {
    
    // 기본 컬러값 (외부에서도 사용 가능하도록 공개)
    val COLORS = listOf(
        0xFF4CAF50.toInt(), // ETIP-A 녹색
        0xFF2196F3.toInt(), // ETIP-E 파란색
        0xFFFF9800.toInt(), // NTIP-N 주황색
        0xFF9C27B0.toInt(), // MTIP-M 보라색
        0xFF607D8B.toInt(), // STIP-S 회색
        0xFFE91E63.toInt(), // BTIP-B 분홍색
        0xFF009688.toInt(), // DTIP-D 청록색
        0xFF673AB7.toInt(), // JTIP-J 진보라색
        0xFFFF5722.toInt(), // CTIP-C 주황-빨강색
        0xFF795548.toInt(), // FTIP-F 갈색
        0xFF3F51B5.toInt()  // KTIP-K 남색
    )
    
    // 변경 가능한 자산 리스트 (업데이트를 위해 mutable로 변경)
    private val assets = mutableListOf(
        // USD 데이터 - 숫자 데이터는 API에서 가져올 예정
        IpAsset(id = "1", name = "US Dollar", ticker = "USD", balance = 0.0, value = 0.0),
        
        // 11개 IP 티커 데이터 - 이름과 티커 정보만 유지, 숫자 데이터는 API에서 가져올 예정
        IpAsset(id = "2", name = "엔터테인먼트 IP A", ticker = "JWV", balance = 0.0, value = 0.0),
        IpAsset(id = "3", name = "엔터테인먼트 IP E", ticker = "MDM", balance = 0.0, value = 0.0),
        IpAsset(id = "4", name = "뉴미디어 IP N", ticker = "CDM", balance = 0.0, value = 0.0),
        IpAsset(id = "5", name = "음악 IP M", ticker = "IJECT", balance = 0.0, value = 0.0),
        IpAsset(id = "6", name = "스포츠 IP S", ticker = "WETALK", balance = 0.0, value = 0.0),
        IpAsset(id = "7", name = "브랜드 IP B", ticker = "SLEEP", balance = 0.0, value = 0.0),
        IpAsset(id = "8", name = "디자인 IP D", ticker = "KCOT", balance = 0.0, value = 0.0),
        IpAsset(id = "9", name = "게임 IP J", ticker = "MSK", balance = 0.0, value = 0.0),
        IpAsset(id = "10", name = "캐릭터 IP C", ticker = "SMT", balance = 0.0, value = 0.0),
        IpAsset(id = "11", name = "영화 IP F", ticker = "AXNO", balance = 0.0, value = 0.0),
        IpAsset(id = "12", name = "K-컨텐츠 IP K", ticker = "KATV", balance = 0.0, value = 0.0)
    )
    
    /**
     * 자산의 단가 정보 반환 (현재가)
     */
    fun getAssetPrice(ticker: String): Double {
        val asset = getAssetByTicker(ticker) ?: return 0.0
        if (asset.balance <= 0) return 0.0
        return asset.value / asset.balance
    }
    
    /**
     * 자산의 컬러 정보 반환
     */
    fun getAssetColor(ticker: String): Int {
        return when (ticker) {
            "JWV" -> COLORS[0]
            "MDM" -> COLORS[1]
            "CDM" -> COLORS[2]
            "IJECT" -> COLORS[3]
            "WETALK" -> COLORS[4]
            "SLEEP" -> COLORS[5]
            "KCOT" -> COLORS[6]
            "MSK" -> COLORS[7]
            "SMT" -> COLORS[8]
            "AXNO" -> COLORS[9]
            "KATV" -> COLORS[10]
            else -> Color.GRAY
        }
    }
    
    /**
     * 기본 자산 더미 데이터 반환
     */
    fun getDefaultAssets(): List<IpAsset> {
        return assets.toList()
    }
    
    /**
     * 특정 ID의 티커 정보 반환
     */
    fun getAssetById(id: String): IpAsset? {
        return getDefaultAssets().find { it.id == id }
    }
    
    /**
     * 특정 코드의 티커 정보 반환
     */
    fun getAssetByCode(code: String): IpAsset? {
        return getDefaultAssets().find { it.currencyCode == code }
    }
    
    /**
     * 특정 티커의 자산 정보 반환
     */
    fun getAssetByTicker(ticker: String): IpAsset? {
        return getDefaultAssets().find { it.ticker == ticker }
    }
    
    /**
     * 특정 코드의 자산 업데이트 (출금/입금 처리)
     * @param code 티커 코드
     * @param amount 변경할 금액 (출금은 음수, 입금은 양수)
     * @return 업데이트 성공 여부
     */
    fun updateAssetAmount(code: String, amount: Float): Boolean {
        val assetIndex = assets.indexOfFirst { it.ticker == code }
        if (assetIndex != -1) {
            val asset = assets[assetIndex]
            // 현재 자산에서 변경할 금액을 더함 (출금은 음수를 전달하므로 차감됨)
            val newBalance = asset.balance + amount
            
            // 잔액이 부족한 경우 처리
            if (newBalance < 0) return false
            
            // 새 자산 객체를 생성하여 기존 자산을 교체 (value는 1:1 비율로 간단히 계산)
            assets[assetIndex] = asset.copy(
                balance = newBalance,
                value = newBalance * (asset.value / asset.balance) // 단가 유지하며 value 계산
            )
            return true
        }
        return false
    }
    
    /**
     * 총 보유 USD 가치 반환
     */
    fun getTotalUsdValue(): Double {
        return getDefaultAssets().sumOf { it.value }
    }
    
    /**
     * IP 자산 비율 정보 반환 (포트폴리오 차트용)
     */
    fun getPortfolioRatios(): List<Pair<String, Float>> {
        val nonUsdAssets = assets.filter { it.ticker != "USD" }
        val totalValue = nonUsdAssets.sumOf { it.value }
        
        return nonUsdAssets.map { asset ->
            val ratio = (asset.value / totalValue * 100).toFloat()
            Pair(asset.ticker, ratio)
        }
    }
}
