package com.stip.stip.iphome.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.stip.stip.R
import com.stip.stip.databinding.ItemIpListingBinding
import com.stip.stip.iphome.fragment.TradingFragment
import com.stip.stip.iphome.model.IpListingItem
import com.stip.stip.iphome.model.IpCategory
import com.stip.stip.iphome.view.OhlcMiniBarView

class IpListingAdapter(var items: List<IpListingItem>) :
    RecyclerView.Adapter<IpListingAdapter.IpListingViewHolder>() {

    inner class IpListingViewHolder(val binding: ItemIpListingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IpListingItem) {
            val context = binding.root.context

            // 기본 배경색
            binding.frame7484.setBackgroundColor(ContextCompat.getColor(context, R.color.white))

            // 거래 발생 시 깜빡임 효과
            if (item.isTradeTriggered) {
                val flashColor = if (item.isBuy) {
                    ContextCompat.getColor(context, R.color.trade_buy_background)
                } else {
                    ContextCompat.getColor(context, R.color.trade_sell_background)
                }
                binding.frame7484.setBackgroundColor(flashColor)
                binding.frame7484.postDelayed({
                    binding.frame7484.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.white)
                    )
                }, 300)
            }

            // 텍스트 설정 (symbol 표시 - /USD 제거)
            val tickerOnly = item.symbol.substringBefore("/")
            binding.itemTickerName.text = tickerOnly
            
            // 카테고리 텍스트 표시 (Patent 또는 BM)
            binding.itemCategoryText.visibility = View.VISIBLE
            binding.itemCategoryText.text = if (tickerOnly == "AXNO") "BM" else "Patent"
            
            binding.itemCurrentPrice.text = item.currentPrice
            binding.itemChangePercent.text = item.changePercent
            binding.itemChangeAbsolute.text = item.changeAbsolute
            binding.itemVolume.text = item.volume

            // 상승/하락에 따른 텍스트 색상
            val priceColor = when {
                item.changePercent.startsWith("+") -> ContextCompat.getColor(context, R.color.red)
                item.changePercent.startsWith("-") -> ContextCompat.getColor(context, R.color.blue)
                else -> ContextCompat.getColor(context, R.color.text_primary)
            }

            binding.itemCurrentPrice.setTextColor(priceColor)
            binding.itemChangePercent.setTextColor(priceColor)
            binding.itemChangeAbsolute.setTextColor(priceColor)
            binding.itemVolume.setTextColor(ContextCompat.getColor(context, R.color.text_primary))

            // OHLC 데이터 세팅
            val open = item.open.toFloatOrNull() ?: 0f
            val high = item.high.toFloatOrNull() ?: 0f
            val low = item.low.toFloatOrNull() ?: 0f
            val close = item.close.toFloatOrNull() ?: 0f
            val isRise = close >= open

            binding.viewOhlc.setOhlc(open, high, low, close, isRise)

            // 텍스트 깜빡임
            if (item.isTradeTriggered) {
                animatePriceFlash(binding.itemCurrentPrice, item.isBuy)
            }

            // 클릭 시 로그인 상태 확인 후 거래 화면으로 이동
            binding.root.setOnClickListener {
                val activity = binding.root.context as? AppCompatActivity
                if (activity != null) {
                    // 로그인 상태 상세 확인
                    val token = com.stip.stip.signup.utils.PreferenceUtil.getToken()
                    val isGuest = com.stip.stip.signup.utils.PreferenceUtil.isGuestMode()
                    val isLoggedIn = com.stip.stip.signup.utils.PreferenceUtil.isRealLoggedIn()
                    
                    Log.d("IpListingAdapter", "실시권 클릭 - 토큰: ${token != null}, 게스트모드: $isGuest, 실제로그인: $isLoggedIn")
                    
                    if (isLoggedIn) {
                        // 로그인된 경우 거래 화면으로 이동
                        Log.d("IpListingAdapter", "로그인 상태 확인됨 - TradingFragment로 이동: ${item.ticker}")
                        val fragment = TradingFragment.newInstance(item.ticker, item.ticker)
                        activity.supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit()
                    } else {
                        // 로그인되지 않은 경우 로그인 필요 다이얼로그 표시
                        Log.d("IpListingAdapter", "로그인 상태 아님 - 로그인 다이얼로그 표시")
                        showLoginRequiredDialog(activity)
                    }
                }
            }

            // 로그 출력
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                Log.d(
                    "IpListingAdapter",
                    "[$position] isTradeTriggered=${item.isTradeTriggered}, isBuy=${item.isBuy}"
                )
            }
        }

        private fun animatePriceFlash(view: TextView, isBuy: Boolean) {
            val borderDrawable =
                if (isBuy) R.drawable.flash_red_border else R.drawable.flash_blue_border
            view.setBackgroundResource(borderDrawable)
            view.postDelayed({ view.setBackgroundResource(0) }, 300)
        }

        private fun showLoginRequiredDialog(activity: AppCompatActivity) {
            val dialogView = activity.layoutInflater.inflate(R.layout.dialog_log_in_inform, null)
            val dialog = androidx.appcompat.app.AlertDialog.Builder(activity)
                .setView(dialogView)
                .create()
            
            // 제목과 메시지 설정
            val titleTextView = dialogView.findViewById<android.widget.TextView>(R.id.dialogLoginInformTitle)
            val messageTextView = dialogView.findViewById<android.widget.TextView>(R.id.dialogLoginInformMessage)
            titleTextView.text = "로그인 필요"
            messageTextView.text = "이 기능을 사용하려면 로그인이 필요합니다."
            
            // 확인 버튼 (로그인)
            val confirmButton = dialogView.findViewById<android.widget.TextView>(R.id.dialogLoginInformButtonConfirm)
            confirmButton.text = "로그인"
            confirmButton.setOnClickListener {
                val storedDi = com.stip.stip.signup.utils.PreferenceUtil.getString(com.stip.stip.signup.Constants.PREF_KEY_DI_VALUE, "")
                if (storedDi.isNotBlank()) {
                    // DI가 있으면 PIN 로그인 화면으로 이동
                    val intent = android.content.Intent(activity, com.stip.stip.signup.login.LoginPinNumberActivity::class.java).apply {
                        putExtra("di_value", storedDi)
                    }
                    activity.startActivity(intent)
                } else {
                    // DI가 없으면 회원가입 화면으로 이동
                    val intent = android.content.Intent(activity, com.stip.stip.signup.signup.SignUpActivity::class.java)
                    activity.startActivity(intent)
                }
                dialog.dismiss()
            }
            
            // 취소 버튼
            val cancelButton = dialogView.findViewById<android.widget.TextView>(R.id.dialogLoginInformButtonCancel)
            cancelButton.text = "취소"
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
            
            dialog.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IpListingViewHolder {
        val binding =
            ItemIpListingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IpListingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IpListingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<IpListingItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
