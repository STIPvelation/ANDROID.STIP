package com.stip.stip.iphome.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.stip.stip.databinding.DialogLicenseAgreementBinding

class LicenseAgreementDialogFragment : DialogFragment() {

    private var _binding: DialogLicenseAgreementBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLicenseAgreementBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ❌ setText 하지 마세요
        // 텍스트는 XML 안에 미리 정의되어 있다고 가정
        // 예: android:text="@string/license_text" ← xml 내부에서 처리됨

        binding.buttonCloseLicenseDialog.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "LicenseAgreementDialog"

        fun newInstance(): LicenseAgreementDialogFragment {
            return LicenseAgreementDialogFragment()
        }
    }
}
