package com.stip.stip.more.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.MainViewModel
import com.stip.stip.R
import android.widget.TextView
import com.stip.stip.more.api.LoginHistoryItem
import com.stip.stip.more.api.LoginHistoryResponse
import com.stip.stip.more.api.LoginHistoryService
import com.stip.stip.signup.utils.PreferenceUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale

class LoginHistoryFragment : Fragment() {

    // Using direct view references since databinding isn't generating binding classes
    
    private val viewModel: MainViewModel by activityViewModels()
    
    private val loginHistoryAdapter by lazy { LoginHistoryAdapter() }
    
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://tapi.sharetheip.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private val loginHistoryService by lazy {
        retrofit.create(LoginHistoryService::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Using the layout resource ID directly to avoid ambiguity
        return inflater.inflate(com.stip.stip.R.layout.fragment_more_login_history2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.updateHeaderTitle(getString(R.string.login_history_title))
        viewModel.enableBackNavigation {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        setupRecyclerView()
        loadLoginHistory()
    }
    
    private fun setupRecyclerView() {
        val recyclerView = view?.findViewById<RecyclerView>(com.stip.stip.R.id.recycler_login_history)
        recyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = loginHistoryAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }
    
    private fun loadLoginHistory() {
        val progressBar = view?.findViewById<View>(com.stip.stip.R.id.progress_bar)
        val textNoHistory = view?.findViewById<View>(com.stip.stip.R.id.text_no_history)
        
        progressBar?.visibility = View.VISIBLE
        textNoHistory?.visibility = View.GONE
        
        // 2초 후에도 로딩이 계속되면 프로그레스바 숨기기
        Handler(Looper.getMainLooper()).postDelayed({
            val progressBar = view?.findViewById<View>(com.stip.stip.R.id.progress_bar)
            if (progressBar?.visibility == View.VISIBLE) {
                progressBar.visibility = View.GONE
            }
        }, 2000)
        
        // JWT 토큰 가져오기
        val token = PreferenceUtil.getString("jwt_token")
        if (token.isEmpty()) {
            showNoHistoryMessage("로그인이 필요합니다")
            return
        }
        
        // 로그인 이력 API 호출
        loginHistoryService.getLoginHistory("Bearer $token")
            .enqueue(object : Callback<LoginHistoryResponse> {
                override fun onResponse(
                    call: Call<LoginHistoryResponse>,
                    response: Response<LoginHistoryResponse>
                ) {
                    view?.findViewById<View>(R.id.progress_bar)?.visibility = View.GONE
                    
                    if (response.isSuccessful) {
                        val data = response.body()?.data
                        if (data.isNullOrEmpty()) {
                            showNoHistoryMessage("로그인 이력이 없습니다")
                        } else {
                            // 최대 3개의 디바이스만 표시
                            val maxDevices = 3
                            val currentDevicesList = data.filter { it.isCurrentDevice }
                            val otherDevicesList = data.filter { !it.isCurrentDevice }
                            
                            // 현재 디바이스는 반드시 포함하고, 나머지 공간에 다른 기기 표시
                            val finalList = currentDevicesList.toMutableList()
                            val remainingSlots = maxDevices - finalList.size
                            
                            if (remainingSlots > 0 && otherDevicesList.isNotEmpty()) {
                                finalList.addAll(otherDevicesList.take(remainingSlots))
                            }
                            
                            loginHistoryAdapter.submitList(finalList)
                            
                            // 최대 기기 수 알림 표시 (현재 기기 수가 3개 이상이면)
                            if (data.size > maxDevices) {
                                Toast.makeText(
                                    requireContext(),
                                    "최대 ${maxDevices}개의 디바이스만 로그인 가능합니다. 현재 ${data.size}개의 디바이스가 로그인 되어 있습니다.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        Log.e("LoginHistory", "응답 실패: ${response.errorBody()?.string()}")
                        showNoHistoryMessage("로그인 이력을 불러오는데 실패했습니다")
                    }
                }

                override fun onFailure(call: Call<LoginHistoryResponse>, t: Throwable) {
                    view?.findViewById<View>(R.id.progress_bar)?.visibility = View.GONE
                    Log.e("LoginHistory", "API 호출 실패", t)
                    showNoHistoryMessage("네트워크 오류가 발생했습니다")
                }
            })
    }
    
    private fun showNoHistoryMessage(message: String) {
        val textNoHistory = view?.findViewById<TextView>(com.stip.stip.R.id.text_no_history)
        textNoHistory?.apply {
            text = message
            visibility = View.VISIBLE
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
    }
    
    // 로그인 이력 어댑터
    private inner class LoginHistoryAdapter : 
        RecyclerView.Adapter<LoginHistoryAdapter.LoginHistoryViewHolder>() {
        
        private var items: List<LoginHistoryItem> = emptyList()
        
        fun submitList(newItems: List<LoginHistoryItem>) {
            items = newItems
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoginHistoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(com.stip.stip.R.layout.item_modern_login_history, parent, false)
            return LoginHistoryViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: LoginHistoryViewHolder, position: Int) {
            holder.bind(items[position])
        }
        
        override fun getItemCount(): Int = items.size
        
        inner class LoginHistoryViewHolder(itemView: View) : 
            RecyclerView.ViewHolder(itemView) {
            
            private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
            }
            private val tvDateTime = itemView.findViewById<TextView>(com.stip.stip.R.id.tv_date_time)
            private val tvDeviceValue = itemView.findViewById<TextView>(com.stip.stip.R.id.tv_device_value)
            private val tvIpAddressValue = itemView.findViewById<TextView>(com.stip.stip.R.id.tv_ip_address_value)
            private val tvLocationValue = itemView.findViewById<TextView>(com.stip.stip.R.id.tv_location_value)
            
            fun bind(item: LoginHistoryItem) {
                // 로그인 시간
                tvDateTime.text = dateFormat.format(item.loginTime)
                
                // 디바이스 정보
                tvDeviceValue.text = item.deviceInfo
                
                // IP 및 위치 정보
                tvIpAddressValue.text = item.ipAddress
                tvLocationValue.text = item.location
                
                // 현재 디바이스 표시
                itemView.setBackgroundResource(
                    if (item.isCurrentDevice) com.stip.stip.R.color.light_blue_bg else android.R.color.transparent
                )
            }
        }
    }
}
