package com.stip.stip.signup.signup

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.stip.stip.signup.Constants
import com.stip.stip.R
import com.stip.stip.databinding.ActivitySignUpOcrWebViewBinding
import com.stip.stip.signup.utils.PreferenceUtil
import com.stip.stip.signup.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder

@AndroidEntryPoint
class SignUpOCRWebViewActivity : AppCompatActivity() {

    private var binding: ActivitySignUpOcrWebViewBinding? = null
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var detail = ""

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_ocr_web_view)
        // 디버깅 실정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // 바인딩 설정
        binding = ActivitySignUpOcrWebViewBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        // 웹뷰 설정
        webView = binding?.webview
        webView?.settings?.javaScriptEnabled = true
        webView?.webViewClient = WebViewClient()
        webView?.webChromeClient = WebChromeClient()
        webView?.addJavascriptInterface(this, "alcherakyc")

        // 카메라 프리뷰 성능
        webView?.clearCache(true);
        webView?.clearHistory();

        // 사용자 데이터 인코딩
        val userInfo = ""
        val encodedUserInfo: String = encodeJson(userInfo)
        // POST
        postUserInfo(encodedUserInfo)
    }

    /*// WebView 액티비티에서 뒤로가기 버튼 막기
    override fun onBackPressed() {
        //super.onBackPressed();
    }*/

    private fun postUserInfo(encodedUserInfo: String) {

        handler.post { // 카메라 권한 요청
            cameraAuthRequest()
            webView?.loadUrl("https://kyc.useb.co.kr/auth")
            webView?.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    webView?.loadUrl("javascript:alcherakycreceive('$encodedUserInfo')")
                }
            }
        }
    }

    private fun getData(): JSONObject {
        val birthday = Utils.birthDayFormatDate(PreferenceUtil.getString(Constants.PREF_KEY_OCR_BIRTHDAY_VALUE))
        val name = PreferenceUtil.getString(Constants.PREF_KEY_OCR_NAME_VALUE)
        val phoneNumber = PreferenceUtil.getString(Constants.PREF_KEY_OCR_PHONE_NUMBER_VALUE)
        val email = PreferenceUtil.getString(Constants.PREF_KEY_OCR_EMAIL_VALUE)
        val isWasmOCRMode = "true"
        val isWasmSSAMode = "false"

        return dataToJson(birthday, name, phoneNumber, email, isWasmOCRMode, isWasmSSAMode);
    }

    private fun dataToJson(
        birthday: String?,
        name: String?,
        phoneNumber: String?,
        email: String?,
        isWasmOCRMode: String,
        isWasmSSAMode: String
    ): JSONObject {

        var jsonObject = JSONObject()
        jsonObject.put("customer_id", Constants.USBE_OCR_CUSTOMER_ID)
        jsonObject.put("id", Constants.USBE_OCR_ID)
        jsonObject.put("key", Constants.USBE_OCR_KEY)
        jsonObject.put("name", name)
        jsonObject.put("birthday", birthday)
        jsonObject.put("phone_number", phoneNumber)
        jsonObject.put("email", email)
        jsonObject.put("isWasmOCRMode", isWasmOCRMode);
        jsonObject.put("isWasmSSAMode", isWasmSSAMode);

        return jsonObject
    }

    private fun encodeURIComponent(encoded: String): String {

        var encodedURI = URLEncoder.encode(encoded, "UTF-8")
        encodedURI = encodedURI.replace("\\+".toRegex(), "%20")
        encodedURI = encodedURI.replace("%21".toRegex(), "!")
        encodedURI = encodedURI.replace("%27".toRegex(), "'")
        encodedURI = encodedURI.replace("%28".toRegex(), "(")
        encodedURI = encodedURI.replace("%29".toRegex(), ")")
        encodedURI = encodedURI.replace("%7E".toRegex(), "~")
        return encodedURI
    }

    private fun encodeJson(data: String): String {

        var data = data
        var encodedData: String? = null
        data = encodeURIComponent(getData().toString())
        encodedData = Base64.encodeToString(data.toByteArray(), 0)
        return encodedData
    }

    @JavascriptInterface
    @Throws(JSONException::class)
    fun receive(data: String?) {
        // Handle potential null safely
        val decodedData = decodedReceiveData(data.orEmpty())
        // JSONObject constructor requires non-null string
        var JsonObject = JSONObject(decodedData ?: "{}")
        var resultData = ""
        try {
            JsonObject = ModifyReviewResult(JsonObject)
            resultData = JsonObject.getString("result")
        } catch (e: JSONException) {
            resultData = JsonObject.getString("result")
        }
        if (resultData == "success") {
            detail = JsonObject.toString(4)
            Log.d("success", "KYC 작업이 성공했습니다.")
            val intent = Intent().apply {
                putExtra("kyc_result", resultData)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        } else if (resultData == "failed") {
            detail = JsonObject.toString(4)
            Log.d("failed", "KYC 작업이 실패했습니다.")
            val intent = Intent().apply {
                putExtra("kyc_result", resultData)
            }
            setResult(Activity.RESULT_CANCELED, intent)
            finish()
        }
        if (resultData == "complete") {
            detail = JsonObject.toString(4)
            Log.d("complete", "KYC가 완료되었습니다.")
            val intent = Intent().apply {
                putExtra("kyc_result", resultData)
            }
            setResult(Activity.RESULT_CANCELED, intent)
            finish()
        } else if (resultData == "close") {
            detail = JsonObject.toString(4)
            Log.d("close", "KYC가 완료되지 않았습니다.")
            val intent = Intent().apply {
                putExtra("kyc_result", resultData)
            }
            setResult(Activity.RESULT_CANCELED, intent)
            finish()
        }

        /*
        val intent = Intent(applicationContext, ReportActivity::class.java)
        intent.putExtra("detail", detail)
        intent.putExtra("result", result)
        startActivity(intent)

        finish()
         */
    }

    @Throws(JSONException::class)
    private fun ModifyReviewResult(JsonObject: JSONObject): JSONObject {
        val reviewResult = JsonObject.getString("review_result")
        val reviewResultJsonObject = JSONObject(reviewResult)
        val image = reviewResultJsonObject.getString("id_card")
        val idCardJsonObject = JSONObject(image)
        var idCardImage = idCardJsonObject.getString("id_card_image")
        var idCardOrigin = idCardJsonObject.getString("id_card_origin")
        var idCropImage = idCardJsonObject.getString("id_crop_image")
        if (idCardImage !== "null") {
            idCardImage = idCardImage.substring(0, 20) + "...생략(omit)..."
            idCardJsonObject.put("id_card_image", idCardImage)
        }
        if (idCardOrigin !== "null") {
            idCardOrigin = idCardOrigin.substring(0, 20) + "...생략(omit)..."
            idCardJsonObject.put("id_card_origin", idCardOrigin)
        }
        if (idCropImage !== "null") {
            idCropImage = idCropImage.substring(0, 20) + "...생략(omit)..."
            idCardJsonObject.put("id_crop_image", idCropImage)
        }
        reviewResultJsonObject.put("id_card", idCardJsonObject)
        val faceCheck = reviewResultJsonObject.getString("face_check")
        val faceCheckObject = JSONObject(faceCheck)
        var faceImage = faceCheckObject.getString("selfie_image")
        if (faceImage !== "null") {
            faceImage = faceImage.substring(0, 20) + "...생략(omit)..."
            faceCheckObject.put("selfie_image", faceImage)
        }
        reviewResultJsonObject.put("face_check", faceCheckObject)
        JsonObject.put("review_result", reviewResultJsonObject)
        return JsonObject
    }


    fun decodedReceiveData(data: String?): String? {
        val decoded = String(Base64.decode(data, 0))
        return decodeURIComponent(decoded)
    }

    private fun decodeURIComponent(decoded: String): String? {
        var decodedURI: String? = null
        try {
            decodedURI = URLDecoder.decode(decoded, "UTF-8")
                .replace("%20".toRegex(), "\\+")
                .replace("!".toRegex(), "\\%21")
                .replace("'".toRegex(), "\\%27")
                .replace("\\(".toRegex(), "\\%28")
                .replace("\\)".toRegex(), "\\%29")
                .replace("~".toRegex(), "\\%7E")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return decodedURI
    }

    private fun cameraAuthRequest() {

        webView = binding?.webview
        val webSettings = webView?.settings
        webSettings?.mediaPlaybackRequiresUserGesture = false
        webView?.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {

                //API레벨이 21이상인 경우
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val requestedResources = request.resources
                    for (r in requestedResources) {
                        if (r == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                            request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                            break
                        }
                    }
                }
            }
        }
        val cameraPermissionCheck =
            ContextCompat.checkSelfPermission(this@SignUpOCRWebViewActivity, Manifest.permission.CAMERA)
        if (cameraPermissionCheck != PackageManager.PERMISSION_GRANTED) { // 권한이 없는 경우
            ActivityCompat.requestPermissions(
                this@SignUpOCRWebViewActivity,
                arrayOf(Manifest.permission.CAMERA),
                1000
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this@SignUpOCRWebViewActivity,
                    R.string.common_camera_permission,
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
}