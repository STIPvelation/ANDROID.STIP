package com.stip.stip.more.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.stip.stip.MainViewModel
import com.stip.stip.R
import com.stip.stip.api.repository.IpListingRepository
import com.stip.stip.databinding.FragmentMoreModeSettingBinding
import com.stip.stip.iphome.TradingDataHolder
import com.stip.stip.iphome.fragment.IpHomeFragment
import kotlinx.coroutines.launch
import java.util.*

class MoreModeSettingFragment : Fragment() {

    private var _binding: FragmentMoreModeSettingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private val TAG = "MoreModeSettingFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreModeSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.updateHeaderTitle(getString(R.string.title_mode_setting))
        viewModel.enableBackNavigation {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        // IP 새로고침 버튼 클릭 이벤트는 UI에 버튼이 없으므로 제거
        // 필요 시 나중에 버튼을 레이아웃에 추가하고 다시 구현

        val sharedPref = requireContext().getSharedPreferences("mode_pref", Context.MODE_PRIVATE)
        val keepScreenOn = sharedPref.getBoolean("keep_screen_on", false)
        
        // 꺼짐 방지 모드 초기값 설정
        binding.switchScreenOn.isChecked = keepScreenOn
        setKeepScreenOnFlag(keepScreenOn)
        
        // 스위치 XML에 MaterialSwitch가 사용되었으며, 테마 색상이 자동으로 적용됩니다
        // 추가 스타일링은 필요한 경우 res/color 폴더에 ColorStateList XML을 정의하여 사용할 수 있음
        
        binding.switchScreenOn.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("keep_screen_on", isChecked).apply()
            setKeepScreenOnFlag(isChecked)
            // 토글 상태 변경만으로 자동 스타일링 적용
        }

        binding.switchDisplayNormal.apply {
            isChecked = true
            setOnTouchListener { _, _ -> true }
        }

        listOf(binding.switchDisplayDark, binding.switchDisplayDesign, binding.switchDisplayUser).forEach { switch ->
            switch.isChecked = false
            switch.isEnabled = true
            switch.setOnCheckedChangeListener { button, _ ->
                button.isChecked = false
                Toast.makeText(requireContext(), "준비중", Toast.LENGTH_SHORT).show()
            }
        }

        setupLanguageToggle()
    }

    private fun setupLanguageToggle() {
        val languageMap = mapOf(
            "ko" to binding.switchLanguageKorean,
            "en" to binding.switchLanguageEnglish,
            "ja" to binding.switchLanguageJapanese,
            "zh" to binding.switchLanguageChinese
        )

        // 현재 설정된 언어 확인
        val currentLang = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "ko"

        // 색상 정의
        val onColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#30C6E8")) // 파란색
        val offColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#CCCCCC")) // 회색

        // 초기 상태 반영 및 색상 설정
        languageMap.forEach { (lang, switch) ->
            switch.setOnCheckedChangeListener(null)
            val isSelected = (lang == currentLang)
            switch.isChecked = isSelected
            // ON/OFF 상태에 따라 색상 설정
            switch.trackTintList = if (isSelected) onColor else offColor
        }

        // 리스너 설정
        languageMap.forEach { (lang, switch) ->
            switch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // 선택된 토글은 파란색으로 설정
                    switch.trackTintList = onColor
                    
                    // 다른 토글은 모두 회색으로 설정
                    languageMap.forEach { (otherLang, otherSwitch) ->
                        if (otherLang != lang) {
                            otherSwitch.setOnCheckedChangeListener(null)
                            otherSwitch.isChecked = false
                            otherSwitch.trackTintList = offColor
                            otherSwitch.setOnCheckedChangeListener { _, newState -> 
                                if (newState) {
                                    handleLanguageToggle(languageMap, otherLang, onColor, offColor)
                                }
                            }
                        }
                    }

                    val newLocale = LocaleListCompat.forLanguageTags(lang)
                    AppCompatDelegate.setApplicationLocales(newLocale)

                    Toast.makeText(requireContext(), "언어가 변경되었습니다", Toast.LENGTH_SHORT).show()

                    requireActivity().recreate() // 🔄 즉시 반영
                }
            }
        }
    }
    
    // 언어 토글 처리를 위한 헬퍼 메서드
    private fun handleLanguageToggle(languageMap: Map<String, com.google.android.material.switchmaterial.SwitchMaterial>, selectedLang: String, onColor: android.content.res.ColorStateList, offColor: android.content.res.ColorStateList) {
        languageMap.forEach { (lang, switch) ->
            switch.setOnCheckedChangeListener(null)
            val isSelected = (lang == selectedLang)
            switch.isChecked = isSelected
            switch.trackTintList = if (isSelected) onColor else offColor
        }
        
        // 언어 변경 적용
        val newLocale = LocaleListCompat.forLanguageTags(selectedLang)
        AppCompatDelegate.setApplicationLocales(newLocale)
        Toast.makeText(requireContext(), "언어가 변경되었습니다", Toast.LENGTH_SHORT).show()
        requireActivity().recreate()
    }

    private fun setKeepScreenOnFlag(enabled: Boolean) {
        if (enabled) {
            requireActivity().window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Log.d(TAG, "화면 꺼짐 방지 ON")
        } else {
            requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Log.d(TAG, "화면 꺼짐 방지 OFF")
        }
    }

    /**
     * IP 리스팅 데이터만 새로 갱신하는 함수
     * 프로그레스바 없이 배경에서 IP 데이터만 갱신함
     */
    private fun refreshIpListing() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 실제 API를 통해 최신 데이터 가져오기
                val repository = IpListingRepository()
                val updatedData = repository.getIpListing()
                
                if (updatedData.isNotEmpty()) {
                    // 전역 데이터 홀더에 업데이트된 데이터 저장
                    // ipListingItems가 불변 List 형식이므로 새 리스트로 할당
                    TradingDataHolder.ipListingItems = updatedData.toList()
                    
                    // 현재 IpHomeFragment가 있다면 그 중 데이터 갱신
                    val fragmentManager = requireActivity().supportFragmentManager
                    val ipHomeFragment = fragmentManager.fragments.filterIsInstance<IpHomeFragment>().firstOrNull()
                    
                    ipHomeFragment?.let {
                        // IpHomeFragment가 있는 경우 조용히 데이터만 갱신
                        it.refreshIpListingData()
                    }
                }
            } catch (e: Exception) {
                // 오류 발생 시 조용히 무시
                android.util.Log.e("MoreModeSettingFragment", "IP 리스팅 갱신 실패: ${e.message}")
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView: 바인딩 해제")
    }
}
