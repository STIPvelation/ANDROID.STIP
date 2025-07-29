package com.stip.stip.signup.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.stip.stip.signup.Constants
import com.stip.stip.signup.customview.LoadingDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

abstract class BaseFragment<T: ViewDataBinding, R: ViewModel>: Fragment() {

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, layoutResource, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner

        loadingPopup = LoadingDialog(binding.root.context)

        initStartView()
        initDataBinding()
        initAfterBinding()
    }

    override fun onDestroy() {
        disposableView.dispose()
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()

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
        Toast.makeText(activity?.baseContext, msg, Toast.LENGTH_SHORT).show()
    }

    protected fun showToast(msg: Int){
        Toast.makeText(activity?.baseContext, msg, Toast.LENGTH_SHORT).show()
    }

    protected fun setOnClick(view: View, callback: () -> Unit) {
        disposableView.add(
            RxView.clicks(view)
                .throttleFirst(Constants.TOUCH_THROTTLE_TIME, TimeUnit.MILLISECONDS)
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

    protected fun setRxTextChangeCallback(view: EditText, timeOut: Long, callback: (String) -> Unit) {
        disposableView.add(
            RxTextView.textChanges(view)
                .debounce(timeOut, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    callback.invoke(view.text.toString())
                }
        )
    }
}