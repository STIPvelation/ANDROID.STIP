package com.stip.stip

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.stip.stip.iptransaction.fragment.IpProfitLossFragment
import com.stip.stip.databinding.ActivityMainBinding
import com.stip.ipasset.fragment.IpAssetFragment
import com.stip.stip.iptransaction.fragment.IpHoldingFragment
import com.stip.stip.iphome.fragment.IpHomeFragment
import com.stip.stip.iptransaction.fragment.IpInvestmentFragment
import com.stip.stip.ipinfo.fragment.IpNewsFragment
import com.stip.stip.ipinfo.fragment.IpTrendFragment
import com.stip.stip.iptransaction.fragment.IpUnfilledFragment
import com.stip.stip.more.fragment.MoreFragment
import com.google.android.material.tabs.TabLayout
import com.skydoves.sandwich.onSuccess

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.stip.stip.signup.login.LoginViewModel

// 만약 HeaderUpdater 인터페이스를 정의했다면 유지, 아니라면 이 줄은 필요 없음
// class MainActivity : AppCompatActivity(), MoreFragment.MoreFragmentListener {
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentMainTabIndex = -1
    private var lastSelectedTabIndex: Int = -1

    // MainViewModel 인스턴스 가져오기
    private val viewModel by viewModels<MainViewModel>()
    private val loginViewModel: LoginViewModel by viewModels()

    // Fragment 리스트 정의 (기존 코드 유지)
    private val ipInfoSubFragments: List<Fragment> by lazy {
        listOf(IpTrendFragment.newInstance(), IpNewsFragment.newInstance())
    }
    private val ipTransactionSubFragments: List<Fragment> by lazy {
        listOf(
            IpHoldingFragment.newInstance(),
            IpProfitLossFragment.newInstance(),
            IpInvestmentFragment.newInstance(),
            IpUnfilledFragment.newInstance()
        )
    }
    // 인증 상태 확인 - 실제 로그인 상태 확인
    private fun isAuthenticated(): Boolean = com.stip.stip.signup.utils.PreferenceUtil.getToken() != null
    
    // 헤더 타이틀 설정 메소드
    fun setHeaderTitle(title: String) {
        binding.headerTitle.text = title
    }
    
    // 헤더 숨기기/보이기 메소드
    fun setHeaderVisibility(visible: Boolean) {
        binding.headerLayout.visibility = if (visible) View.VISIBLE else View.GONE
        binding.headerTitle.visibility = if (visible) View.VISIBLE else View.GONE
    }
    
    // 로그인이 필요한 기능 접근 시 로그인 화면으로 리다이렉트
    private fun redirectToLogin() {
        Toast.makeText(this, "로그인이 필요한 서비스입니다.", Toast.LENGTH_SHORT).show()
        // PIN 번호 입력 화면으로 바로 이동
        val intent = Intent(this, com.stip.stip.signup.login.LoginPinNumberActivity::class.java)
        startActivity(intent)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        // 항상 로그인 후 네비게이션 그래프 사용
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? androidx.navigation.fragment.NavHostFragment
        navHostFragment?.let {
            val navController = it.navController
            // 항상 nav_graph_after_login 사용
            val navGraphId = R.navigation.nav_graph_after_login
            val navGraph = navController.navInflater.inflate(navGraphId)
            navController.graph = navGraph
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뉴스 데이터 앱 시작 시 미리 로딩
        com.stip.stip.ipinfo.NewsRepository.preloadNews()

        Log.d("MAIN", "✅ MainActivity 시작됨")

        // ✅ 1. 헤더 제목 옵저버
        viewModel.headerTitle.observe(this) { title ->
            binding.headerTitle.text = title
            Log.d("MainActivity_Debug", "Header title updated to: $title")
        }

        // ✅ 2. 헤더 아이콘 리소스 옵저버
        viewModel.navigationIconRes.observe(this) { resId ->
            if (resId != 0) {
                binding.buttonBack.visibility = View.VISIBLE
                binding.buttonBack.setImageResource(resId)
            } else {
                binding.buttonBack.visibility = View.GONE
            }
        }

        // ✅ 3. 아이콘 클릭 리스너
        viewModel.navigationClickListener.observe(this) { listener ->
            binding.buttonBack.setOnClickListener {
                listener.invoke()
            }
        }

        // ✅ 4. 초기 상태: 기본 아이콘 숨기기
        viewModel.updateNavigationIcon(0) // <-- 헤더 초기화용 (선택)

        // ✅ 5. 탭 및 기본화면 세팅
        setupBottomTabClickListeners()
        setupSecondaryTabListener()

        // 앱 실행 시 뉴스 미리 로딩
        com.stip.stip.ipinfo.NewsRepository.preloadNews()
        
        // 환율 초기화 (비동기로 실행)
        lifecycleScope.launch {
            com.stip.utils.ExchangeRateManager.initialize()
        }
        
        if (savedInstanceState == null) {
            handleBottomTabClick(0, force = true)
        }
    }



    // 하단 탭 클릭 리스너 설정 (기존 코드 유지)
    private fun setupBottomTabClickListeners() {
        binding.tabIphome.setOnClickListener { handleBottomTabClick(0, force = true) }
        binding.tabIpinfo.setOnClickListener { handleBottomTabClick(1, force = true) }
        binding.tabIptransaction.setOnClickListener {
            if (isAuthenticated()) {
                handleBottomTabClick(2, force = true)
            } else {
                showLoginRequiredDialog()
            }
        }
        binding.tabIpasset.setOnClickListener {
            if (isAuthenticated()) {
                // 입출금 탭 클릭 시 전화 사기 경고 다이얼로그 표시 여부 확인
                if (shouldShowPhoneFraudAlert()) {
                    showPhoneFraudAlertDialog()
                } else {
                    handleBottomTabClick(3, force = true)
                }
            } else {
                showLoginRequiredDialog()
            }
        }
        binding.tabMore.setOnClickListener {
            if (isAuthenticated()) {
                handleBottomTabClick(4, force = true)
            } else {
                showLoginRequiredDialog()
            }
        }
    }

    // 상단 탭 리스너 설정 (기존 코드 유지)
    private fun setupSecondaryTabListener() {
        binding.secondaryTabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { position ->
                    when (currentMainTabIndex) {
                        1 -> if (position < ipInfoSubFragments.size) replaceFragment(
                            ipInfoSubFragments[position]
                        )

                        2 -> {
                            // IP손익 탭(position 1)은 비활성화 - 클릭해도 아무 동작 안함
                            if (position == 1) {
                                // IP손익 탭 클릭 시 아무 동작 안함
                                return@let
                            }
                            if (position < ipTransactionSubFragments.size) replaceFragment(
                                ipTransactionSubFragments[position]
                            )
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // 하단 탭 클릭 처리 함수
    private fun handleBottomTabClick(index: Int, force: Boolean = false) {
        // 중복 클릭 처리 (기존 코드 유지)
        if (!force && index == lastSelectedTabIndex) {
            val currentFragment =
                supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
            if (currentFragment is ScrollableToTop) {
                currentFragment.scrollToTop()
            }
            return
        }

        lastSelectedTabIndex = index
        currentMainTabIndex = index
        updateBottomTabIcons(index) // 아이콘 업데이트 (기존 코드 유지)
        binding.secondaryTabLayout.removeAllTabs() // 상단 탭 초기화 (기존 코드 유지)

        // --- 헤더 제목 직접 설정 대신 ViewModel 호출로 변경 --- vvv
        // binding.headerTitle.text = when (index) { ... } // 이 부분을 아래 코드로 대체
        val defaultTitle = when (index) {
            0 -> getString(R.string.header_iphome)
            1 -> getString(R.string.header_ipinfo)
            2 -> getString(R.string.header_iptransaction)
            3 -> getString(R.string.header_ipasset)
            4 -> getString(R.string.header_more) // '더보기' 탭 기본 제목
            else -> ""
        }
        // ViewModel을 통해 기본 타이틀 업데이트 요청 (실제 표시될 제목은 각 Fragment가 설정)
        viewModel.updateHeaderTitle(defaultTitle)
        // --- 헤더 제목 직접 설정 대신 ViewModel 호출로 변경 --- ^^^

        // 탭 밑줄 및 상단 탭 가시성 처리 (기존 코드 유지)
        binding.tabUnderline.visibility = if (index == 1 || index == 2) View.VISIBLE else View.GONE

        // Fragment 교체 로직 (기존 코드 유지)
        when (index) {
            0 -> {
                binding.secondaryTabLayout.visibility = View.GONE
                binding.tabUnderline.visibility = View.GONE
                replaceFragment(IpHomeFragment.newInstance())
            }

            1 -> {
                binding.secondaryTabLayout.visibility = View.VISIBLE
                binding.tabUnderline.visibility = View.VISIBLE
                populateSecondaryTabs(
                    listOf(
                        getString(R.string.tab_info_trend),
                        getString(R.string.tab_info_news)
                    )
                )
                replaceFragment(ipInfoSubFragments[0])
                binding.secondaryTabLayout.getTabAt(0)?.select()
                // 다이얼로그 표시 제거됨 - 준비중 기능
            }

            2 -> {
                binding.secondaryTabLayout.visibility = View.VISIBLE
                binding.tabUnderline.visibility = View.VISIBLE
                populateSecondaryTabs(
                    listOf(
                        getString(R.string.tab_transaction_holdings),
                        getString(R.string.tab_transaction_profit),
                        getString(R.string.tab_transaction_investment),
                        getString(R.string.tab_transaction_unfilled)
                    )
                )
                replaceFragment(ipTransactionSubFragments[0])
                binding.secondaryTabLayout.getTabAt(0)?.select()
                // 다이얼로그 표시 제거됨 - 준비중 기능
            }

            3 -> {
                binding.secondaryTabLayout.visibility = View.GONE
                binding.tabUnderline.visibility = View.GONE
                replaceFragment(IpAssetFragment.newInstance())
                
                // 출금 페이지인 경우 전화사기 경고 다이얼로그만 표시
                // IpAsset은 준비중 다이얼로그 표시하지 않음
            }

            4 -> {
                binding.secondaryTabLayout.visibility = View.GONE
                binding.tabUnderline.visibility = View.GONE
                replaceFragment(MoreFragment())
                // 더보기 페이지는 이미 구현되어 있으므로 별도의 안내 메시지 없음
                
                // 더보기 탭 진입 시 회원정보 강제 새로고침
                if (isAuthenticated()) {
                    viewModel.loadMemberInfo()
                }
            }
        }
    }

    // 상단 탭 내용 채우기 (기존 코드 유지)
    private fun populateSecondaryTabs(titles: List<String>) {
        titles.forEachIndexed { index, title ->
            val tab = binding.secondaryTabLayout.newTab().setText(title)
            
            // IP내역 탭에서 IP손익 탭(position 1)은 비활성화
            if (currentMainTabIndex == 2 && index == 1) {
                tab.view.isEnabled = false
                tab.view.alpha = 0.5f
            }
            
            binding.secondaryTabLayout.addTab(tab)
        }
    }

    /**
     * 로그인 이력 화면으로 네비게이션하는 함수
     */
    fun navigateToLoginHistory() {
        try {
            Log.d("MainActivity", "로그인 이력 화면으로 이동합니다")
            val fragment = com.stip.stip.more.fragment.MoreLoginHistoryFragment()
            supportFragmentManager.commit {
                replace(R.id.fragment_container, fragment)
                addToBackStack(null)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "로그인 이력 화면 이동 오류: ${e.message}")
            Toast.makeText(this, "로그인 이력 화면 로드 오류", Toast.LENGTH_SHORT).show()
        }
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        supportFragmentManager.commit {
            replace(binding.fragmentContainer.id, fragment)
            if (addToBackStack) {
                addToBackStack(null)
            }
        }

        // ✅ OrderContentViewFragment인 경우만 헤더 숨김
        if (fragment is com.stip.stip.iphome.fragment.OrderContentViewFragment) {
            hideHeaderAndTabs()
        } else {
            showHeader()
        }
    }

    override fun onStart() {
        super.onStart()
        // 로그인 상태 변경 확인 및 UI 업데이트
        updateUiBasedOnAuthState()
    }

    override fun onResume() {
        super.onResume()
        // setupOutsideTouchToCloseDropdown 함수 호출 제거 (정의되지 않은 함수)
        showHeader() // ✅ 헤더 다시 보이기
        
        // 로그인 상태 변경 확인 및 UI 업데이트
        updateUiBasedOnAuthState()
    }
    
    /**
     * 로그인 상태에 따라 UI 업데이트
     */
    private fun updateUiBasedOnAuthState() {
        val isCurrentlyAuthenticated = isAuthenticated()
        
        // 탭 아이콘 업데이트 (IP거래, IP자산, 더보기 탭에 로그인 필요 표시)
        binding.iconIptransaction.alpha = if (isCurrentlyAuthenticated) 1.0f else 0.5f
        binding.iconIpasset.alpha = if (isCurrentlyAuthenticated) 1.0f else 0.5f
        binding.iconMore.alpha = if (isCurrentlyAuthenticated) 1.0f else 0.5f
        
        // 현재 탭이 로그인이 필요한 탭이고 로그아웃 상태라면 홈으로 이동
        if (!isCurrentlyAuthenticated && (currentMainTabIndex == 2 || currentMainTabIndex == 3 || currentMainTabIndex == 4)) {
            handleBottomTabClick(0, force = true)
            Toast.makeText(this, "로그인이 필요한 서비스입니다.", Toast.LENGTH_SHORT).show()
        }
        
        // 로그인 상태가 변경되었을 때 현재 탭의 UI도 업데이트
        if (isCurrentlyAuthenticated && currentMainTabIndex == 4) {
            // 더보기 탭이 활성화되어 있고 로그인된 상태라면 UI 새로고침
            val currentFragment = supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
            if (currentFragment is com.stip.stip.more.fragment.MoreFragment) {
                // MoreFragment의 onResume을 강제로 호출하여 회원정보 새로고침
                currentFragment.onResume()
            }
        }
    }

    // 헤더/탭 숨기기 (기존 코드 유지)
    fun hideHeaderAndTabs() {
        binding.headerLayout.visibility = View.GONE
        binding.secondaryTabLayout.visibility = View.GONE
    }

    // 헤더 보이기 (기존 코드 유지)
    fun showHeader() {
        binding.headerLayout.visibility = View.VISIBLE
        // 상단 탭은 현재 메인 탭 인덱스에 따라 결정되므로 여기서는 headerLayout만 보이게 함
        binding.secondaryTabLayout.visibility =
            if (currentMainTabIndex == 1 || currentMainTabIndex == 2) View.VISIBLE else View.GONE
    }

    // 하단 탭 아이콘 업데이트 (기존 코드 유지)
    private fun updateBottomTabIcons(selectedIndex: Int) {
        val activeIcons = listOf(
            R.drawable.ic_tab_iphome_active,
            R.drawable.ic_tab_ipinfo_active,
            R.drawable.ic_tab_transaction_active,
            R.drawable.ic_tab_ipasset_active,
            R.drawable.ic_tab_more_active
        )
        val inactiveIcons = listOf(
            R.drawable.ic_tab_iphome_inactive,
            R.drawable.ic_tab_ipinfo_inactive,
            R.drawable.ic_tab_transaction_inactive,
            R.drawable.ic_tab_ipasset_inactive,
            R.drawable.ic_tab_more_inactive
        )
        binding.iconIphome.setImageResource(if (selectedIndex == 0) activeIcons[0] else inactiveIcons[0])
        binding.iconIpinfo.setImageResource(if (selectedIndex == 1) activeIcons[1] else inactiveIcons[1])
        binding.iconIptransaction.setImageResource(if (selectedIndex == 2) activeIcons[2] else inactiveIcons[2])
        binding.iconIpasset.setImageResource(if (selectedIndex == 3) activeIcons[3] else inactiveIcons[3])
        binding.iconMore.setImageResource(if (selectedIndex == 4) activeIcons[4] else inactiveIcons[4])
    }

    private fun showLoginRequiredDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_log_in_inform, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // 제목과 메시지 설정
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialogLoginInformTitle)
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialogLoginInformMessage)
        titleTextView.text = "로그인 필요"
        messageTextView.text = "이 기능을 사용하려면 로그인이 필요합니다."
        
        // 확인 버튼 (로그인)
        val confirmButton = dialogView.findViewById<TextView>(R.id.dialogLoginInformButtonConfirm)
        confirmButton.text = "로그인"
        confirmButton.setOnClickListener {
            val storedDi = com.stip.stip.signup.utils.PreferenceUtil.getString(com.stip.stip.signup.Constants.PREF_KEY_DI_VALUE, "")
            if (storedDi.isNotBlank()) {
                // DI가 있으면 회원 정보 조회
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    val memberInfoResponse = loginViewModel.getMemberInfo()
                    var isMember = false
                    memberInfoResponse.onSuccess {
                        isMember = true
                    }
                    if (isMember) {
                        // 회원가입이 완전히 끝난 회원 → PIN 로그인 화면 이동
                        val intent = Intent(this@MainActivity, com.stip.stip.signup.login.LoginPinNumberActivity::class.java).apply {
                            putExtra("di_value", storedDi)
                        }
                        startActivity(intent)
                    } else {
                        // 회원가입 미완료 → 회원가입 화면 이동
                        val intent = Intent(this@MainActivity, com.stip.stip.signup.signup.SignUpActivity::class.java)
                        startActivity(intent)
                    }
                    dialog.dismiss()
                }
            } else {
                // DI가 없으면 회원가입 화면으로 이동
                val intent = Intent(this@MainActivity, com.stip.stip.signup.signup.SignUpActivity::class.java)
                startActivity(intent)
                dialog.dismiss()
            }
        }
        
        // 취소 버튼
        val cancelButton = dialogView.findViewById<TextView>(R.id.dialogLoginInformButtonCancel)
        cancelButton.text = "취소"
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    /**
     * 전화 사기 경고 다이얼로그를 표시해야 하는지 확인
     * 하루에 한 번만 표시되도록 처리
     */
    private fun shouldShowPhoneFraudAlert(): Boolean {
        // PreferenceUtil이 초기화 확인 후 SharedPreferences 사용
        val sharedPreferences = getSharedPreferences("phone_fraud_alert_prefs", MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastShownDate = sharedPreferences.getString("last_shown_date", "")
        
        // 오늘 이미 표시했으면 다시 표시하지 않음
        return lastShownDate != today
    }
    
    /**
     * 전화 사기 경고 다이얼로그 표시
     */
    fun showPhoneFraudAlertDialog() {
        val dialogView = layoutInflater.inflate(R.layout.fragment_phone_fraud_alert_dialog, null)
        
        // BottomSheetDialog 대신 AlertDialog 사용하여 전체 내용이 한 번에 보이도록 함
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
            
        // 다이얼로그 크기 설정
        dialog.window?.let { window ->
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            val params = window.attributes
            params.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            window.attributes = params
        }
        
        // 다이얼로그의 모든 내용이 한 번에 잘 보이도록 최적화
        // ScrollView가 명시적 ID 없이 레이아웃에 포함되어 있어서 직접 찾기 어려움
        // 대신에 전체 다이얼로그 높이를 화면의 80%로 제한
        dialog.window?.let { window ->
            val params = window.attributes
            params.height = (resources.displayMetrics.heightPixels * 0.8).toInt()
            window.attributes = params
        }
        
        // 확인 버튼 클릭 처리
        dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btn_confirm).setOnClickListener {
            // 임시로 오늘 날짜를 저장해서 IpAssetFragment에서 다이얼로그를 표시하지 않도록 함
            val sharedPreferences = getSharedPreferences("phone_fraud_alert_prefs", MODE_PRIVATE)
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            
            // 임시 플래그 설정 (다음 프래그먼트 생성 시에만 적용)
            sharedPreferences.edit().putString("temp_shown_date", today).commit()
            
            dialog.dismiss()
            handleBottomTabClick(3, force = true)
        }
        
        // 오늘 하루 보지 않기 버튼 클릭 처리
        dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btn_cancel).setOnClickListener {
            // 오늘 날짜를 YYYY-MM-DD 형식으로 저장 (SharedPreferences 사용)
            val sharedPreferences = getSharedPreferences("phone_fraud_alert_prefs", MODE_PRIVATE)
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            
            sharedPreferences.edit().putString("last_shown_date", today).commit()
            
            dialog.dismiss()
            handleBottomTabClick(3, force = true)
        }
        
        dialog.show()
    }

    // companion object (기존 코드 유지)
    companion object {
        fun startMainActivity(activity: Activity) {
            val intent = Intent(activity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            activity.startActivity(intent)
            activity.finish()
        }
    }
}

// ScrollableToTop 인터페이스 (기존 코드 유지)
interface ScrollableToTop {
    fun scrollToTop()
}