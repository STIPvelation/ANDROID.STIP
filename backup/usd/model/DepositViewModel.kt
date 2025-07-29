package com.stip.ipasset.usd.model

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import com.stip.stip.ipasset.model.AccountInfoItem
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DepositViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext

    private val _depositUrl = MutableStateFlow<String?>(null)
    val depositUrl = _depositUrl.asStateFlow()

    private val _qrCode = MutableStateFlow<Bitmap?>(null)
    val qrCode = _qrCode.asStateFlow()

    private val _depositAccountInfoList = MutableStateFlow<List<AccountInfoItem>>(emptyList())
    val depositAccountInfoList = _depositAccountInfoList.asStateFlow()

    fun generateQrCodeFromUrl() {
        // TODO: 실제 API에서 입금 주소 가져오기
        // val response = depositRepository.getDepositAddress(currencyCode)
        // val url = response.address
        
        val url = "" // 임시 빈 값 사용
        _depositUrl.value = url

        if (url.isNotEmpty()) {
            try {
                val barcodeEncoder = BarcodeEncoder()
                _qrCode.value = barcodeEncoder.encodeBitmap(url, BarcodeFormat.QR_CODE, 512, 512)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchAccountInfo(currencyCode: String? = null) {
        // 입금 계좌 정보
        _depositAccountInfoList.value = listOf(
            AccountInfoItem(
                accountHolder = "주식회사 아이피미디어그룹",
                accountNumber = "102701-04-435574",
                bank = "국민은행"
            ),
            AccountInfoItem(
                accountHolder = "주식회사 아이피미디어그룹",
                accountNumber = "140-015-070902",
                bank = "신한은행"
            ),
            AccountInfoItem(
                accountHolder = "주식회사 아이피미디어그룹",
                accountNumber = "1005-804-753434",
                bank = "우리은행"
            )
        )
        // 추후 API 연동 시 아래 코드 활용
        // TODO: 실제 API에서 계좌 정보 가져오기
        // val response = accountRepository.getDepositAccounts(currencyCode)
        // val accountList = response.accounts.map { account ->
        //    AccountInfoItem(
        //        accountHolder = account.holderName,
        //        accountNumber = account.accountNumber,
        //        bank = account.bankName
        //    )
        // }
        // _depositAccountInfoList.value = accountList
    }
}
