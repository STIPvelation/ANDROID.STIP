package com.stip.stip.signup.customview

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import com.stip.stip.R
import com.stip.stip.databinding.DialogBiometricAuthBinding

class BiometricAuthDialog(
    context: Context,
    private val biometricCallback: (() -> Unit),
    private val pinNumberCallback: (() -> Unit)
): Dialog(context, R.style.PopUpDialog) {

    private lateinit var binding: DialogBiometricAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.dialog_biometric_auth, null, false)
        setContentView(binding.root)

        window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setDimAmount(0.5f)
            attributes = attributes.apply {
                gravity = android.view.Gravity.CENTER
            }
        }

        binding.ivClose.setOnClickListener {
            dismiss()
        }

        binding.clBiometricAuthSection.setOnClickListener {
            biometricCallback.invoke()
            dismiss()
        }

        binding.clPinSection.setOnClickListener {
            pinNumberCallback.invoke()
            dismiss()
        }
    }
}