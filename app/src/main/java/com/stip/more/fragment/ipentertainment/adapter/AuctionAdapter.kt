package com.stip.stip.more.fragment.ipentertainment.adapter

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stip.stip.R
import com.stip.stip.databinding.ItemAuctionBinding
import com.stip.stip.more.fragment.ipentertainment.data.AuctionModel
import com.stip.stip.more.fragment.ipentertainment.data.IpType
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AuctionAdapter : ListAdapter<AuctionModel, AuctionAdapter.AuctionViewHolder>(AuctionDiffCallback()) {

    private var onItemClickListener: ((AuctionModel, Int) -> Unit)? = null

    fun setOnItemClickListener(listener: (AuctionModel, Int) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuctionViewHolder {
        val binding = ItemAuctionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AuctionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AuctionViewHolder, position: Int) {
        val auction = getItem(position)
        holder.bind(auction, position)
    }

    inner class AuctionViewHolder(private val binding: ItemAuctionBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onItemClickListener?.invoke(getItem(bindingAdapterPosition), bindingAdapterPosition)
            }
        }

        fun bind(auction: AuctionModel, position: Int) {
            // 제목 설정
            binding.textAuctionTitle.text = auction.title
            
            // 등록번호 설정 - 포맷 통일
            binding.textRegistrationNumber.text = "IP #${auction.registrationNumber}"
            
            // 가격 포맷팅 (달러 기반)
            binding.textAuctionPrice.text = String.format("$%,d", auction.currentPrice)
            
            // 남은 시간 계산
            val remainingTime = getRemainingTimeText(auction.endTime)
            binding.textTimeRemaining.text = remainingTime
            
            // 참여자 수 표시 - 포맷 통일
            binding.textBidCount.text = "${String.format("%d", auction.bidCount)} bids"
            
            // 이미지 로딩 개선
            try {
                Glide.with(itemView.context)
                    .load(auction.imageUrl)
                    .placeholder(R.drawable.ic_ipentertainment_auction)
                    .error(R.drawable.ic_ipentertainment_auction)
                    .centerCrop() // iOS와 동일한 이미지 처리
                    .into(binding.imageAuction)
            } catch (e: Exception) {
                // 이미지 로딩 실패 시 기본 이미지 직접 설정
                binding.imageAuction.setImageResource(R.drawable.ic_ipentertainment_auction)
                e.printStackTrace()
            }
            
            // IP 유형 태그 설정
            binding.badgeIpType.apply {
                text = auction.ipType.displayName
                
                // 배경색 동적 설정
                val tagBackground = ContextCompat.getDrawable(context, R.drawable.tag_background) as? GradientDrawable
                tagBackground?.setColor(auction.ipType.getColor())
                background = tagBackground
            }
        }
        
        private fun getRemainingTimeText(endTime: Date): String {
            val now = Date()
            val diffInMillis = endTime.time - now.time
            
            if (diffInMillis <= 0) {
                return "마감됨"
            }
            
            // iOS와 동일한 시간 형식으로 변경
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
            val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
            val days = TimeUnit.HOURS.toDays(hours)
            
            return when {
                days > 0 -> "${days} days left"
                hours > 0 -> "${hours} hours left"
                minutes > 0 -> "${minutes} min left"
                else -> "<1 min left"
            }
        }
    }

    class AuctionDiffCallback : DiffUtil.ItemCallback<AuctionModel>() {
        override fun areItemsTheSame(oldItem: AuctionModel, newItem: AuctionModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AuctionModel, newItem: AuctionModel): Boolean {
            return oldItem.id == newItem.id &&
                   oldItem.title == newItem.title &&
                   oldItem.currentPrice == newItem.currentPrice &&
                   oldItem.endTime == newItem.endTime &&
                   oldItem.ipType == newItem.ipType &&
                   oldItem.registrationNumber == newItem.registrationNumber &&
                   oldItem.bidCount == newItem.bidCount
        }
    }
}
