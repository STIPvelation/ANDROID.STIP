package com.stip.stip.more.activity

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import com.stip.stip.R
import com.stip.stip.databinding.ActivityMoreMemberInfoEditBinding
import com.stip.stip.model.MemberInfo
import com.stip.stip.more.fragment.RequiredInfoConsentDialogFragment
import com.stip.stip.more.fragment.EmailChangeFragment
import com.stip.stip.more.viewmodel.MemberInfoEditViewModel
import com.stip.stip.signup.utils.Utils.Companion.getJobCodeByName
import com.google.android.material.button.MaterialButton
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import android.provider.MediaStore
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.text.SimpleDateFormat
import java.util.*

class MemberInfoEditActivity : AppCompatActivity(),
    RequiredInfoConsentDialogFragment.ConsentListener {

    private lateinit var binding: ActivityMoreMemberInfoEditBinding
    private val viewModel: MemberInfoEditViewModel by viewModels()

    // 원본 입력 값을 저장할 변수들
    private var rawEnglishName: String = ""
    private var rawPostalCode: String = ""
    private var rawAddress: String = ""
    private var rawAddressDetail: String = ""
    private var rawJob: String = ""
    
    // 프로필 이미지 관련 변수
    private var currentPhotoPath: String = ""
    private val PERMISSION_REQUEST_CAMERA = 100
    private val PERMISSION_REQUEST_STORAGE = 101
    
    // Activity Result API 로 이전의 startActivityForResult 대체
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 카메라로 촬영한 이미지 처리
            // currentPhotoPath에 이미 경로가 저장되어 있으므로 그대로 사용
            // 필요한 이미지 처리 코드 추가
        }
    }
    
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri = result.data?.data
            // 선택한 이미지 처리 코드 추가
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoreMemberInfoEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scrollView.visibility = View.VISIBLE
        binding.fragmentContainer.visibility = View.GONE

        setupCustomHeaderAndListeners()
        setupObservers()
        setupBackStackListener()
        setupProfilePhotoButton()

        // API에서 회원정보 불러오기
        viewModel.loadMemberInfo()
    }

    private fun setupCustomHeaderAndListeners() {
        binding.headerTitle.text = getString(R.string.member_info_edit_title)
        binding.buttonBack.setOnClickListener {
            Log.d("MemberInfoEditActivity", "Back button clicked")
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupObservers() {
        // 로딩 상태 관찰
        viewModel.isLoading.observe(this, Observer { isLoading ->
            // 사용자 요청에 따라 progressBar 사용하지 않음
            // 로딩 상태를 다른 방식으로 표시할 수 있음(예: 버튼 비활성화)
        })

        // 회원 정보 관찰
        viewModel.memberInfo.observe(this, Observer { memberInfo ->
            memberInfo?.let {
                setupMemberInfoUI(it)
                setupSectionClickListeners()
            } ?: run {
                // 회원 정보가 없는 경우
                Toast.makeText(this, "회원 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        })

        // 오류 메시지 관찰
        viewModel.errorMessage.observe(this, Observer { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        })

        // 성공 메시지 관찰
        viewModel.successMessage.observe(this, Observer { successMessage ->
            Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
        })
    }

    private fun setupProfilePhotoButton() {
        // 프로필 사진 변경 버튼 클릭 리스너
        binding.btnChangePhoto.setOnClickListener {
            showImageSelectionDialog()
        }
    }
    
    /**
     * 활동 상태 표시기를 업데이트합니다
     * @param isActive 활동 중인지 여부
     */
    private fun updateActivityStatusIndicator(isActive: Boolean) {
        val statusDrawable = if (isActive) {
            R.drawable.bg_activity_status_active
        } else {
            R.drawable.bg_activity_status_inactive
        }
        binding.activityStatusIndicator.setBackgroundResource(statusDrawable)
    }

    private fun setupMemberInfoUI(memberInfo: MemberInfo) {
        // UI에 회원 정보 표시
        binding.tvEmailValue.text = memberInfo.email
        
        // 활동 상태 설정 - 기본값으로 활성 상태 설정
        val isActive = true // MemberInfo 클래스에 status 필드가 없으므로 기본값으로 설정
        updateActivityStatusIndicator(isActive)

        // 전화번호 포맷팅
        val formattedPhoneNumber =
            com.stip.stip.signup.utils.Utils.formatPhoneNumber(memberInfo.phoneNumber)
        binding.tvPhoneValue.text = formattedPhoneNumber ?: "-"

        // 영문 이름 표시 - EditText로 직접 표시
        val englishName = "${memberInfo.englishFirstName} ${memberInfo.englishLastName}"
        rawEnglishName = englishName

        // TextView 대신 EditText에 바로 표시
        binding.tvEnglishNameValue.visibility = View.GONE
        binding.etEnglishNameValue.apply {
            visibility = View.VISIBLE
            setText(englishName.trim().ifEmpty { "" })

            // EditText 변경 이벤트 감지를 위한 TextWatcher 설정
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    rawEnglishName = s.toString().trim()
                    Log.d("MemberInfoEditActivity", "영문 이름 변경됨: $rawEnglishName")
                }
            })
        }

        // 직업 정보 표시
        val jobName = com.stip.stip.signup.utils.Utils.getJobNameByCode(
            binding.root.context,
            memberInfo.job
        )
        rawJob = memberInfo.job
        binding.tvJobValue?.text = jobName ?: "-"

        // 주소 포맷팅 (우편번호, 기본주소, 상세주소 포함)
        val formattedAddress = com.stip.stip.signup.utils.Utils.formatAddress(
            memberInfo.postalCode,
            "${memberInfo.address} ${memberInfo.addressDetail}"
        )
        rawAddress = memberInfo.address
        rawAddressDetail = memberInfo.addressDetail
        rawPostalCode = memberInfo.postalCode
        binding.tvAddressValue?.text = formattedAddress ?: "-"

        // 은행 정보 포맷팅
        val bankName = com.stip.stip.signup.utils.Utils.getBankNameByCode(
            binding.root.context,
            memberInfo.bankCode
        )
        val formattedAccountNumber = com.stip.stip.signup.utils.Utils.formatAccountNumber(
            memberInfo.bankCode,
            memberInfo.accountNumber
        )
        binding.root.findViewById<android.widget.TextView>(R.id.value_bank)?.text =
            if (bankName.isNotEmpty() && formattedAccountNumber.isNotEmpty()) "$bankName $formattedAccountNumber" else "-"

    }

    private fun setupSectionClickListeners() {

        binding.sectionPhone.setOnClickListener {
            Log.d("MemberInfoEditActivity", "Phone section clicked!")
            showPhoneChangeConfirmationDialog()
        }

        binding.sectionEmail.setOnClickListener {
            Log.d("MemberInfoEditActivity", "Email section clicked!")
            Toast.makeText(this, "준비중", Toast.LENGTH_SHORT).show()
//            navigateToEmailChangeFragment()
        }

        // 필수 정보 변경
        binding.btnChange.setOnClickListener {
            Log.d("MemberInfoEditActivity", "Change Required Info button clicked!")
            Toast.makeText(this, "준비중", Toast.LENGTH_SHORT).show()
        }

        binding.tvJobValue.setOnClickListener {
            val jobList = resources.getStringArray(R.array.kyc_job_select)
            com.stip.stip.signup.customview.KYCInformBottomSheet(
                getString(R.string.sign_up_kyc_job_select),
                jobList.toList(),
                { selectedJob ->
                    // 선택된 직업명을 TextView에 표시
                    rawJob = getJobCodeByName(binding.root.context, selectedJob)
                    binding.tvJobValue.text = selectedJob
                }
            ).show(supportFragmentManager, "jobSelect")
        }
        binding.tvAddressValue.setOnClickListener {
            // 주소 검색 화면으로 이동
            startAddressSearch()
        }
    }

    private fun navigateToEmailChangeFragment() {
        Log.d("MemberInfoEditActivity", "Navigating to EmailChangeFragment")
        // 현재 이메일 값 가져오기
        val currentUserEmail = viewModel.memberInfo.value?.email ?: ""
        val emailChangeFragment = EmailChangeFragment.newInstance(currentUserEmail)

        // 이메일 변경 후 콜백 설정 - API 연동
        emailChangeFragment.setOnEmailChangeListener { newEmail ->
            viewModel.updateEmail(newEmail)
        }

        binding.scrollView.visibility = View.GONE
        binding.fragmentContainer.visibility = View.VISIBLE

        supportFragmentManager.commit {
            replace(R.id.fragment_container, emailChangeFragment)
            addToBackStack("EmailChangeFragment")
            setReorderingAllowed(true)
        }
    }

    private fun setupBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                // 뒤로가기 후 메인 화면으로 복귀
                binding.scrollView.visibility = View.VISIBLE
                binding.fragmentContainer.visibility = View.GONE

                // 최신 회원정보 로드
                viewModel.loadMemberInfo()
            }
        }
    }

    // 전화번호 변경 다이얼로그
    // 핸드폰 인증 결과를 처리하기 위한 ActivityResultLauncher
    private val phoneAuthLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("MemberInfoEditActivity", "휴대폰 인증 결과 받음: resultCode=${result.resultCode}")

            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                // PhoneAuthActivity에서 받은 전화번호 데이터 처리
                val phoneNumber = data?.getStringExtra(PhoneAuthActivity.EXTRA_PHONE_NUMBER)
                val sourceActivity = data?.getStringExtra(PhoneAuthActivity.EXTRA_SOURCE_ACTIVITY)
                if (!phoneNumber.isNullOrEmpty()) {
                    // 인증 성공 처리
                    viewModel.updatePhone(phoneNumber)
                    Toast.makeText(this, "전화번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()

                    // 회원 정보 다시 로드
                    viewModel.loadMemberInfo()
                } else {
                    // 인증 실패 처리
                    Toast.makeText(this, "전화번호 인증에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                // 사용자가 인증 취소
                Log.d("MemberInfoEditActivity", "휴대폰 인증 취소됨")
                // 토스트 메시지 제거
            }
        }

    // 주소 검색 결과를 처리하기 위한 ActivityResultLauncher
    private val addressLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                // 주소 검색에서 받은 데이터 처리
                val address = data?.getStringExtra(AddressSearchActivity.EXTRA_ADDRESS)
                val addressDetail = data?.getStringExtra(AddressSearchActivity.EXTRA_ADDRESS_DETAIL)
                val postalCode = data?.getStringExtra(AddressSearchActivity.EXTRA_POSTAL_CODE)

                rawAddress = address.toString()
                rawAddressDetail = addressDetail.toString()
                rawPostalCode = postalCode.toString()

                if (!address.isNullOrEmpty()) {
                    // 포맷팅된 주소로 UI 업데이트
                    val formattedAddress = com.stip.stip.signup.utils.Utils.formatAddress(
                        postalCode,
                        "$address ${addressDetail ?: ""}"
                    )
                    binding.tvAddressValue.text = formattedAddress ?: "-"

                    // 토스트 메시지 표시
                    Toast.makeText(this, "주소가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // 전화번호 변경 다이얼로그
    private fun showPhoneChangeConfirmationDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_phone_authentication, null)
        dialogBuilder.setView(dialogView)

        val alertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 인증 버튼 - NICE 인증으로 연동
        val btnAuthenticate = dialogView.findViewById<MaterialButton>(R.id.button_authenticate)
        btnAuthenticate.setOnClickListener {
            // NICE 휴대폰 본인인증으로 이동
            alertDialog.dismiss()
            startNiceAuthentication()
        }

        // 취소 버튼
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.button_cancel)
        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    // NICE 휴대폰 본인인증 시작
    private fun startNiceAuthentication() {
        try {
            // 휴대폰 인증 전용 액티비티로 이동 - 회원가입 프로세스 없이 인증만 진행
            val intent = Intent(this, PhoneAuthActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            // 현재 액티비티 정보 전달
            intent.putExtra(PhoneAuthActivity.EXTRA_SOURCE_ACTIVITY, javaClass.simpleName)

            Log.d("MemberInfoEditActivity", "휴대폰 인증 화면으로 이동 시작")

            // 결과를 받기 위한 ActivityResultLauncher 사용
            phoneAuthLauncher.launch(intent)

        } catch (e: Exception) {
            Log.e("MemberInfoEditActivity", "NICE 인증 시작 오류: ${e.message}")
            Toast.makeText(this, "인증 시작 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- 필수 정보 변경 동의 다이얼로그 표시 함수 ---
    private fun showRequiredInfoConsentDialog() {
        // RequiredInfoConsentDialogFragment의 정확한 경로 사용
        val dialog = RequiredInfoConsentDialogFragment.newInstance()
        dialog.setConsentListener(this) // Activity가 리스너 역할을 하도록 설정
        dialog.show(supportFragmentManager, RequiredInfoConsentDialogFragment.TAG)
    }
    // --- 함수 끝 ---

    // ConsentListener 인터페이스 구현 - 필수 정보 변경 시작
    override fun onConsentStartClicked() {
        Log.d("MemberInfoEditActivity", "Consent given, updating required info with API")

        // 캡처한 원본 값으로 API 요청
        viewModel.updateRequiredInfo(
            englishName = rawEnglishName,
            postalCode = rawPostalCode,
            address = rawAddress,
            addressDetail = rawAddressDetail,
            job = rawJob
        )

        Toast.makeText(this, "필수 정보 변경을 위한 요청이 전송되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun setupEnglishNameEdit() {
        // EditText 설정만 진행
        binding.etEnglishNameValue.apply {
            isEnabled = true
            isFocusableInTouchMode = true
            isFocusable = true

            // 바깥쪽 영역을 클릭해도 EditText에 포커스가 유지되도록 설정
            binding.containerEnglishName.setOnClickListener {
                requestFocus()
            }

            // EditText의 너비 확장
            val params = layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.width = 0 // match_constraint
            params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams = params
        }
    }

    // 주소 검색 시작 함수
    private fun startAddressSearch() {
        try {
            // 주소 검색 액티비티로 이동
            val intent = Intent(this, AddressSearchActivity::class.java)

            Log.d("MemberInfoEditActivity", "주소 검색 화면으로 이동 시작")

            // 결과를 받기 위한 ActivityResultLauncher 사용
            addressLauncher.launch(intent)

        } catch (e: Exception) {
            Log.e("MemberInfoEditActivity", "주소 검색 시작 오류: ${e.message}")
            Toast.makeText(this, "주소 검색 시작 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 카메라 권한 확인
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // 저장소 권한 확인
    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // 카메라 권한 요청
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CAMERA
        )
    }
    
    // 저장소 권한 요청
    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_STORAGE
        )
    }
    
    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent()
                } else {
                    Toast.makeText(this, "카메라 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_REQUEST_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "저장소 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // 카메라 앱 실행
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // 카메라 앱이 있는지 확인
            takePictureIntent.resolveActivity(packageManager)?.also {
                // 임시 파일 생성
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // 오류 발생 시
                    Toast.makeText(this, "이미지 파일을 생성할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    null
                }
                
                // 생성된 파일이 있으면 계속 진행
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.stip.stip.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureLauncher.launch(takePictureIntent)
                }
            } ?: run {
                // 카메라 앱이 없는 경우
                Toast.makeText(this, "카메라 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // 이미지 파일 생성
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // 파일명 생성
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = getExternalFilesDir(null) ?: throw IOException("External storage not available")
        
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }
    
    // 갤러리 앱 실행
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }
    
    // 이미지 선택 다이얼로그 표시
    private fun showImageSelectionDialog() {
        val options = arrayOf("카메라로 촬영", "갤러리에서 선택")
        
        AlertDialog.Builder(this)
            .setTitle("프로필 사진 변경")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // 카메라 권한 확인 후 실행
                        if (checkCameraPermission()) {
                            dispatchTakePictureIntent()
                        } else {
                            requestCameraPermission()
                        }
                    }
                    1 -> {
                        // 저장소 권한 확인 후 실행
                        if (checkStoragePermission()) {
                            openGallery()
                        } else {
                            requestStoragePermission()
                            Toast.makeText(this, "이미지를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }
}