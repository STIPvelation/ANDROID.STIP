package com.stip.stip.more.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.stip.stip.MainActivity
import com.stip.stip.MainViewModel
import com.stip.stip.R
import com.stip.stip.databinding.FragmentMoreSecurityBinding

class MoreSecurityFragment : Fragment() {

    private var _binding: FragmentMoreSecurityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreSecurityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.updateHeaderTitle(getString(R.string.title_security_auth))
        viewModel.enableBackNavigation {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        initializeUI()
    }

    override fun onResume() {
        super.onResume()
        // 화면에 재진입할 때마다 상태 갱신
        updateOverseasLoginBlockStatus()
        updateBiometricSwitchState()
    }

    private fun updateOverseasLoginBlockStatus() {
        val sharedPref = requireContext().getSharedPreferences("security_pref", Context.MODE_PRIVATE)
        val isBlocked = sharedPref.getBoolean("overseas_login_blocked", false)
        binding.tvOverseasLoginBlockStatus.text = if (isBlocked) "활성화" else "꺼짐"
        
        // 텍스트 색상 설정: 활성화 상태일 때만 #30C6E8 색상으로 표시
        binding.tvOverseasLoginBlockStatus.setTextColor(
            if (isBlocked) Color.parseColor("#30C6E8") else Color.GRAY
        )
    }

    private fun updateBiometricSwitchState() {
        val sharedPrefBio = requireContext().getSharedPreferences("security_pref", Context.MODE_PRIVATE)
        val isBiometricEnabled = sharedPrefBio.getBoolean("biometric_enabled", false)
        binding.switchBiometricAuth.isEnabled = true
        binding.switchBiometricAuth.isChecked = isBiometricEnabled
    }

    private fun initializeUI() {
        // 해외 로그인 차단 박스 클릭 이벤트 처리 - OverseasLoginBlockActivity로 이동
        binding.itemOverseasLoginBlock.setOnClickListener {
            try {
                val intent = Intent(requireContext(), com.stip.stip.more.activity.OverseasLoginBlockActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "해외 로그인 차단 설정 화면이 준비 중입니다", Toast.LENGTH_SHORT).show()
                Log.e("MoreSecurityFragment", "OverseasLoginBlockActivity error: ${e.message}")
            }
        }

        // 로그인 이력 클릭 이벤트 처리
        binding.itemLoginHistory.setOnClickListener {
            try {
                viewModel.updateHeaderTitle(getString(R.string.login_history_title_manage))
                (activity as? MainActivity)?.navigateToLoginHistory()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "로그인 이력 화면이 준비 중입니다", Toast.LENGTH_SHORT).show()
                Log.e("MoreSecurityFragment", "Login history navigation error: ${e.message}")
            }
        }

        // PIN 비밀번호 변경 클릭 이벤트 처리
        binding.itemPinChange.setOnClickListener {
            try {
                val intent = Intent(requireContext(), Class.forName("com.stip.stip.more.activity.PinVerificationActivity"))
                intent.putExtra("is_pin_change", true)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "PIN 비밀번호 변경 화면이 준비 중입니다", Toast.LENGTH_SHORT).show()
                Log.e("MoreSecurityFragment", "PIN verification error: ${e.message}")
            }
        }

        // 생체인증 설정 박스 클릭 이벤트 처리
        binding.itemBiometricAuth.setOnClickListener {
            try {
                val intent = Intent(requireContext(), Class.forName("com.stip.stip.more.activity.PinVerificationActivity"))
                intent.putExtra("is_pin_change", false)
                intent.putExtra("is_biometric_setup", true)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "생체인증 설정 화면이 준비 중입니다", Toast.LENGTH_SHORT).show()
                Log.e("MoreSecurityFragment", "Biometric setup error: ${e.message}")
            }
        }

        // 생체인증 스위치 상태 변경 이벤트
        binding.switchBiometricAuth.setOnCheckedChangeListener { _, isChecked ->
            val sharedPrefBio = requireContext().getSharedPreferences("security_pref", Context.MODE_PRIVATE)
            sharedPrefBio.edit().putBoolean("biometric_enabled", isChecked).apply()

            // 생체인증 사용 여부에 따라 메시지 표시
            if (isChecked) {
                Toast.makeText(requireContext(), "생체인증이 활성화되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "생체인증이 비활성화되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 초기 상태 갱신
        updateOverseasLoginBlockStatus()
        updateBiometricSwitchState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
