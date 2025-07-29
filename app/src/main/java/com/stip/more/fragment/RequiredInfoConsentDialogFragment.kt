package com.stip.stip.more.fragment // 실제 패키지 경로 확인

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast // Toast import 추가
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.stip.stip.R // 실제 R 경로 확인
import com.stip.stip.databinding.DialogRequiredInfoConsentBinding // ViewBinding 클래스 이름 확인

class RequiredInfoConsentDialogFragment : DialogFragment() {

    private var _binding: DialogRequiredInfoConsentBinding? = null
    private val binding get() = _binding!!

    // Activity/Fragment와 통신하기 위한 리스너 인터페이스 (유지)
    interface ConsentListener {
        fun onConsentStartClicked()
    }
    private var listener: ConsentListener? = null

    // 리스너 설정 함수 (유지)
    fun setConsentListener(listener: ConsentListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRequiredInfoConsentBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isCancelable = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateConsentButtonState(false)

        binding.checkboxAgree.setOnCheckedChangeListener { _, isChecked ->
            updateConsentButtonState(isChecked)
        }

        binding.buttonCancelConsent.setOnClickListener {
            dismiss()
        }

        // --- 시작 버튼 리스너 수정 ---
        binding.buttonStartConsent.setOnClickListener {
            if (it.isEnabled) {
                // ▼▼▼ Toast 메시지 표시 추가 ▼▼▼
                Toast.makeText(requireContext(), "준비중", Toast.LENGTH_SHORT).show()
                // ▲▲▲ 추가 끝 ▲▲▲

                // 리스너 호출 및 다이얼로그 닫기는 그대로 유지
                listener?.onConsentStartClicked()
                dismiss()
            }
        }
        // --- 수정 끝 ---
    }

    override fun onResume() {
        super.onResume()
        // 다이얼로그 크기 조절
        try {
            val windowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowManager.currentWindowMetrics.bounds
            } else {
                val point = Point()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getSize(point)
                point
            }
            val width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                (display as android.graphics.Rect).width() * 0.90
            } else {
                (display as Point).x * 0.90
            }
            dialog?.window?.setLayout(width.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        } catch (e: Exception) {
            Log.e(TAG, "Error resizing dialog", e)
        }
    }


    private fun updateConsentButtonState(isEnabled: Boolean) {
        _binding?.let { currentBinding ->
            currentBinding.buttonStartConsent.isEnabled = isEnabled
            val context = requireContext()
            if (isEnabled) {
                currentBinding.buttonStartConsent.backgroundTintList = ContextCompat.getColorStateList(context, R.color.main_point)
                currentBinding.buttonStartConsent.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                currentBinding.buttonStartConsent.backgroundTintList = ContextCompat.getColorStateList(context, R.color.button_disabled_grey)
                currentBinding.buttonStartConsent.setTextColor(ContextCompat.getColor(context, R.color.button_disabled_text_grey))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RequiredInfoConsentDialog"
        fun newInstance(): RequiredInfoConsentDialogFragment {
            return RequiredInfoConsentDialogFragment()
        }
    }
}