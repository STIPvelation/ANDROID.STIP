package com.stip.ipasset.ticker.fragment

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.stip.stip.R
import com.stip.ipasset.usd.model.DepositViewModel
import com.stip.ipasset.extension.copyToClipboard
import com.stip.stip.MainActivity
import com.stip.ipasset.model.IpAsset
import com.stip.stip.databinding.FragmentIpAssetTickerDepositBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.stip.stip.signup.utils.PreferenceUtil

/**
 * 티커 입금을 처리하는 Fragment
 * QR 코드와 입금 주소를 표시합니다.
 */
@AndroidEntryPoint
class TickerDepositFragment : Fragment() {
    private var _binding: FragmentIpAssetTickerDepositBinding? = null
    private val binding get() = _binding!!
    
    private var ipAsset: IpAsset? = null
    private val currencyCode: String get() = ipAsset?.currencyCode?.uppercase() ?: ""

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
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIpAssetTickerDepositBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        
        // 티커 화면에서는 헤더 레이아웃 숨기기
        (activity as? MainActivity)?.setHeaderVisibility(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 번들에서 데이터 추출 - API 레벨 33+ 호환성 수정
        ipAsset = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("ipAsset", com.stip.ipasset.model.IpAsset::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("ipAsset")
        }
        
        // 헤더 숨기기
        (activity as? MainActivity)?.setHeaderVisibility(false)
        
        // 티커 잔고 정보 로드 (새로운 포트폴리오 DTO 구조 사용)
        loadTickerBalance()
        
        // 뒤로가기 버튼은 toolbar의 navigation icon을 사용
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
        
        // 뷰 설정
        setupViews()
    }

    /**
     * 티커 잔고 정보 로드
     */
    private fun loadTickerBalance() {
        lifecycleScope.launch {
            try {
                val memberId = PreferenceUtil.getUserId()
                if (memberId.isNullOrBlank()) {
                    android.util.Log.w("TickerDepositFragment", "사용자 ID가 없습니다.")
                    return@launch
                }
                
                val portfolioRepository = com.stip.api.repository.PortfolioRepository()
                val portfolioResponse = portfolioRepository.getPortfolioResponse(memberId)
                
                if (portfolioResponse != null) {
                    // 현재 티커의 지갑 찾기
                    val tickerWallet = portfolioResponse.wallets.find { it.symbol == currencyCode }
                    
                    if (tickerWallet != null) {
                        android.util.Log.d("TickerDepositFragment", "${currencyCode} 잔고 로드 완료: ${tickerWallet.balance} ${currencyCode} (≈ $${tickerWallet.evalAmount} USD)")
                    } else {
                        android.util.Log.w("TickerDepositFragment", "${currencyCode} 지갑을 찾을 수 없습니다.")
                    }
                } else {
                    android.util.Log.w("TickerDepositFragment", "포트폴리오 응답이 null입니다.")
                }
            } catch (e: Exception) {
                android.util.Log.e("TickerDepositFragment", "${currencyCode} 잔고 로드 실패: ${e.message}", e)
            }
        }
    }

    private fun setupViews() {
        val toolbar = binding.toolbar
        toolbar.title = "$currencyCode 입금"
        toolbar.setNavigationOnClickListener {
            // 프래그먼트 매니저를 사용하여 이전 화면으로 돌아감
            requireActivity().supportFragmentManager.popBackStack()
        }

        // 티커 정보 바인딩
        setupTickerDeposit()
    }

    private fun setupTickerDeposit() {
        // API에서 주소 조회
        viewModel.fetchWalletAddress(currencyCode)

        // 티커 심볼 표시
        binding.tvTickerCode.text = currencyCode

        // 주소 및 QR코드
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.depositUrl.collect { address ->
                binding.tvDepositAddress.text = address ?: "-"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.qrCode.collect { bitmap ->
                if (bitmap != null) {
                    binding.ivQrCode.setImageBitmap(bitmap)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { msg ->
                if (!msg.isNullOrBlank()) {
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 주소 복사 버튼 설정
        binding.ivCopy.setOnClickListener {
            viewModel.depositUrl.value?.let { address ->
                copyAddressToClipboard(address)
            }
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
                    val ivQrCode = view?.findViewById<ImageView>(R.id.iv_qr_code)
                    ivQrCode?.setImageBitmap(bmp)
                }
            }
        }
    }
    
    /**
     * 출금 입력 화면으로 이동하는 함수
     */
    private fun navigateToWithdrawal() {
        try {
            // 프래그먼트 트랜잭션을 사용하여 출금 화면으로 전환
            val fragmentManager = requireActivity().supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            
            // 애니메이션 추가
            fragmentTransaction.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            
            // 티커 출금 프래그먼트로 이동 (TickerWithdrawalFragment가 존재한다면 사용)
            // 현재는 토스트 메시지로 대체
            Toast.makeText(requireContext(), "${currencyCode} 출금 기능 준비 중", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "화면 전환 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
