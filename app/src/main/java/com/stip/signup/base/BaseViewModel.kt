package com.stip.stip.signup.base

import androidx.lifecycle.ViewModel
import com.stip.stip.signup.utils.MutableSingleLiveData
import com.stip.stip.signup.utils.SingleLiveData
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

open class BaseViewModel: ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    fun addDisposable(disposable: Disposable){
        compositeDisposable.add(disposable)
    }

    override fun onCleared() {
        compositeDisposable.clear()
        super.onCleared()
    }

    private val _isLoading = MutableSingleLiveData(false)
    val isLoading: SingleLiveData<Boolean> get() = _isLoading

    fun showProgress() {
        _isLoading.postValue(true)
    }

    fun hideProgress() {
        _isLoading.postValue(false)
    }
}