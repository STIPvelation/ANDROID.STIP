package com.stip.ipasset.ticker.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.stip.stip.R
import com.stip.stip.databinding.FragmentIpAssetTickerTransactionDialogBinding

/**
 * 티커 거래 결과를 보여주는 다이얼로그 프래그먼트
 */
class TickerTransactionDialogFragment : DialogFragment() {

    private var _binding: FragmentIpAssetTickerTransactionDialogBinding? = null
    private val binding get() = _binding!!
    
    private var title: String? = null
    private var message: String? = null
    private var confirmButtonText: String? = null
    private var confirmListener: (() -> Unit)? = null
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIpAssetTickerTransactionDialogBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 다이얼로그 크기 설정
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        
        // Bundle에서 데이터 가져오기
        arguments?.let { args ->
            title = args.getString(ARG_TITLE)
            message = args.getString(ARG_MESSAGE)
            confirmButtonText = args.getString(ARG_CONFIRM_BUTTON_TEXT)
        }
        
        // 뷰 설정
        with(binding) {
            title.text = this@TickerTransactionDialogFragment.title
            message.text = this@TickerTransactionDialogFragment.message
            confirmButton.text = confirmButtonText ?: getString(R.string.common_confirm)
            
            confirmButton.setOnClickListener {
                confirmListener?.invoke()
                dismiss()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        const val ARG_TITLE = "arg_title"
        const val ARG_MESSAGE = "arg_message"
        const val ARG_CONFIRM_BUTTON_TEXT = "arg_confirm_button_text"
        
        fun newInstance(title: String, message: String, confirmButtonText: String? = null, confirmListener: (() -> Unit)? = null): TickerTransactionDialogFragment {
            return TickerTransactionDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    confirmButtonText?.let { putString(ARG_CONFIRM_BUTTON_TEXT, it) }
                }
                this.confirmListener = confirmListener
            }
        }
    }
}
