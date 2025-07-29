package com.stip.stip.more.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.fragment.app.Fragment
import com.stip.stip.R

class MarketPriceAlertFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_more_price_alert_market, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("alert_pref", Context.MODE_PRIVATE)

        // 모든 스위치를 준비 중으로 표시할 ID 목록
        val allSwitches = listOf(
            R.id.switch_dnd_rest,
            R.id.switch_owned_ip,
            R.id.switch_comp_designated,
            R.id.switch_comp_rise_fall,
            R.id.switch_comp_report_low,
            R.id.switch_comp_ip_cross,
            R.id.switch_comp_new_user,
            R.id.switch_comp_revoke_user
        )

        // 모든 스위치에 대해 준비중 메시지 표시
        for (id in allSwitches) {
            view.findViewById<SwitchMaterial>(id)?.apply {
                // 확실하게 OFF 상태로 설정
                this.isChecked = false
                isEnabled = true
                setOnCheckedChangeListener { buttonView, isChecked ->
                    // 토글 후 다시 OFF 상태로 되돌림
                    buttonView.isChecked = false
                    Toast.makeText(requireContext(), "준비중", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
