package com.stip.stip.more.fragment

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.stip.stip.R
import android.content.SharedPreferences


class StandardAlertFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_more_price_alert_standard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("alert_pref", Context.MODE_PRIVATE)

        // 모든 스위치를 준비 중으로 표시할 ID 목록
        val allSwitches = listOf(
            R.id.switch_login_alert,
            R.id.switch_transaction_alert,
            R.id.switch_deposit_withdrawal_alert,
            R.id.switch_order_alert,
            R.id.switch_staking_alert,
            R.id.switch_ip_webtoon_alert,
            R.id.switch_ip_auction_alert,
            R.id.switch_new_listing_alert,
            R.id.switch_ip_swap_alert
        )

        // 모든 스위치에 대해 준비중 메시지 표시
        for (id in allSwitches) {
            view.findViewById<SwitchCompat>(id)?.apply {
                isChecked = false
                isEnabled = true
                setOnCheckedChangeListener { _, isChecked ->
                    // 토글 후 다시 원래 상태로 되돌림
                    this.isChecked = !isChecked
                    Toast.makeText(requireContext(), "준비중", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ✅ 알림 설정으로 이동
        view.findViewById<View>(R.id.notification_settings_layout)?.setOnClickListener {
            val intent = android.content.Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            }
            startActivity(intent)
        }
    }

    // 모든 기능이 준비중이므로 setupSwitch 함수는 사용하지 않음
    // private fun setupSwitch() 함수 제거
}
