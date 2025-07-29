package com.stip.stip.more.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.stip.stip.R
import com.stip.stip.databinding.FragmentMoreMemberInfoBinding
import com.stip.stip.MainViewModel
import com.stip.stip.more.activity.MemberInfoEditActivity
import com.stip.stip.more.activity.PinVerificationActivity
import com.stip.stip.signup.login.LoginActivity
import com.stip.stip.signup.utils.PreferenceUtil
import com.stip.stip.ui.dialog.LogoutDialogFragment
import com.stip.stip.ui.dialog.AccountDeletionGuideFragment
import com.stip.stip.signup.Constants

class MoreMemberInfoFragment : Fragment() {

    private var _binding: FragmentMoreMemberInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    
    // PIN 확인 액티비티 결과를 처리하기 위한 ActivityResultLauncher
    private lateinit var pinVerificationLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreMemberInfoBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // PIN 확인 액티비티 결과 처리 등록
        pinVerificationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // PIN 확인 성공 - 수정 확인 다이얼로그 표시
                showEditConfirmDialog()
            }
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ 헤더 설정
        viewModel.updateHeaderTitle("회원 정보")
        viewModel.enableBackNavigation {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        // 랜덤 사용자 ID 설정
        setRandomUserId()
        
        // 프로필 사진 변경 버튼 리스너
        setupProfilePhotoChangeListener()

        // ViewModel 관찰 설정
        observeViewModel()

        // API에서 회원정보 로드 (ViewModel에서 업데이트 시작)
        // loadMemberInfo()는 onStart() 또는 onResume()에서 호출할 수도 있음
        // 회원정보 관찰 및 UI 반영
        viewModel.memberInfo.observe(viewLifecycleOwner) { info ->
            if (info != null) {
                // 프로필 섹션
                binding.textViewProfileName.text = info.name
                // 이미 생성된 랜덤 ID가 없는 경우에만 생성
                if (binding.textViewUserId.text.isNullOrEmpty()) {
                    setRandomUserId()
                }
                // 기본 회원정보
                binding.root.findViewById<TextView>(R.id.value_name).text = info.name
                binding.root.findViewById<TextView>(R.id.value_email).text = info.email
                
                // 핸드폰 번호 포맷팅
                val formattedPhoneNumber = com.stip.stip.signup.utils.Utils.formatPhoneNumber(info.phoneNumber)
                binding.root.findViewById<TextView>(R.id.value_phone).text = formattedPhoneNumber
                
                // 여권 영문 이름 설정
                val englishName = (info.englishFirstName + " " + info.englishLastName).takeIf { !it.isNullOrBlank() } ?: info.name.uppercase()
                binding.root.findViewById<TextView>(R.id.value_english_name).text = englishName
                // 추가 필드 업데이트
                // 생년월일 포맷팅
                val formattedBirthdate = com.stip.stip.signup.utils.Utils.formatBirthdate(info.birthdate)
                binding.root.findViewById<TextView>(R.id.value_dob).text = formattedBirthdate
                // 은행 코드를 은행 이름으로 변환하여 표시
                val bankName = com.stip.stip.signup.utils.Utils.getBankNameByCode(requireContext(), info.bankCode)
                
                // 계좌번호 포맷팅
                val formattedAccountNumber = com.stip.stip.signup.utils.Utils.formatAccountNumber(info.bankCode, info.accountNumber)
                binding.root.findViewById<TextView>(R.id.value_bank).text = "$bankName $formattedAccountNumber"
                // 주소 포맷팅 (우편번호 포함)
                val formattedAddress = com.stip.stip.signup.utils.Utils.formatAddress(info.postalCode, info.address)
                binding.root.findViewById<TextView>(R.id.value_address).text = formattedAddress
                
                // 직업 코드를 직업명으로 변환하여 표시
                val jobName = com.stip.stip.signup.utils.Utils.getJobNameByCode(requireContext(), info.job)
                binding.root.findViewById<TextView>(R.id.value_job).text = jobName

                // 회원정보가 있는 경우 관련 버튼 활성화
                binding.buttonEditInfo.isEnabled = true
                binding.buttonLogout.isEnabled = true
                binding.textViewWithdraw.isEnabled = true
            } else {
                // 회원정보가 없는 경우 (로그인 필요)
                binding.textViewProfileName.text = "로그인이 필요합니다"

                val allValueIds = listOf(
                    R.id.value_name, R.id.value_english_name, R.id.value_email,
                    R.id.value_phone, R.id.value_dob, R.id.value_bank,
                    R.id.value_address, R.id.value_job
                )

                allValueIds.forEach { id ->
                    binding.root.findViewById<TextView>(id)?.apply {
                        text = "-"
                    }
                }

                // 관련 버튼 비활성화
                binding.buttonEditInfo.isEnabled = false
                binding.buttonLogout.isEnabled = false
                binding.textViewWithdraw.isEnabled = false

                // 로그인 화면으로 이동 처리 추가
                binding.profileSummarySection.setOnClickListener {
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                }
            }
        }

        // API를 통해 회원정보 로드
        refreshMemberInfo()

        setupNavigationListener()
        setupLogoutListener()
        setupWithdrawDialogListener()
    }

    private fun setupNavigationListener() {
        // buttonEditInfo(수정 버튼)에 클릭 리스너 설정
        binding.buttonEditInfo.setOnClickListener {
            // 회원정보가 있는지 확인 (isAuthenticated 대신 회원정보 존재 여부로 체크)
            if (viewModel.memberInfo.value != null) {
                // PIN 확인 화면으로 이동 (ActivityResultLauncher 사용)
                val intent = Intent(requireContext(), PinVerificationActivity::class.java)
                pinVerificationLauncher.launch(intent)
            } else {
                // 로그인 필요 메시지
                Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(
                        requireContext(),
                        LoginActivity::class.java
                    )
                )
            }
        }
    }
    
    // PIN 확인 성공 후 수정 여부를 묻는 다이얼로그 표시
    private fun showEditConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("회원 정보 수정")
            .setMessage("회원 정보를 수정하시겠습니까?")
            .setPositiveButton("수정") { _, _ ->
                // 회원 정보 수정 화면으로 이동
                val intent = Intent(requireContext(), MemberInfoEditActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("취소", null)
            .show()
    }


    private fun setupLogoutListener() {
        binding.buttonLogout.setOnClickListener {
            LogoutDialogFragment {
                performLogout()
            }.show(parentFragmentManager, "LogoutDialog")
        }
    }

    private fun setupWithdrawDialogListener() {
        binding.textViewWithdraw.setOnClickListener {
            AccountDeletionGuideFragment {
                performAccountDeletion()
            }.show(parentFragmentManager, "AccountDeletionGuideDialog")
        }
    }
    
    private fun setupProfilePhotoChangeListener() {
        // 프로필 사진 변경 버튼 클릭 리스너
        binding.buttonChangeProfilePhoto.setOnClickListener {
            // 카메라 및 갤러리에서 이미지 선택 다이얼로그 표시
            showImageSelectionDialog()
        }
    }
    
    private fun showImageSelectionDialog() {
        val options = arrayOf("카메라로 사진 촬영", "갤러리에서 선택")
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("프로필 사진 변경")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // 카메라 실행 로직
                        // 실제 구현시 카메라 권한 확인 및 Intent 처리 필요
                        Toast.makeText(context, "준비중", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        // 갤러리 실행 로직
                        // 실제 구현시 저장소 권한 확인 및 Intent 처리 필요
                        Toast.makeText(context, "준비중", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun setRandomUserId() {
        // 9자리 랜덤 숫자 생성
        val random = java.util.Random()
        val randomId = StringBuilder()
        for (i in 0 until 9) {
            randomId.append(random.nextInt(10))
        }
        binding.textViewUserId.text = "ID: " + randomId.toString()
    }

    private fun performLogout() {
        PreferenceUtil.clear()

        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    /**
     * 회원탈퇴 완료 처리
     */
    private fun completeWithdrawal() {
        try {
            // 사용자 데이터 모두 삭제
            PreferenceUtil.clear()

            // Toast 메시지 표시
            android.widget.Toast.makeText(
                requireContext(),
                "회원탈퇴가 완료되었습니다.",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            // 로그인 화면으로 이동
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
            
        } catch (e: Exception) {
            Log.e("MoreMemberInfoFragment", "회원탈퇴 완료 처리 중 오류 발생", e)
            android.widget.Toast.makeText(
                requireContext(),
                "오류가 발생했습니다. 다시 시도해주세요.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 회원탈퇴 제한 상수
    companion object {
        // 기기 식별자를 이용해 동일 기기에서 탈퇴-재가입 횟수 추적
        private const val PREF_KEY_DEVICE_ID = "device_unique_id"
        private const val PREF_KEY_WITHDRAWAL_COUNT = "withdrawal_count_yearly"
        private const val PREF_KEY_LAST_WITHDRAWAL_TIME = "last_withdrawal_time"
        private const val PREF_KEY_YEARLY_COUNT_RESET_TIME = "yearly_count_reset_time"
        private const val PREF_KEY_PHONE_NUMBERS = "withdrawn_phone_numbers"
        private const val MAX_WITHDRAWALS_PER_YEAR = 3
        private const val REACTIVATION_DELAY_HOURS = 24
    }

    private fun performAccountDeletion() {
        // 탈퇴 제한 사항 확인
        val currentTime = System.currentTimeMillis()
        val sharedPrefs = requireActivity().getSharedPreferences(
            "MembershipRestrictionPrefs",
            Context.MODE_PRIVATE
        )

        // 고유 기기 ID 가져오기
        var deviceId = sharedPrefs.getString(PREF_KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = Settings.Secure.getString(
                requireActivity().contentResolver,
                Settings.Secure.ANDROID_ID
            )
            sharedPrefs.edit().putString(PREF_KEY_DEVICE_ID, deviceId).apply()
        }

        // 1. 연간 탈퇴 횟수 제한 검사
        var yearlyWithdrawalCount = sharedPrefs.getInt(PREF_KEY_WITHDRAWAL_COUNT, 0)
        val yearlyResetTime = sharedPrefs.getLong(PREF_KEY_YEARLY_COUNT_RESET_TIME, 0)

        // 연간 카운터 리셋해야 하는지 확인
        if (yearlyResetTime > 0 && currentTime - yearlyResetTime > 365 * 24 * 60 * 60 * 1000L) {
            yearlyWithdrawalCount = 0
            sharedPrefs.edit().putLong(PREF_KEY_YEARLY_COUNT_RESET_TIME, currentTime).apply()
        } else if (yearlyResetTime == 0L) {
            // 최초 설정
            sharedPrefs.edit().putLong(PREF_KEY_YEARLY_COUNT_RESET_TIME, currentTime).apply()
        }

        // 탈퇴는 횟수 제한 없이 가능
        // yearlyWithdrawalCount는 로그인 화면에서 재가입 제한을 위해서만 확인함

        // 현재 회원 폰번호 가져오기
        val currentMemberInfo = PreferenceUtil.getMemberInfo()
        val phoneNumber = currentMemberInfo?.phoneNumber

        // TODO: 서버에 회원탈퇴 API 호출 (API 완성 시 아래 함수만 구현하면 됨)
        val di = PreferenceUtil.getString(Constants.PREF_KEY_DI_VALUE)
        val pin = getPinFromUser() // 추후 pin 입력 UI 구현 필요
        withdrawMemberFromServer(di, pin)

        // 아래 코드는 실제 API 성공 시에만 실행하도록 추후 위치 이동 필요
        // PreferenceUtil.clear()

        // 탈퇴한 폰번호 목록에 추가 (폰번호 변경 시 후 재가입 방지용)
        if (phoneNumber != null) {
            val phoneSet = sharedPrefs.getStringSet(PREF_KEY_PHONE_NUMBERS, mutableSetOf<String>())
                ?: mutableSetOf()
            val updatedPhoneSet = phoneSet.toMutableSet()
            updatedPhoneSet.add(phoneNumber)
            sharedPrefs.edit().putStringSet(PREF_KEY_PHONE_NUMBERS, updatedPhoneSet).apply()
        }

        // 탈퇴 카운트 증가 및 탈퇴 시간 저장
        sharedPrefs.edit().apply {
            putInt(PREF_KEY_WITHDRAWAL_COUNT, yearlyWithdrawalCount + 1)
            putLong(PREF_KEY_LAST_WITHDRAWAL_TIME, currentTime)
        }.apply()

        // 로컬 저장소 내 모든 회원 정보 삭제 (PreferenceUtil 외에도 개인정보 저장소 모두 삭제)
        // 1. SharedPreferences
        PreferenceUtil.clear()

        // 2. 앱 내부 데이터베이스 삭제 (필요한 경우)
        // val dbHelper = AppDatabase.getInstance(requireContext())
        // dbHelper.clearAllTables()

        // 3. 캐시 삭제
        try {
            requireContext().cacheDir.deleteRecursively()
        } catch (e: Exception) {
            Log.e("MoreMemberInfoFragment", "Cache deletion error: ${e.message}")
        }

        // 로그인 화면으로 이동
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_withdrawal_message", true)
            putExtra("withdrawal_time", currentTime)  // 탈퇴 시간 전달
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun observeViewModel() {
        // 회원탈퇴 결과 관찰
        viewModel.memberDeleteResult.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                Log.d("MoreMemberInfoFragment", "회원탈퇴 성공")
                completeWithdrawal()
            }
        }

        // 회원탈퇴 오류 관찰
        viewModel.memberDeleteError.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Log.e("MoreMemberInfoFragment", "회원탈퇴 실패: $errorMessage")
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearMemberDeleteResult()
            }
        }
    }

    private fun withdrawMemberFromServer(di: String?, pin: String?) {
        if (di.isNullOrEmpty() || pin.isNullOrEmpty()) {
            Log.e("MoreMemberInfoFragment", "DI 또는 PIN이 비어있음")
            Toast.makeText(requireContext(), "회원 인증 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("MoreMemberInfoFragment", "회원탈퇴 API 호출 시작")
        viewModel.deleteMember(di, pin)
    }

    private fun getPinFromUser(): String? {
        // TODO: PIN 입력 UI
        // 현재는 저장된 PIN을 반환 (임시)
        return PreferenceUtil.getString(Constants.PREF_KEY_PIN_VALUE)
    }

    //새로고침
    private fun refreshMemberInfo() {
        // 로그인 상태 확인
        if (viewModel.isAuthenticated) {
            Log.d("MoreMemberInfoFragment", "회원정보 API 조회")
            viewModel.loadMemberInfo()
        }
    }

    override fun onResume() {
        super.onResume()

        // 화면으로 돌아올 때마다 회원정보 새로고침
        refreshMemberInfo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
