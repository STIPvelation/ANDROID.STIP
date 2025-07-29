package com.stip.stip.more.fragment

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.stip.stip.R
import com.stip.stip.databinding.FragmentMoreLoginHistoryBinding
import com.stip.stip.more.adapter.FilterPeriodAdapter
import com.stip.stip.more.adapter.ModernLoginHistoryAdapter
import com.stip.stip.more.model.LoginRecord
import com.stip.stip.more.viewmodel.LoginHistoryViewModel
import com.stip.stip.signup.Constants
import com.stip.stip.signup.login.LoginActivity
import com.stip.stip.signup.utils.PreferenceUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

class MoreLoginHistoryFragment : Fragment() {
    
    companion object {
        private const val TAG = "LoginHistoryFragment"
    }

    private lateinit var binding: FragmentMoreLoginHistoryBinding
    private lateinit var adapter: ModernLoginHistoryAdapter
    private lateinit var viewModel: LoginHistoryViewModel
    
    // UI 컴포넌트 직접 참조
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var textNoHistory: TextView
    private lateinit var filterButton: MaterialButton
    
    // 필터 기간 옵션
    private val filterPeriods = listOf("전체", "1주일", "1개월", "3개월", "6개월", "1년")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMoreLoginHistoryBinding.inflate(inflater, container, false)
        // ViewModel 초기화
        viewModel = ViewModelProvider(this)[LoginHistoryViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d(TAG, "onViewCreated started")
        
        // UI 컴포넌트 참조 가져오기
        try {
            recyclerView = view.findViewById(R.id.recycler_view_login_history)
            progressBar = view.findViewById(R.id.progressBar)
            textNoHistory = view.findViewById(R.id.textNoHistory)
            filterButton = view.findViewById(R.id.btn_filter_period)
            Log.d(TAG, "UI components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing UI components: ${e.message}")
        }
        
        setupAdapter()
        setupObservers()
        setupFilterButton()

        // 로그인 이력 데이터 로드
        viewModel.fetchLoginHistory()

        // 모든 기기에서 로그아웃 → 로그인 화면 이동
        binding.btnLogoutAll.setOnClickListener {
            logoutAllAndRedirect()
        }
    }
    
    private fun setupAdapter() {
        try {
            Log.d(TAG, "Setting up adapter for login history")
            adapter = ModernLoginHistoryAdapter()
            if (!::recyclerView.isInitialized) {
                Log.e(TAG, "RecyclerView is not initialized! Attempting to find it.")
                recyclerView = view?.findViewById(R.id.recycler_view_login_history) 
                    ?: throw IllegalStateException("RecyclerView not found!")
            }
            
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter
            
            Log.d(TAG, "Adapter setup completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up adapter: ${e.message}")
            Toast.makeText(context, "로그인 이력을 불러오는데 문제가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupObservers() {
        // 로그인 이력 데이터 관찰
        viewModel.loginHistory.observe(viewLifecycleOwner) { data ->
            Log.d(TAG, "LoginHistory data received, size: ${data?.size ?: 0}")
            updateLoginHistoryDisplay()
        }
        
        // 로딩 상태 관찰
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogoutAll.isEnabled = !isLoading
        }
        
        // 필터 기간 관찰
        viewModel.filterPeriod.observe(viewLifecycleOwner) { period ->
            filterButton.text = "기간: $period"
            updateLoginHistoryDisplay()
        }
    }
    
    private fun setupFilterButton() {
        // 필터 버튼 초기화 및 클릭 리스너 설정
        filterButton.text = "기간: 전체"
        filterButton.setOnClickListener {
            showFilterPeriodDialog()
        }
    }

    /**
     * 로그인 이력 데이터 표시 업데이트
     * 필터링된 데이터를 어댑터에 전달하고 UI 상태를 업데이트
     */
    private fun updateLoginHistoryDisplay() {
        val filteredHistory = viewModel.getFilteredHistory()
        
        Log.d(TAG, "Updating login history display. Data size: ${filteredHistory.size}")
        
        if (filteredHistory.isEmpty()) {
            Log.d(TAG, "No login history data to display")
            recyclerView.visibility = View.GONE
            textNoHistory.visibility = View.VISIBLE
            textNoHistory.text = getString(R.string.login_history_no_data)
        } else {
            Log.d(TAG, "Displaying ${filteredHistory.size} login history items")
            recyclerView.visibility = View.VISIBLE
            textNoHistory.visibility = View.GONE
            adapter.submitList(filteredHistory)
        }
    }
    
    /**
     * 필터 기간 선택 다이얼로그 표시
     */
    private fun showFilterPeriodDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_filter_period)
        
        val periodRecyclerView = dialog.findViewById<RecyclerView>(R.id.rv_filter_period)
        val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel)
        
        // 필터 어댑터 설정
        val currentPeriod = viewModel.filterPeriod.value ?: "전체"
        val filterAdapter = FilterPeriodAdapter(filterPeriods, currentPeriod) { selectedPeriod ->
            viewModel.setFilterPeriod(selectedPeriod)
            dialog.dismiss()
        }
        
        periodRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        periodRecyclerView.adapter = filterAdapter
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        
        dialog.show()
    }
    
    /**
     * 기존 메서드 유지 (ViewModel로 이동 예정)
     */
    private fun loadLoginHistory() {
        viewModel.fetchLoginHistory()
    }
    
    /**
     * 에러 메시지 표시
     */
    private fun showNoHistoryMessage(message: String) {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        textNoHistory.visibility = View.VISIBLE
        textNoHistory.text = message
    }

    /**
     * 모든 기기에서 로그아웃 처리 및 로그인 화면으로 이동
     * ViewModel 사용하여 처리
     */
    private fun logoutAllAndRedirect() {
        // 로그아웃 전 확인 다이얼로그 표시
        AlertDialog.Builder(requireContext())
            .setTitle("모든 기기에서 로그아웃")
            .setMessage("모든 기기에서 로그아웃하시겠습니까?")
            .setPositiveButton("로그아웃") { _, _ ->
                viewModel.logoutFromAllDevices {
                    clearLocalSessionAndRedirect()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    /**
     * 로컬 세션 정보를 삭제하고 로그인 화면으로 이동
     */
    private fun clearLocalSessionAndRedirect() {
        // 로컬 세션 정보 삭제
        PreferenceUtil.putString(Constants.PREF_KEY_AUTH_TOKEN_VALUE, "")
        // PreferenceUtil.KEY_MEMBER_INFO는 private이므로 직접 상수 문자열 사용
        PreferenceUtil.remove("member_info_json")
        
        Toast.makeText(requireContext(), "모든 기기에서 로그아웃되었습니다", Toast.LENGTH_SHORT).show()
        
        // 로그인 화면으로 이동
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
