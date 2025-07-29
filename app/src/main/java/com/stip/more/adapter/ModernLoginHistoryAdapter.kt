package com.stip.stip.more.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.more.model.LoginRecord
import java.text.SimpleDateFormat
import java.util.*

/**
 * 로그인 이력을 모던한 카드 UI로 표시하기 위한 어댑터
 * iOS의 LoginRecordRow와 유사한 디자인으로 구현
 */
class ModernLoginHistoryAdapter : ListAdapter<LoginRecord, ModernLoginHistoryAdapter.LoginHistoryViewHolder>(LoginHistoryDiffCallback()) {
    
    companion object {
        private const val TAG = "ModernLoginHistoryAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoginHistoryViewHolder {
        Log.d(TAG, "onCreateViewHolder called")
        try {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.item_modern_login_history,
                parent,
                false
            )
            Log.d(TAG, "View inflated successfully")
            return LoginHistoryViewHolder(view)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ViewHolder: ${e.message}")
            throw e
        }
    }

    override fun onBindViewHolder(holder: LoginHistoryViewHolder, position: Int) {
        try {
            Log.d(TAG, "onBindViewHolder called for position $position")
            val loginRecord = getItem(position)
            Log.d(TAG, "Binding data: ${loginRecord.ipAddress}, ${loginRecord.location}")
            holder.bind(loginRecord)
        } catch (e: Exception) {
            Log.e(TAG, "Error binding ViewHolder at position $position: ${e.message}")
        }
    }

    class LoginHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewHolderTag = TAG // 외부 클래스의 TAG 참조
        private val tvDateTime: TextView = itemView.findViewById(R.id.tv_date_time)
        private val tvUnusualBadge: TextView = itemView.findViewById(R.id.tv_unusual_badge)
        private val tvIpAddressLabel: TextView = itemView.findViewById(R.id.tv_ip_address_label)
        private val tvIpAddressValue: TextView = itemView.findViewById(R.id.tv_ip_address_value)
        private val tvLocationLabel: TextView = itemView.findViewById(R.id.tv_location_label)
        private val tvLocationValue: TextView = itemView.findViewById(R.id.tv_location_value)
        private val tvDeviceLabel: TextView = itemView.findViewById(R.id.tv_device_label)
        private val tvDeviceValue: TextView = itemView.findViewById(R.id.tv_device_value)
        private val cardView: CardView = itemView.findViewById(R.id.card_login_history)

        fun bind(loginRecord: LoginRecord) {
            try {
                // 날짜 포맷팅
                val dateFormatter = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
                }
                tvDateTime.text = dateFormatter.format(loginRecord.date)
                Log.d(viewHolderTag, "DateTime set: ${dateFormatter.format(loginRecord.date)}")

                // 비정상 접속 배지 표시
                if (loginRecord.isUnusual) {
                    tvUnusualBadge.visibility = View.VISIBLE
                    // 비정상 접속인 경우 카드 그림자 더 진하게 설정
                    try {
                        cardView.cardElevation = cardView.resources.getDimension(R.dimen.card_elevation_unusual)
                        Log.d(viewHolderTag, "Set unusual card elevation")
                    } catch (e: Exception) {
                        Log.e(viewHolderTag, "Error setting card elevation: ${e.message}")
                        cardView.cardElevation = 8f // 8dp로 기본값 지정
                    }
                } else {
                    tvUnusualBadge.visibility = View.GONE
                    try {
                        cardView.cardElevation = cardView.resources.getDimension(R.dimen.card_elevation_normal)
                        Log.d(viewHolderTag, "Set normal card elevation")
                    } catch (e: Exception) {
                        Log.e(viewHolderTag, "Error setting card elevation: ${e.message}")
                        cardView.cardElevation = 4f // 4dp로 기본값 지정
                    }
                }

                // 정보 설정
                tvIpAddressLabel.text = "IP 주소"
                tvIpAddressValue.text = loginRecord.ipAddress
                Log.d(viewHolderTag, "IP set: ${loginRecord.ipAddress}")
                
                tvLocationLabel.text = "위치"
                tvLocationValue.text = loginRecord.location
                Log.d(viewHolderTag, "Location set: ${loginRecord.location}")
                
                tvDeviceLabel.text = "기기"
                tvDeviceValue.text = loginRecord.deviceInfo
                Log.d(viewHolderTag, "Device info set: ${loginRecord.deviceInfo}")
            } catch (e: Exception) {
                Log.e(viewHolderTag, "Error in bind method: ${e.message}")
            }
        }
    }

    class LoginHistoryDiffCallback : DiffUtil.ItemCallback<LoginRecord>() {
        override fun areItemsTheSame(oldItem: LoginRecord, newItem: LoginRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LoginRecord, newItem: LoginRecord): Boolean {
            return oldItem == newItem
        }
    }
}
