package com.stip.stip.signup.main

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.stip.stip.databinding.ActivityMainSignUpBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stip.stip.R
import com.stip.stip.signup.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainSignUpActivity: BaseActivity<ActivityMainSignUpBinding, MainSignUpViewModel>() {
    override val layoutResource: Int
        get() = R.layout.activity_main_sign_up

    override val viewModel: MainSignUpViewModel by viewModels()

    private var backPressedTime: Long = 0L
    private val backToastTimeLimit = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navController = findNavController(R.id.nav_host_fragment_activity_main)
                val currentDestination = navController.currentDestination?.id

                if (currentDestination != R.id.navigation_stock_exchange) {
                    // 다른 탭일 때 → 스톡 프래그먼트로 이동
                    navController.navigate(R.id.navigation_stock_exchange)
                } else {
                    // 스톡 프래그먼트일 때 → 두 번 누르면 종료
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - backPressedTime < backToastTimeLimit) {
                        finish()
                    } else {
                        backPressedTime = currentTime
                        showToast(R.string.common_app_end)
                    }
                }
            }
        })
    }

    override fun initStartView() {
        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)

        binding.navView.itemIconTintList = null
    }

    override fun initDataBinding() {
    }

    override fun initAfterBinding() {
    }
}