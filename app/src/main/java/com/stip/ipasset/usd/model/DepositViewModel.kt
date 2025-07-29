package com.stip.ipasset.usd.model

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.stip.ipasset.model.AccountInfoItem
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import com.stip.api.repository.PortfolioRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.lifecycle.viewModelScope

@HiltViewModel
class DepositViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext
    private val portfolioRepository = PortfolioRepository()

    private val _depositUrl = MutableStateFlow<String?>(null)
    val depositUrl = _depositUrl.asStateFlow()

    private val _qrCode = MutableStateFlow<Bitmap?>(null)
    val qrCode = _qrCode.asStateFlow()

    private val _depositAccountInfoList = MutableStateFlow<List<AccountInfoItem>>(emptyList())
    val depositAccountInfoList = _depositAccountInfoList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

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
                id = "1",
                name = "주식회사 아이피미디어그룹",
                accountNumber = "102701-04-435574",
                bankName = "국민은행",
                isSelected = false
            ),
            AccountInfoItem(
                id = "2",
                name = "주식회사 아이피미디어그룹",
                accountNumber = "140-015-070902",
                bankName = "신한은행",
                isSelected = false
            ),
            AccountInfoItem(
                id = "3",
                name = "주식회사 아이피미디어그룹",
                accountNumber = "1005-804-753434",
                bankName = "우리은행",
                isSelected = false
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

    fun fetchWalletAddress(symbol: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                val memberId = com.stip.stip.signup.utils.PreferenceUtil.getUserId()
                Log.d("DepositViewModel", "지갑 주소 조회: memberId=$memberId, symbol=$symbol")
                
                if (memberId.isNullOrBlank()) {
                    _errorMessage.value = "로그인 정보가 없습니다."
                    return@launch
                }
                
                // 포트폴리오 API에서 지갑 주소 가져오기
                val address = portfolioRepository.getWalletAddress(memberId, symbol)
                
                if (address != null) {
                    _depositUrl.value = address
                    try {
                        val qrCodeWriter = com.google.zxing.qrcode.QRCodeWriter()
                        val bitMatrix = qrCodeWriter.encode(address, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
                        val width = bitMatrix.width
                        val height = bitMatrix.height
                        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
                        for (x in 0 until width) {
                            for (y in 0 until height) {
                                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                            }
                        }
                        _qrCode.value = bitmap
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _qrCode.value = null
                    }
                } else {
                    _depositUrl.value = null
                    _qrCode.value = null
                    _errorMessage.value = "지갑 주소를 찾을 수 없습니다."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _depositUrl.value = null
                _qrCode.value = null
                _errorMessage.value = "네트워크 오류: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
