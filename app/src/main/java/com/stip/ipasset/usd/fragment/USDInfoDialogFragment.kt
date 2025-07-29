package com.stip.ipasset.usd.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.stip.stip.R

/**
 * USD 관련 정보를 보여주는 다이얼로그 프래그먼트
 * 여러 정보 다이얼로그를 하나의 클래스로 관리
 */
class USDInfoDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 전달받은 레이아웃 ID로 다이얼로그 레이아웃 설정
        val layoutResId = when (arguments?.getString("dialog_layout")) {
            "dialog_ip_asset_usd_withdrawal_available_info" -> 
                R.layout.dialog_ip_asset_usd_withdrawal_available_info
            "dialog_ip_asset_usd_withdrawal_limit_info" -> 
                R.layout.dialog_ip_asset_usd_withdrawal_limit_info
            else -> R.layout.dialog_ip_asset_usd_withdrawal_available_info // 기본값
        }
        
        return inflater.inflate(layoutResId, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 다이얼로그 닫기 버튼 설정
        view.findViewById<View>(R.id.btn_close)?.setOnClickListener {
            dismiss()
        }
        
        // 확인 버튼 클릭 처리 (레이아웃에 따라 ID가 다를 수 있음)
        view.findViewById<View>(R.id.btn_confirm)?.setOnClickListener {
            dismiss()
        }
        
        // 대체 확인 버튼 ID 처리 (layout에 따라 다른 ID를 가질 수 있음)
        view.findViewById<View>(R.id.btn_ok)?.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        
        // 다이얼로그 너비를 화면 너비의 90%로 설정
        dialog?.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            val params = attributes
            params.width = width
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes = params
        }
    }
}
