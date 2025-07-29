package com.stip.ipasset.usd.model

import java.text.NumberFormat
import java.util.*

/**
 * USDDepositTransaction 확장 함수
 */
fun USDDepositTransaction.getFormattedDate(): String = date
fun USDDepositTransaction.getFormattedTime(): String = time
fun USDDepositTransaction.getFormattedUsdAmount(): String = String.format("%.2f USD", usdAmount)
fun USDDepositTransaction.getFormattedKrwAmount(): String = String.format("%,d KRW", krwAmount)

/**
 * USDWithdrawalTransaction 확장 함수
 */
fun USDWithdrawalTransaction.getFormattedDate(): String = date
fun USDWithdrawalTransaction.getFormattedTime(): String = time
fun USDWithdrawalTransaction.getFormattedUsdAmount(): String = String.format("%.2f USD", usdAmount)
fun USDWithdrawalTransaction.getFormattedKrwAmount(): String = String.format("%,d KRW", krwAmount)

/**
 * USDReturnTransaction 확장 함수
 */
fun USDReturnTransaction.getFormattedDate(): String = date
fun USDReturnTransaction.getFormattedTime(): String = time
fun USDReturnTransaction.getFormattedUsdAmount(): String = String.format("%.2f USD", usdAmount)
fun USDReturnTransaction.getFormattedKrwAmount(): String = String.format("%,d KRW", krwAmount)

/**
 * USDProcessTransaction 확장 함수
 */
fun USDProcessTransaction.getFormattedDate(): String = date
fun USDProcessTransaction.getFormattedTime(): String = time
fun USDProcessTransaction.getFormattedUsdAmount(): String = String.format("%.2f USD", usdAmount)
fun USDProcessTransaction.getFormattedKrwAmount(): String = String.format("%,d KRW", krwAmount)
