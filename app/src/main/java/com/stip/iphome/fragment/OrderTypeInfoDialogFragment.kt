package com.stip.stip.iphome.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.stip.stip.databinding.DialogOrderTypeBinding

class OrderTypeInfoDialogFragment : DialogFragment() {

    private var _binding: DialogOrderTypeBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "OrderTypeInfoDialog"

        fun newInstance(): OrderTypeInfoDialogFragment {
            return OrderTypeInfoDialogFragment()
        }

        // ✨ 여기 추가!
        fun show(manager: FragmentManager) {
            newInstance().show(manager, TAG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogOrderTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonConfirm.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 다이얼로그 크기 조절
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.63).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}