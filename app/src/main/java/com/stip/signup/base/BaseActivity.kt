package com.stip.stip.signup.base

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.stip.stip.signup.Constants.TOUCH_THROTTLE_TIME
import com.stip.stip.signup.customview.LoadingDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

abstract class BaseActivity<T: ViewDataBinding, R: BaseViewModel>: AppCompatActivity() {

    lateinit var binding: T

    abstract val layoutResource: Int

    abstract val viewModel: R

    private var disposableView = CompositeDisposable()

    private var loadingPopup: LoadingDialog? = null

    /**
     * 레이아웃 띄운 직후 호출
     * view or Activity 속성 등 초기화
     * ex)_ 리싸이클러뷰, 툴바, 드로어뷰
     */
    abstract fun initStartView()

    /**
     * 데이터 바인딩 및 RxJava 설정
     * ex)_ RxJava Observe, DataBinding Observe
     */
    abstract fun initDataBinding()

    /**
     * 바인딩 이후에 할 일을 여기에 구현
     * 그 외에 설정할 것이 있으면 여기서 설정
     * 클릭 리스너도 설정
     */
    abstract fun initAfterBinding()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, layoutResource)
        binding.lifecycleOwner

        loadingPopup = LoadingDialog(this)

        initStartView()
        initDataBinding()
        initAfterBinding()

        statusBarSetWhiteColor()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableView.clear()

        if (loadingPopup != null && loadingPopup?.isShowing == true) {
            loadingPopup?.dismiss()
            loadingPopup = null
        }
    }

    open fun showLoadingDialog(){
        loadingPopup?.show()
    }

    open fun hideLoadingDialog(){
        loadingPopup?.dismiss()
    }

    protected fun showToast(msg: String){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    protected fun showToast(msg: Int){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    protected fun setOnClick(view: View, callback: () -> Unit) {
        disposableView.add(
            RxView.clicks(view)
                .throttleFirst(TOUCH_THROTTLE_TIME, TimeUnit.MILLISECONDS)
                .subscribe {
                    callback.invoke()
                }
        )
    }

    protected fun setRxTextChangeCallback(view: EditText, callback: (String) -> Unit) {
        disposableView.add(
            RxTextView.textChanges(view)
                .debounce(200L, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    callback.invoke(view.text.toString())
                }
        )
    }

    private fun statusBarSetWhiteColor() {
        // StatusBar 색상 변경
        window.statusBarColor = ContextCompat.getColor(this, com.stip.stip.R.color.white)

        // StatusBar 색상 변경 후 텍스트 보이지 않아 텍스트 색상 변경
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }
}