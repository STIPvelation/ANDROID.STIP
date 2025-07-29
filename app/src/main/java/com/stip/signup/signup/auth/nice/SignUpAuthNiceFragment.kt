package com.stip.stip.signup.signup.auth.nice

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.stip.stip.R
import com.stip.stip.databinding.FragmentSignUpAuthNiceBinding
import com.stip.stip.signup.Constants
import com.stip.stip.signup.base.BaseFragment
import com.stip.stip.signup.customview.CustomContentDialog
import com.stip.stip.signup.signup.SignUpViewModel
import com.stip.stip.signup.utils.PreferenceUtil
import dagger.hilt.android.AndroidEntryPoint
import android.os.Handler
import android.os.Looper

@AndroidEntryPoint
class SignUpAuthNiceFragment :
    BaseFragment<FragmentSignUpAuthNiceBinding, SignUpAuthNiceViewModel>() {

    companion object {
        private const val TAG = "SignUpAuthNice"
        private const val WEB_VIEW_CLEANUP_DELAY_MS = 300L
        private const val AUTH_COMPLETION_DELAY_MS = 200L
    }

    override val layoutResource: Int
        get() = R.layout.fragment_sign_up_auth_nice

    override val viewModel: SignUpAuthNiceViewModel by viewModels()
    private val activityViewModel: SignUpViewModel by activityViewModels()
    private var isLogin: Boolean = false

    /** 인증번호 입력시 키보드가 화면 가림 문제 해결 */
    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        resumeWebView()
    }

    private fun resumeWebView() {
        try {
            // Removed redundant condition check
            binding.webView.resumeTimers()
            binding.webView.onResume()
        } catch (e: Exception) {
            Log.e(TAG, "WebView resume failed: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        pauseWebView()
    }

    private fun pauseWebView() {
        try {
            // Removed redundant condition check
            binding.webView.pauseTimers()
            binding.webView.onPause()
        } catch (e: Exception) {
            Log.e(TAG, "WebView pause failed: ${e.message}")
        }
    }

    override fun onDestroyView() {
        cleanupWebView()
        super.onDestroyView()
    }

    private fun cleanupWebView() {
        try {
            // Removed redundant condition check
            binding.webView.stopLoading()
            binding.webView.clearHistory()
            binding.webView.clearCache(true)
            binding.webView.clearFormData()
            binding.webView.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "WebView cleanup failed: ${e.message}")
        }
    }

    /** 로그인 성공 후 메인 화면으로 이동 */
    private fun navigateToMainActivity() {
        try {
            Log.d(TAG, "Navigating to MainActivity")
            val intent =
                Intent(requireActivity(), Class.forName("com.stip.stip.MainActivity"))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        } catch (e: Exception) {
            Log.e(TAG, "Navigation to MainActivity failed: ${e.message}")
        }
    }

    /** PIN 번호 설정 화면으로 이동 */
    private fun navigateToPinNumberSetting() {
        try {
            Log.d(TAG, "Navigating to PIN number setting screen")

            // 기본 로그인 타입을 PIN 번호로 설정
            PreferenceUtil.putString(
                Constants.PREF_KEY_BASIC_LOGIN_TYPE,
                Constants.BASIC_LOGIN_TYPE_PIN_NUMBER
            )

            val intent = Intent(
                requireActivity(),
                Class.forName("com.stip.stip.signup.login.LoginPinNumberActivity")
            )
            startActivity(intent)
            activity?.finish()
        } catch (e: Exception) {
            Log.e(TAG, "Navigation to PIN setting failed: ${e.message}")
            navigateToMainActivity() // 오류 시 메인 화면으로
        }
    }

    /** 생체인증 설정 화면으로 이동 */
    private fun navigateToBiometricSetting() {
        try {
            Log.d(TAG, "Navigating to biometric setting screen")

            // 기본 로그인 타입을 생체인증으로 설정
            PreferenceUtil.putString(
                Constants.PREF_KEY_BASIC_LOGIN_TYPE,
                Constants.BASIC_LOGIN_TYPE_BIOMETRIC_AUTH
            )

            val intent = Intent(
                requireActivity(),
                Class.forName("com.stip.stip.signup.login.LoginBiometricAuthActivity")
            )
            startActivity(intent)
            activity?.finish()
        } catch (e: Exception) {
            Log.e(TAG, "Navigation to biometric setting failed: ${e.message}")
            navigateToMainActivity() // 오류 시 메인 화면으로
        }
    }

    override fun initStartView() {
        initWebView()
        binding.webView.loadUrl(Constants.NICE_PAY_AUTH_URL)
        Log.d(TAG, "WebView initialized with NICE auth URL")
    }

    override fun initDataBinding() {
        observeViewModels()
    }

    private fun observeViewModels() {
        // 액티비티 ViewModel 관찰
        with(activityViewModel) {
            isLoginLiveData.observe(viewLifecycleOwner) {
                isLogin = it
                Log.d(TAG, "Login status updated: $isLogin")
            }
        }

        // 프래그먼트 ViewModel 관찰
        with(viewModel) {
            isLoading.observe(viewLifecycleOwner) {
                if (it) showProgress() else hideProgress()
            }

            observeNavigationEvents()
            observeApiEvents()
        }
    }

    private fun observeNavigationEvents() {
        viewModel.navigateToBankEvent.observe(viewLifecycleOwner) { di ->
            if (!isFragmentValid()) return@observe

            Log.d(TAG, "Navigation event with DI received")
            cleanupWebViewForNavigation()

            // 네비게이션 전 지연 추가
            binding.root.postDelayed({
                if (!isFragmentValid()) return@postDelayed

                // 유저 존재 여부 확인
                Log.d(TAG, "Requesting NICE auth verification for DI")
                viewModel.requestGetNiceAuth(di)

                // 회원 존재 여부에 따른 처리
                handleMemberExistence(di)
            }, WEB_VIEW_CLEANUP_DELAY_MS)
        }
    }

    private fun handleMemberExistence(di: String) {
        viewModel.memberExistenceLiveData.observe(viewLifecycleOwner) { memberExistence ->
            Log.d(TAG, "Member existence result: ${memberExistence.exists}")

            if (memberExistence.exists) {
                handleExistingMember(di)
            } else {
                handleNewMember(di)
            }
        }
    }

    private fun handleExistingMember(di: String) {
        Log.d(TAG, "Processing existing member flow")
//        showProgress()

        // DI 값 저장 및 회원 정보 요청
        PreferenceUtil.putString(Constants.PREF_KEY_DI_VALUE, di)
        viewModel.requestGetMemberInfo(di)

        // 로그인 상태 및 기본 로그인 타입 설정
        PreferenceUtil.putString(
            Constants.PREF_KEY_BASIC_LOGIN_TYPE,
            Constants.BASIC_LOGIN_TYPE_PIN_NUMBER
        )
        viewModel.setLoginState(true)

        // PIN 번호 설정 화면으로 이동
        navigateToPinNumberSetting()
    }

    private fun handleNewMember(di: String) {
        Log.d(TAG, "Processing new member flow")
        try {
            val bundle = Bundle().apply {
                putString(Constants.BUNDLE_AUTH_DI_KEY, di)
            }

            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.SignUpAuthNiceFragment) {
                Log.d(TAG, "Navigating to SignUpBankFragment")
                navController.navigate(
                    R.id.action_SignUpAuthNiceFragment_to_SignUpBankFragment,
                    bundle
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Navigation to bank fragment failed: ${e.message}")
        }
    }

    private fun observeApiEvents() {
        // 에러 처리
        viewModel.errorLiveData.observe(viewLifecycleOwner) {
            val errorMessage = when (it) {
                Constants.NETWORK_DUPLICATE_ERROR_CODE -> getString(R.string.error_duplicate_409)
                Constants.NETWORK_SERVER_ERROR_CODE -> getString(R.string.error_admin_500)
                else -> getString(R.string.error_network_999)
            }

            Log.e(TAG, "API error occurred: $it - $errorMessage")
            showErrorDialog(errorMessage)
        }

        // 회원 존재 여부 결과 처리
        viewModel.memberExistenceLiveData.observe(viewLifecycleOwner) {
            binding.webView.clearFocus()
            Log.d(TAG, "Member existence checked: ${it.toString()}")

            if (it.exists) {
                processExistingMember(it.di)
            } else {
                processNewMember(it.di)
            }
        }
    }

    private fun processExistingMember(di: String) {
        Log.d(TAG, "Processing existing member" + di)
//        showProgress()

        // DI 값 저장 및 회원 정보 요청
        PreferenceUtil.putString(Constants.PREF_KEY_DI_VALUE, di)
        Log.d(TAG, "Member info requested")

        // 로그인 설정
        PreferenceUtil.putString(
            Constants.PREF_KEY_BASIC_LOGIN_TYPE,
            Constants.BASIC_LOGIN_TYPE_PIN_NUMBER
        )
        Log.d(TAG, "Login state updated")
        viewModel.setLoginState(true)

        Log.d(TAG, "Navigating to MainActivity")

        // 로그인 옵션 다이얼로그 표시
        showLoginOptionsDialog()
    }

    private fun showLoginOptionsDialog() {
        Log.d(TAG, "Showing login options dialog")
        val loginOptionsDialog = CustomContentDialog(binding.root.context) {
            Log.d(TAG, "PIN number option selected")
            navigateToPinNumberSetting()
        }

        loginOptionsDialog.setText(
            getString(R.string.dialog_bank_guide_title),
            "휴대폰 인증이 완료되었습니다.\n로그인 방식을 선택해주세요.",
            "생체인증 사용",
            "PIN 번호 사용"
        )

        // 생체인증 옵션 리스너 설정
        try {
            val cancelBtn = loginOptionsDialog.findViewById<Button>(R.id.btn_cancel)
            cancelBtn?.setOnClickListener {
                Log.d(TAG, "Biometric option selected")
                loginOptionsDialog.dismiss()
                navigateToBiometricSetting()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Setting biometric button listener failed: ${e.message}")
        }
    }

    private fun processNewMember(di: String) {
        Log.d(TAG, "Processing new member")
        navigateToSignUpBank(di)
    }

    private fun navigateToSignUpBank(di: String) {
        try {
            val bundle = Bundle().apply {
                putString(Constants.BUNDLE_AUTH_DI_KEY, di)
            }

            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.SignUpAuthNiceFragment) {
                Log.d(TAG, "Navigating to SignUpBankFragment")
                navController.navigate(
                    R.id.action_SignUpAuthNiceFragment_to_SignUpBankFragment,
                    bundle
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Navigation to bank fragment failed: ${e.message}")
        }
    }

    private fun showErrorDialog(errorMessage: String) {
        CustomContentDialog(binding.root.context) {
            activity?.finish()
        }.setText(
            getString(R.string.dialog_bank_guide_title),
            errorMessage,
            "",
            getString(R.string.common_confirm)
        )
    }

    override fun initAfterBinding() {
        // 추가 초기화 작업이 필요한 경우 여기에 구현
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        // JavaScript Interface 추가
        binding.webView.addJavascriptInterface(WebAppInterface(this), "Android")
        binding.webView.webViewClient = createWebViewClient()

        Log.d(TAG, "WebView settings configured")
    }

    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                return handleCustomScheme(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url == "https://nice.checkplus.co.kr/Common/close.cb") {
                    Log.d(TAG, "NICE authentication page finished")
                }
            }
        }
    }

    /** 특정 스킴을 처리하는 함수 */
    private fun handleCustomScheme(url: String): Boolean {
        return when {
            url.startsWith("intent://") -> {
                Log.d(TAG, "Handling intent scheme: $url")
                handleIntentScheme(url)
            }

            url.startsWith("market://") -> {
                Log.d(TAG, "Opening market URL: $url")
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                true
            }

            else -> false
        }
    }

    /** Intent 스킴 처리 */
    private fun handleIntentScheme(url: String): Boolean {
        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        try {
            Log.d(TAG, "Starting activity for intent: ${intent.action}")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "App not installed, redirecting to Play Store")
            intent.getPackage()?.let {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$it")
                    )
                )
            }
        }
        return true
    }

    private fun isFragmentValid(): Boolean {
        return isAdded && !isRemoving && !isDetached && view != null
    }

    private fun cleanupWebViewForNavigation() {
        binding.webView.clearHistory()
        binding.webView.removeAllViews()
        binding.webView.clearCache(true)
        binding.webView.loadUrl("about:blank")
        Log.d(TAG, "WebView cleaned up for navigation")
    }

    /** JavaScript에서 호출할 메서드를 제공하는 클래스 */
    class WebAppInterface(private val fragment: SignUpAuthNiceFragment) {
        // 인증 완료 상태를 추적하기 위한 변수
        private var isAuthCompleted = false
        private val handler = Handler(Looper.getMainLooper())

        // 인증 완료 후 네이티브 앱에 결과 전달 (예: "success" 또는 "fail")
        @JavascriptInterface
        fun onAuthCompleted(result: String, di: String) {
            // 중복 호출 방지
            if (isAuthCompleted) {
                Log.d(TAG, "Auth already completed, ignoring duplicate call")
                return
            }
            isAuthCompleted = true

            Log.d(TAG, "Auth completed with result: $result, DI: $di")

            // UI 스레드에서 안전하게 처리
            handler.post {
                if (result == "success") {
                    handleSuccessfulAuth(di)
                } else {
                    handleFailedAuth()
                }
            }
        }

        private fun handleSuccessfulAuth(di: String) {
            try {
                Log.d(TAG, "Processing successful authentication")

                // 웹뷰 정리
                cleanupWebView()

                // DI 값 저장
                PreferenceUtil.putString(Constants.PREF_KEY_DI_VALUE, di)
                // 회원 DI 저장 (전화번호 변경 시 비교용)
                PreferenceUtil.saveMemberDi(di)

                // 프래그먼트가 유효한지 확인 후 네비게이션 진행
                if (fragment.isFragmentValid()) {
                    // 지연 추가하여 웹뷰 정리 시간 확보
                    handler.postDelayed({
                        if (fragment.isFragmentValid()) {
                            Log.d(TAG, "Setting DI value and triggering navigation")
                            fragment.viewModel.setDiValue(di)
                        }
                    }, AUTH_COMPLETION_DELAY_MS)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing successful auth: ${e.message}")
            }
        }

        private fun handleFailedAuth() {
            try {
                Log.d(TAG, "Processing failed authentication")
                if (fragment.isFragmentValid()) {
                    val navController = fragment.findNavController()
                    if (navController.currentDestination?.id == R.id.SignUpAuthNiceFragment) {
                        Log.d(TAG, "Navigating back due to auth failure")
                        navController.popBackStack()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing failed auth: ${e.message}")
            }
        }

        private fun cleanupWebView() {
            try {
                with(fragment.binding.webView) {
                    stopLoading()
                    clearHistory()
                    removeAllViews()
                }
                Log.d(TAG, "WebView cleaned up after auth")
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up WebView: ${e.message}")
            }
        }

        companion object {
            private const val TAG = "WebAppInterface"
        }
    }
}
