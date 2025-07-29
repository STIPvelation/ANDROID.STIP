package com.stip.stip.signup.utils

import android.content.Context
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.stip.stip.R
import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Utils {
    companion object {
        // 은행별 계좌번호 형식 정보
        private val bankAccountFormats = mapOf(
            "002" to Regex("^\\d{10,12}$"),  // 산업은행
            "003" to Regex("^\\d{10,12}$"),  // 기업은행
            "004" to Regex("^\\d{10,16}$"),  // 국민은행
            "007" to Regex("^\\d{10,12}$"),  // 수협은행/수협중앙회
            "011" to Regex("^\\d{11,13}$"),  // 농협은행
            "012" to Regex("^\\d{10,13}$"),  // 농협중앙회
            "020" to Regex("^\\d{11,14}$"),  // 우리은행
            "023" to Regex("^\\d{10,12}$"),  // SC제일은행
            "027" to Regex("^\\d{10,12}$"),  // 한국씨티은행
            "031" to Regex("^\\d{10,12}$"),  // 대구은행
            "032" to Regex("^\\d{10,12}$"),  // 부산은행
            "034" to Regex("^\\d{10,12}$"),  // 광주은행
            "035" to Regex("^\\d{10,12}$"),  // 제주은행
            "037" to Regex("^\\d{10,12}$"),  // 전북은행
            "039" to Regex("^\\d{10,12}$"),  // 경남은행
            "045" to Regex("^\\d{10,12}$"),  // 새마을금고중앙회
            "048" to Regex("^\\d{10,12}$"),  // 신협중앙회
            "050" to Regex("^\\d{10,12}$"),  // 상호저축은행
            "054" to Regex("^\\d{10,12}$"),  // HSBC은행
            "055" to Regex("^\\d{10,12}$"),  // 도이치은행
            "057" to Regex("^\\d{10,12}$"),  // 제이피모간체이스은행
            "060" to Regex("^\\d{10,12}$"),  // BOA은행
            "062" to Regex("^\\d{10,12}$"),  // 중국공상은행
            "064" to Regex("^\\d{10,12}$"),  // 산림조합중앙회
            "071" to Regex("^\\d{10,12}$"),  // 우체국
            "081" to Regex("^\\d{10,12}$"),  // KEB하나은행
            "088" to Regex("^\\d{11}$"),     // 신한은행
            "089" to Regex("^\\d{11}$"),     // K뱅크
            "090" to Regex("^\\d{11}$"),     // 카카오뱅크
            "092" to Regex("^\\d{10,12}$"),  // 토스뱅크
            // 증권사(계좌번호 형식 확인 필요—대개 8~12자리 숫자)
            "209" to Regex("^\\d{8,12}$"),   // 유안타증권
            "218" to Regex("^\\d{8,12}$"),   // KB증권
            "238" to Regex("^\\d{8,12}$"),   // 미래에셋대우
            "240" to Regex("^\\d{8,12}$"),   // 삼성증권
            "243" to Regex("^\\d{8,12}$"),   // 한국투자증권
            "247" to Regex("^\\d{8,12}$"),   // NH투자증권
            "261" to Regex("^\\d{8,12}$"),   // 교보증권
            "262" to Regex("^\\d{8,12}$"),   // 하이투자증권
            "263" to Regex("^\\d{8,12}$"),   // 현대차투자증권
            "264" to Regex("^\\d{8,12}$"),   // 키움증권
            "265" to Regex("^\\d{8,12}$"),   // 이베스트투자증권
            "266" to Regex("^\\d{8,12}$"),   // SK증권
            "267" to Regex("^\\d{8,12}$"),   // 대신증권
            "269" to Regex("^\\d{8,12}$"),   // 한화투자증권
            "270" to Regex("^\\d{8,12}$"),   // 하나금융투자
            "278" to Regex("^\\d{8,12}$"),   // 신한금융투자
            "279" to Regex("^\\d{8,12}$"),   // 동부증권
            "280" to Regex("^\\d{8,12}$"),   // 유진투자증권
            "287" to Regex("^\\d{8,12}$"),   // 메리츠종합금융증권
            "290" to Regex("^\\d{8,12}$"),   // 부국증권
            "291" to Regex("^\\d{8,12}$"),   // 신영증권
            "292" to Regex("^\\d{8,12}$"),   // 케이프투자증권
            "103" to Regex("^\\d{10,12}$")   // SBI저축은행
        )
        
        // 상태바 색상 변경
        fun adjustStatusBarColor(context: Context, window: Window, color: Int) {
            window.statusBarColor = ContextCompat.getColor(context, color)
            // StatusBar 색상 변경 후 텍스트 보이지 않아 텍스트 색상 변경
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }

        // 은행 코드로 은행 이름 찾기
        fun getBankNameByCode(context: Context, code: String): String {
            val bankCodes = context.resources.getStringArray(R.array.bank_code)
            val bankNames = context.resources.getStringArray(R.array.bank_name)

            val index = bankCodes.indexOf(code)
            return if (index != -1) bankNames[index] else "해당 코드에 맞는 은행이 없습니다."
        }

        // 은행 이름으로 은행 코드 찾기
        fun getBankCodeByName(context: Context, name: String): String {
            val bankCodes = context.resources.getStringArray(R.array.bank_code)
            val bankNames = context.resources.getStringArray(R.array.bank_name)

            val index = bankNames.indexOf(name)
            return if (index != -1) bankCodes[index] else "00"
        }
          // 직업 코드로 직업명 찾기
        fun getJobNameByCode(context: Context, code: String): String {
            val jobCodes = context.resources.getStringArray(R.array.kyc_job_select_code)
            val jobNames = context.resources.getStringArray(R.array.kyc_job_select)

            val index = jobCodes.indexOf(code)
            return if (index != -1) jobNames[index] else "기타"
        }

        // 직업 코드로 직업명 찾기
        fun getJobCodeByName(context: Context, name: String): String {
            val jobCodes = context.resources.getStringArray(R.array.kyc_job_select_code)
            val jobNames = context.resources.getStringArray(R.array.kyc_job_select)

            val index = jobNames.indexOf(name)
            return if (index != -1) jobCodes[index] else "OFFICE_WORKER"
        }
        
        fun getKycCode(context: Context, selectedValue: String, arrayResId: Int, codeArrayResId: Int): String? {
            val values = context.resources.getStringArray(arrayResId)
            val codes = context.resources.getStringArray(codeArrayResId)

            val index = values.indexOf(selectedValue)
            return if (index != -1) codes[index] else null
        }

        // 생년월일 포맷팅
        fun formatBirthdate(birthdate: String?): String {
            return if (birthdate?.length == 8) {
                val year = birthdate.substring(0, 4)
                val month = birthdate.substring(4, 6)
                val day = birthdate.substring(6, 8)
                "$year-$month-$day"
            } else birthdate ?: "-"
        }
        
        // 핸드폰 번호 포맷팅
        fun formatPhoneNumber(phoneNumber: String?): String {
            return if (phoneNumber?.length == 11) {
                val prefix = phoneNumber.substring(0, 3)
                val middle = phoneNumber.substring(3, 7)
                val suffix = phoneNumber.substring(7, 11)
                "$prefix-$middle-$suffix"
            } else phoneNumber ?: "-"
        }
        
        // 주소 포맷팅 (우편번호 포함)
        fun formatAddress(zipCode: String?, address: String?): String {
            return if (!zipCode.isNullOrBlank() && !address.isNullOrBlank()) {
                "($zipCode) $address"
            } else {
                address ?: "-"
            }
        }
          
        // 계좌번호 포맷팅
        fun formatAccountNumber(bankCode: String, accountNumber: String?): String {
            if (accountNumber.isNullOrBlank()) return "-"
            
            // 은행별 포맷팅 적용
            return when (bankCode) {
                "002" -> { // 산업은행: 000-00-000000
                    if (accountNumber.length >= 10) {
                        val part1 = accountNumber.take(3)
                        val part2 = accountNumber.substring(3, 5)
                        val part3 = accountNumber.substring(5)
                        "$part1-$part2-$part3"
                    } else {
                        accountNumber.chunked(4).joinToString("-")
                    }
                }
                "003" -> { // 기업은행: 000-000000-00-000
                    if (accountNumber.length >= 10) {
                        val part1 = accountNumber.take(3)
                        val part2 = accountNumber.substring(3, 9)
                        val part3 = accountNumber.substring(9, 11)
                        val part4 = if (accountNumber.length > 11) accountNumber.substring(11) else ""
                        if (part4.isNotEmpty()) "$part1-$part2-$part3-$part4" else "$part1-$part2-$part3"
                    } else {
                        accountNumber.chunked(4).joinToString("-")
                    }
                }
                "004" -> { // 국민은행: 000000-00-000000 또는 000-000000-00-000
                    if (accountNumber.length <= 11) {
                        val part1 = accountNumber.take(6)
                        val part2 = accountNumber.substring(6, 8)
                        val part3 = accountNumber.substring(8)
                        "$part1-$part2-$part3"
                    } else {
                        val part1 = accountNumber.take(3)
                        val part2 = accountNumber.substring(3, 9)
                        val part3 = accountNumber.substring(9, 11)
                        val part4 = accountNumber.substring(11)
                        "$part1-$part2-$part3-$part4"
                    }
                }
                "007" -> { // 수협은행: 000-00-000000
                    val part1 = accountNumber.take(3)
                    val part2 = accountNumber.substring(3, 5)
                    val part3 = accountNumber.substring(5)
                    "$part1-$part2-$part3"
                }
                "011", "012" -> { // 농협: 000-0000-0000-00 또는 000-0000-0000
                    if (accountNumber.length >= 13) {
                        val part1 = accountNumber.take(3)
                        val part2 = accountNumber.substring(3, 7)
                        val part3 = accountNumber.substring(7, 11)
                        val part4 = accountNumber.substring(11)
                        "$part1-$part2-$part3-$part4"
                    } else {
                        val part1 = accountNumber.take(3)
                        val part2 = accountNumber.substring(3, 7)
                        val part3 = accountNumber.substring(7)
                        "$part1-$part2-$part3"
                    }
                }
                "020" -> { // 우리은행: 0000-000-000000
                    val part1 = accountNumber.take(4)
                    val part2 = accountNumber.substring(4, 7)
                    val part3 = accountNumber.substring(7)
                    "$part1-$part2-$part3"
                }
                "023" -> { // SC제일은행: 000-00-000000
                    val part1 = accountNumber.take(3)
                    val part2 = accountNumber.substring(3, 5)
                    val part3 = accountNumber.substring(5)
                    "$part1-$part2-$part3"
                }
                "027" -> { // 한국씨티은행: 000-00-000000
                    val part1 = accountNumber.take(3)
                    val part2 = accountNumber.substring(3, 5)
                    val part3 = accountNumber.substring(5)
                    "$part1-$part2-$part3"
                }
                "031", "032", "034", "035", "037", "039" -> { // 지방은행: 000-00-000000
                    val part1 = accountNumber.take(3)
                    val part2 = accountNumber.substring(3, 5)
                    val part3 = accountNumber.substring(5)
                    "$part1-$part2-$part3"
                }
                "045", "048", "050", "064", "071" -> { // 새마을금고, 신협 등 금융기관: 000-000-000000
                    val part1 = accountNumber.take(3)
                    val part2 = accountNumber.substring(3, 6)
                    val part3 = accountNumber.substring(6)
                    "$part1-$part2-$part3"
                }
                "081" -> { // 하나은행: 000-00000-00000
                    val part1 = accountNumber.take(3)
                    val part2 = accountNumber.substring(3, 8)
                    val part3 = accountNumber.substring(8)
                    "$part1-$part2-$part3"
                }
                "088" -> { // 신한은행: 000-000-000000
                    val part1 = accountNumber.take(3)
                    val part2 = accountNumber.substring(3, 6)
                    val part3 = accountNumber.substring(6)
                    "$part1-$part2-$part3"
                }
                "089", "090", "092" -> { // K뱅크, 카카오뱅크, 토스뱅크: 일반적인 포맷
                    if (accountNumber.length > 8) {
                        val part1 = accountNumber.take(accountNumber.length - 8)
                        val part2 = accountNumber.substring(accountNumber.length - 8, accountNumber.length - 4)
                        val part3 = accountNumber.substring(accountNumber.length - 4)
                        "$part1-$part2-$part3"
                    } else if (accountNumber.length > 4) {
                        val part1 = accountNumber.take(accountNumber.length - 4)
                        val part2 = accountNumber.substring(accountNumber.length - 4)
                        "$part1-$part2"
                    } else {
                        accountNumber
                    }
                }
                // 증권사 계좌번호: 000-00-000000 또는 00-000000-00
                "209", "218", "238", "240", "243", "247", "261", "262", "263", "264", "265", "266", "267", "269", "270", "278", "279", "280", "287", "290", "291", "292" -> {
                    if (accountNumber.length <= 8) {
                        accountNumber.chunked(4).joinToString("-")
                    } else if (accountNumber.length <= 10) {
                        val part1 = accountNumber.take(2)
                        val part2 = accountNumber.substring(2, accountNumber.length - 2)
                        val part3 = accountNumber.substring(accountNumber.length - 2)
                        "$part1-$part2-$part3"
                    } else {
                        val part1 = accountNumber.take(3)
                        val part2 = accountNumber.substring(3, 5)
                        val part3 = accountNumber.substring(5)
                        "$part1-$part2-$part3"
                    }
                }
                "103" -> { // SBI저축은행: 000-000-000000
                    val part1 = accountNumber.take(3)
                    val part2 = accountNumber.substring(3, 6)
                    val part3 = accountNumber.substring(6)
                    "$part1-$part2-$part3"
                }
                else -> ({}).toString()
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun birthDayFormatDate(input: String): String? {
            // 입력이 8자리 숫자인지 확인
            if (!input.matches(Regex("\\d{8}"))) {
                return null
            }

            return try {
                val year = input.substring(0, 4).toInt()
                val month = input.substring(4, 6).toInt()
                val day = input.substring(6, 8).toInt()

                // LocalDate를 사용해 유효한 날짜인지 검증
                val date = LocalDate.of(year, month, day)

                // 날짜 형식 변환 (yyyy-MM-dd)
                date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            } catch (e: DateTimeException) {
                null // 존재하지 않는 날짜 (예: 20230230 등)
            } catch (e: Exception) {
                null // 기타 예외 처리
            }
        }
    }
}