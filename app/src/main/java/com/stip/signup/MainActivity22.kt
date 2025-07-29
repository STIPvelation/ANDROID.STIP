package com.stip.stip.signup


// These classes are now in the same package as MainActivity22
// No need to import them separately
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLEncoder
import java.nio.charset.Charset
import com.stip.stip.R

class MainActivity22 : AppCompatActivity() {

    // API 호출을 위한 Retrofit 인터페이스
    private lateinit var apiService: ApiService

    // 인증 관련 필수 데이터
    private lateinit var tokenVersionId: TextView
    private lateinit var encData: TextView
    private lateinit var integrityValue: TextView

    // 표준창 호출 버튼
    private lateinit var buttonSubmit: Button

    // WebView 관련 변수
    private lateinit var webView: WebView
    private lateinit var toolbar: Toolbar
    private lateinit var closeButton: Button
    private var showNavigationBar = false  // 네비게이션 바 표시 여부
    private lateinit var formLayout: LinearLayout

    // 인증 성공 후 반환되는 데이터
    private lateinit var returnEncData: TextView
    private lateinit var returnIntegrityValue: TextView
    private lateinit var buttonDecrypt: Button

    // 복호화된 데이터 표시
    private lateinit var returnDecryptedData: TextView

    // JSON 파싱을 위한 Gson 객체
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_22)

        // UI 요소 초기화
        initializeViews()

        // Retrofit 설정
        val retrofit = Retrofit.Builder()
            .baseUrl("https://example.com/") // API 기본 URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // 초기 데이터 가져오기
        fetchData()

        // 버튼 클릭 이벤트 설정
        buttonSubmit.setOnClickListener {
            submitData()
        }

        // WebView 설정
        configureWebView()

        // 닫기 버튼 클릭 시 동작
        closeButton.setOnClickListener {
            resetWebView()
        }
    }

    /** UI 요소 초기화 */
    private fun initializeViews() {
        webView = findViewById(R.id.webView)
        tokenVersionId = findViewById(R.id.tokenVersionId)
        encData = findViewById(R.id.encData)
        integrityValue = findViewById(R.id.integrityValue)
        buttonSubmit = findViewById(R.id.buttonSubmit)
        formLayout = findViewById(R.id.formLayout)
        toolbar = findViewById(R.id.toolbar)
        closeButton = findViewById(R.id.closeButton)
        returnEncData = findViewById(R.id.returnEncData)
        returnIntegrityValue = findViewById(R.id.returnIntegrityValue)
        returnDecryptedData = findViewById(R.id.returnDcryptedData)
        buttonDecrypt = findViewById(R.id.buttonDecrypt)
    }

    /** WebView 설정 */
    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        webView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            webView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = webView.rootView.height
            val keyboardHeight = screenHeight - rect.bottom

            if (keyboardHeight > screenHeight * 0.15) { // 키보드가 올라왔는지 감지
                val params = webView.layoutParams
                params.height = rect.bottom // WebView 크기를 조정
                webView.layoutParams = params

                webView.postDelayed({
                    val scrollAmount = keyboardHeight / 7 // 여기서 조절하기
                    webView.scrollTo(0, scrollAmount)
                }, 100) // UI가 변경된 후 스크롤 반영
            } else {
                val params = webView.layoutParams
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
                webView.layoutParams = params
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                return handleCustomScheme(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url == "https://nice.checkplus.co.kr/Common/close.cb") {
                    resetWebView()
                }
                showNavigationBar = if (formLayout.visibility == View.VISIBLE) {
                    false
                } else {
                    url?.contains("nice.checkplus.co.kr") == true
                }
                updateNavigationBarVisibility()
            }
        }
    }

    /** 특정 스킴을 처리하는 함수 */
    private fun handleCustomScheme(url: String): Boolean {
        return when {
            url.startsWith("intent://") -> handleIntentScheme(url)
            url.startsWith("market://") -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                true
            }
            url.startsWith("nicesample://") -> handleNiceSampleScheme(url)
            else -> false
        }
    }

    /** Intent 스킴 처리 */
    private fun handleIntentScheme(url: String): Boolean {
        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            intent.getPackage()?.let {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$it")))
            }
        }
        return true
    }

    /** NICE 샘플 스킴 처리 */
    private fun handleNiceSampleScheme(url: String): Boolean {
        val uri = Uri.parse(url)
        val encData = uri.getQueryParameter("enc_data")
        val integrityValue = uri.getQueryParameter("integrity_value")
        if (!encData.isNullOrEmpty() && !integrityValue.isNullOrEmpty()) {
            runOnUiThread {
                returnEncData.text = "암호화 데이터: $encData"
                returnIntegrityValue.text = "무결성 값: $integrityValue"
                buttonDecrypt.visibility = View.VISIBLE
                webView.visibility = View.GONE
                toolbar.visibility = View.GONE
                formLayout.visibility = View.VISIBLE
                buttonDecrypt.setOnClickListener {
                    requestDecryption(encData, integrityValue)
                }
            }
        }
        return true
    }

    /** API에서 초기 데이터 가져오기 */
    private fun fetchData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getFormData().execute()
                response.body()?.let { data ->
                    runOnUiThread {
                        tokenVersionId.text = data.token_version_id
                        encData.text = data.enc_data
                        integrityValue.text = data.integrity_value
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** WebView를 통한 데이터 제출 */
    private fun submitData() {
        formLayout.visibility = View.GONE
        webView.visibility = View.VISIBLE
        val url = "https://nice.checkplus.co.kr/CheckPlusSafeModel/service.cb"
        val postData = "m=service" +
                "&token_version_id=" + URLEncoder.encode("202503242138470H-NC83CE091-D4ADA-5287521HFA", "UTF-8") +
                "&integrity_value=" + URLEncoder.encode("xWqY/EZ76oSYBUf7M+1dBJX/zXfudAtjKV/jVyrIQMQ=", "UTF-8") +
                "&enc_data=" + URLEncoder.encode("vVemQ/raJK5wafnThk1ZMQqWu4FAtG4kz459qdOQpGAfO26yHH482P7R7FGPsEHGocQNBLPm7ellrwvqjLfYp9CH8bapC6cNwklUkcEI73cZxAdPLk/LmzJZAo60OQrvfNfLZ7CpgeiGM5w5ENCTnsYoMDxeaufgvrg8x1ETUy5KSviFRD1Rv4b7pypiZmZD", "UTF-8")
        webView.postUrl(url, postData.toByteArray(Charset.forName("UTF-8")))
    }

    /** WebView 초기화 */
    private fun resetWebView() {
        webView.loadUrl("about:blank")
        webView.visibility = View.GONE
        formLayout.visibility = View.VISIBLE
    }

    /** 네비게이션 바 표시 여부 업데이트 */
    private fun updateNavigationBarVisibility() {
        toolbar.visibility = if (showNavigationBar) View.VISIBLE else View.GONE
    }

    /** 복호화 요청 */
    private fun requestDecryption(encData: String, integrityValue: String) {
        val requestData = DecryptionRequest(encData, integrityValue)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.decryptData(requestData)
                val responseJson = gson.toJson(response.body())

                withContext(Dispatchers.Main) {
                    returnDecryptedData.text = responseJson
                    webView.visibility = View.GONE
                    formLayout.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    returnDecryptedData.text = "네트워크 오류 발생"
                }
            }
        }
    }
}
