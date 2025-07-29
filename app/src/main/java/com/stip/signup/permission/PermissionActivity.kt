package com.stip.stip.signup.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.Html
import androidx.activity.viewModels
import com.stip.stip.R
import com.stip.stip.signup.base.BaseActivity
import com.stip.stip.databinding.ActivityPermissionBinding
import com.stip.stip.signup.login.LoginActivity
import com.stip.stip.signup.Constants
import com.stip.stip.signup.utils.PreferenceUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionActivity: BaseActivity<ActivityPermissionBinding, PermissionViewModel>() {

    companion object {
        fun startPermissionActivity(
            activity: Activity,
        ) {
            val intent = Intent(activity, PermissionActivity::class.java).apply {
            }
            activity.startActivity(intent)
            activity.finish()
        }
    }

    override val layoutResource: Int
        get() = R.layout.activity_permission

    override val viewModel: PermissionViewModel by viewModels()

    override fun initStartView() {
        val result = String.format(binding.root.context.getString(R.string.permission_title))
        binding.tvPermissionExplain.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(result, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(result)
        }
    }

    override fun initDataBinding() {
    }

    override fun initAfterBinding() {
        setOnClick(binding.btnOk) {
            // Save that user has seen permission screen
            PreferenceUtil.putBoolean("PREF_KEY_PERMISSION_SHOWN", true)
            
            // Request permissions if needed (Android M and above)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    100
                )
            }
            
            // Proceed to login activity
            LoginActivity.startLoginActivity(this)
        }
    }
}