package com.stip.stip.iptransaction.model

data class IpInvestmentItem(
    val type: String,           // 타입 (예: "매수", "매도")
    val name: String,           // 종목 (예: "티커 A")
    val quantity: String,       // 거래수량 (숫자지만 우선 String 처리)
    val unitPrice: String,      // 거래단가
    val amount: String,         // 거래금액
    val fee: String,            // 수수료
    val settlement: String,     // 정산금액
    val orderTime: String,      // 주문시간
    val executionTime: String   // 체결시간
)