package com.stip.ipasset.ticker.fragment

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpAssetTickerDepositBinding
import com.stip.stip.databinding.LayoutDepositTickerBinding
import com.stip.ipasset.usd.model.DepositViewModel
import com.stip.stip.ipasset.extension.copyToClipboard
import com.stip.stip.ipasset.fragment.BaseFragment
import androidx.navigation.fragment.navArgs
import com.stip.stip.ipasset.model.IpAsset
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 티커 입금을 처리하는 Fragment
 * QR 코드와 입금 주소를 표시합니다.
 */
@AndroidEntryPoint
class TickerDepositFragment : BaseFragment<FragmentIpAssetTickerDepositBinding>() {
    private val args by navArgs<com.stip.ipasset.ticker.fragment.TickerDepositFragmentArgs>()
    private val ipAsset: IpAsset get() = args.ipAsset
    private val currencyCode: String get() = ipAsset.currencyCode

    private val viewModel by viewModels<DepositViewModel>()
    
    // 티커 관련 상수 및 데이터
    private val tickerList = listOf(
        "AXNO", "IJECT", "WETALK", "MDM", "KCOT", "MUSIC", "GGD", 
        "FRANCHISE", "ETIP 신재생 에너지", "ETIP 인공지능", "ETIP 반도체", "ETIP 바이오"
    )
    
    // 티커별 입금 주소 맵
    private val tickerAddressMap = mapOf(
        "AXNO" to "ax39dk2mflse9sdf9sd2mxsd9f",
        "IJECT" to "ij58dkeld9sdkfm39dkf92lsd",
        "WETALK" to "we45dkfm39ekf9sdkf93ksdf",
        "MDM" to "md67sfkd93kfs93kfd93kdls",
        "KCOT" to "kc83skfd93kfs93dkf39skf",
        "MUSIC" to "mu94dkf93kfd93kfd93kfw",
        "GGD" to "gg74dkf93kfd93kfd39skf",
        "FRANCHISE" to "fr83skd93kfs93kfd93ksd",
        "ETIP 신재생 에너지" to "et67skd93kfs93kfd93kdf",
        "ETIP 인공지능" to "et89dkf93kfd93kfd93kfd",
        "ETIP 반도체" to "et54dkf93kfd93kfd93kdf",
        "ETIP 바이오" to "et23dkf93kfd93kfd93kfd"
    )

    private fun bind() = with(viewBinding) {
        materialToolbar.title = "$currencyCode ${getString(R.string.common_deposit_action)}"
        materialToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // 티커 레이아웃 바인딩
        layoutTicker?.let { bindTickerLayout(it) }
    }

    private fun bindTickerLayout(binding: LayoutDepositTickerBinding) {
        val address = tickerAddressMap[currencyCode] ?: ""
        
        // 티커 심볼 표시
        binding.root.findViewById<android.widget.TextView>(R.id.tv_ticker_code).text = currencyCode
        
        // 입금 주소 표시
        binding.root.findViewById<android.widget.TextView>(R.id.tv_deposit_address).text = address
        
        // QR 코드 생성 (비동기로 처리)
        generateQRCode(address)
        
        // 주소 복사 버튼 설정
        binding.root.findViewById<android.widget.ImageView>(R.id.iv_copy).setOnClickListener {
            copyAddressToClipboard(address)
        }
    }
    
    private fun copyAddressToClipboard(address: String) {
        requireContext().copyToClipboard(
            label = "Deposit Address",
            text = address
        )
        Toast.makeText(requireContext(), "주소가 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show()
    }
    
    private fun generateQRCode(address: String) {
        if (address.isEmpty()) return
        
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                try {
                    val qrCodeWriter = QRCodeWriter()
                    val bitMatrix = qrCodeWriter.encode(address, BarcodeFormat.QR_CODE, 512, 512)
                    val width = bitMatrix.width
                    val height = bitMatrix.height
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                    
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                        }
                    }
                    
                    bitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            
            bitmap?.let { bmp ->
                withContext(Dispatchers.Main) {
                    // Find the QR code ImageView the same way we access other views
                    viewBinding.layoutTicker?.root?.findViewById<android.widget.ImageView>(R.id.iv_qr_code)?.setImageBitmap(bmp)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind()
    }

    override fun inflate(inflater: LayoutInflater, container: ViewGroup?): FragmentIpAssetTickerDepositBinding {
        return FragmentIpAssetTickerDepositBinding.inflate(inflater, container, false)
    }
    
    /**
     * 출금 입력 화면으로 이동하는 함수
     */
    private fun navigateToWithdrawal() {
        // 현재 화면에서 출금 화면으로 전환
        findNavController().navigate(
            R.id.tickerWithdrawalInputFragment,
            Bundle().apply { putParcelable("ipAsset", ipAsset) }
        )
    }
}
