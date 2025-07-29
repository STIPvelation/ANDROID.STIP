package com.stip.stip.more.model

import java.util.*

/**
 * 로그인 이력 모델 클래스
 * iOS 버전의 LoginRecord 구조체와 동일한 기능 구현
 */
data class LoginRecord(
    val id: String = UUID.randomUUID().toString(), // 각 로그인 이력의 고유 식별자
    val date: Date, // 로그인 일시
    val ipAddress: String, // 접속 IP 주소
    val location: String, // 접속 위치
    val deviceInfo: String, // 기기 정보
    val isUnusual: Boolean = false // 비정상 접속 여부
)
