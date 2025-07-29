package com.stip.stip.more.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.stip.stip.databinding.ActivityPhoneAuthBinding
import com.stip.stip.more.viewmodel.MemberInfoEditViewModel
import com.stip.stip.signup.Constants

/**
 * 휴대폰 번호 인증만을 위한 전용 액티비티
 * NICE 본인인증 웹뷰를 표시하고 결과를 반환합니다.
 */
class PhoneAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhoneAuthBinding
    private lateinit var viewModel: MemberInfoEditViewModel

    companion object {
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_SOURCE_ACTIVITY = "source_activity"

        // 웹뷰 디버깅
        init {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        // 휴대폰 인증 액티비티 시작 Helper 메서드
        fun startPhoneAuth(activity: Activity) {
            val intent = Intent(activity, PhoneAuthActivity::class.java)
            intent.putExtra(EXTRA_SOURCE_ACTIVITY, activity.javaClass.simpleName)
            activity.startActivity(intent)
        }
        
        // 결과를 돌려받기 위한 startActivityForResult용 Helper 메서드
        fun startPhoneAuthForResult(activity: Activity, requestCode: Int) {
            val intent = Intent(activity, PhoneAuthActivity::class.java)
            intent.putExtra(EXTRA_SOURCE_ACTIVITY, activity.javaClass.simpleName)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    private var sourceActivity: String? = null
    private var lastAuthenticatedPhone: String? = null
    private var isAuthenticationSuccessful = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // ViewModel 초기화
        viewModel = ViewModelProvider(this).get(MemberInfoEditViewModel::class.java)
        setupObservers()
        
        // 로그인된 회원의 DI 값 로드 (SharedPreferences에서 바로 가져옴)
        viewModel.getMemberDi()
        
        // 호출한 액티비티 정보 저장
        sourceActivity = intent.getStringExtra(EXTRA_SOURCE_ACTIVITY)
        Log.d("PhoneAuthActivity", "Started from: $sourceActivity")
        
        // 웹뷰 설정
        initWebView()
        binding.webView.loadUrl(Constants.NICE_PAY_AUTH_URL)
        
        // 돌아가기 버튼 설정
        binding.btnCancel.setOnClickListener {
            // 인증 성공이라면 성공 결과 전달
            if (isAuthenticationSuccessful && lastAuthenticatedPhone != null) {
                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_PHONE_NUMBER, lastAuthenticatedPhone)
                resultIntent.putExtra(EXTRA_SOURCE_ACTIVITY, sourceActivity)
                setResult(Activity.RESULT_OK, resultIntent)
                Log.d("PhoneAuthActivity", "인증 성공 후 돌아가기 버튼 클릭, 전화번호: $lastAuthenticatedPhone")
            } else {
                // 인증 실패 또는 실패 전 취소인 경우
                val resultIntent = Intent()
                setResult(Activity.RESULT_CANCELED, resultIntent) 
                Log.d("PhoneAuthActivity", "인증 실패 또는 취소 후 돌아가기 버튼 클릭")
            }
            // 음안에서 현재 액티비티 종료
            finish()
        }
    }
    
    private fun setupObservers() {
        // 성공 메시지
        viewModel.successMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        
        // 에러 메시지 관찰
        viewModel.errorMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
          // 회원 DI 값 관찰
        viewModel.memberDi.observe(this) { di ->
            Log.d("PhoneAuthActivity", "회원 DI 값 로드됨: $di")
            if (di.isNullOrEmpty()) {
                Log.w("PhoneAuthActivity", "회원 DI 값이 없습니다. 본인 인증 필요 시 최초 인증으로 간주합니다.")
            }
        }
    }

    /** 인증번호 입력시 키보드가 화면 가림 문제 해결 */
    override fun onResume() {
        super.onResume()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11 이상에서는 WindowInsets.Type을 사용
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    override fun onPause() {
        super.onPause()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        val webView = binding.webView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        // 자바스크립트 인터페이스 추가
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        
        // 웹뷰 클라이언트 설정
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                // Intent URL 처리
                val url = request.url.toString()
                if (url.startsWith("intent://") || url.startsWith("market://")) {
                    try {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        val existPackage = packageManager.getLaunchIntentForPackage(
                            intent.getPackage() ?: return false
                        )
                        if (existPackage != null) {
                            startActivity(intent)
                        } else {
                            val marketIntent = Intent(Intent.ACTION_VIEW)
                            marketIntent.data =
                                Uri.parse("market://details?id=" + intent.getPackage())
                            startActivity(marketIntent)
                        }
                        return true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else if (url.startsWith("tel:")) {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = Uri.parse(url)
                    startActivity(intent)
                    return true
                }
                return false
            }
        }
    }

    inner class WebAppInterface(private val context: Context) {
        // 인증 완료 상태를 추적 변수
        private var isAuthCompleted = false
        private val handler = Handler(Looper.getMainLooper())

        // onAuthCompleted: SignUpAuthNiceFragment와 동일한 인터페이스 제공
        @JavascriptInterface
        fun onAuthCompleted(result: String, di: String) {
            // 중복 호출 방지
            if (isAuthCompleted) {
                return
            }
            isAuthCompleted = true

            Log.d("PhoneAuthActivity", "onAuthCompleted 호출됨: result=$result, di=$di")

            handler.post {
                if (result == "success" && di.isNotEmpty()) {
                    // DI 값을 서버로 전송하여 사용자 정보 조회
                    Toast.makeText(context, "본인인증 정보를 확인하는 중입니다...", Toast.LENGTH_SHORT).show()                    // 기존 DI 값과 비교
                    val existingDi = viewModel.memberDi.value
                    if (existingDi != null && existingDi.isNotEmpty() && existingDi != di) {
                        Log.e("PhoneAuthActivity", "인증 실패: 기존 DI 값과 새 DI 값이 일치하지 않습니다.")
                        Log.d("PhoneAuthActivity", "기존 DI: $existingDi, 새 DI: $di")
                        Toast.makeText(
                            context,
                            "본인인증에 실패했습니다. 가입 시 인증한 본인과 동일한 정보로 인증해주세요.",
                            Toast.LENGTH_LONG
                        ).show()
                        handleFailedAuth()
                        return@post
                    }

                    // 회원가입에서와 동일하게 DI 값으로 전화번호 조회
                    viewModel.getNiceAuthInfo(di)
                    // 결과
                    viewModel.niceAuthInfo.observe(this@PhoneAuthActivity) { niceAuthInfo ->
                        if (niceAuthInfo != null) {
                            // 성공 시 인증된 전화번호로 처리
                            val phoneNumber = niceAuthInfo.phoneNumber
                            Log.d("PhoneAuthActivity", "NICE 인증 전화번호 조회 성공: $phoneNumber")
                            handleSuccessfulAuth(phoneNumber)
                        }
                    }
                } else {
                    handleFailedAuth()
                }
            }
        }

        // 성공 처리 공통 메소드
        private fun handleSuccessfulAuth(phoneNumber: String) {
            try {
                // 인증 성공 메시지
                Toast.makeText(context, "본인인증이 완료되었습니다.", Toast.LENGTH_LONG).show()

                // 인증 성공 상태 및 전화번호 저장
                isAuthenticationSuccessful = true
                lastAuthenticatedPhone = phoneNumber

                // 인증 성공 결과 전달
                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                resultIntent.putExtra(EXTRA_SOURCE_ACTIVITY, sourceActivity)
                setResult(Activity.RESULT_OK, resultIntent)

                binding.btnCancel.text = "확인"

                // 인증 성공 후 자동으로 액티비티 종료
                handler.postDelayed({
                    finish()
                }, 500)
            } catch (e: Exception) {
                Log.e("PhoneAuthActivity", "Error processing successful auth: ${e.message}")
            }
        }

        // 실패 처리 공통 메소드
        private fun handleFailedAuth() {
            // 실패 처리 - 메시지만 표시
            Toast.makeText(context, "인증에 실패했습니다", Toast.LENGTH_SHORT).show()
            isAuthenticationSuccessful = false
            lastAuthenticatedPhone = null
        }
    }

    // 웹뷰 메모리 관리
    override fun onDestroy() {
        try {
            binding.webView.stopLoading()
            binding.webView.clearHistory()
            binding.webView.clearCache(true)
            binding.webView.clearFormData()
            binding.webView.destroy()
        } catch (e: Exception) {
            Log.e("PhoneAuthActivity", "웹뷰 정리 실패: ${e.message}")
        }
        super.onDestroy()
    }
}
