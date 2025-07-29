package com.stip.stip.signup

import android.app.Application
import com.stip.stip.signup.utils.PreferenceUtil
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class STIPApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        PreferenceUtil.init(this)
    }
}