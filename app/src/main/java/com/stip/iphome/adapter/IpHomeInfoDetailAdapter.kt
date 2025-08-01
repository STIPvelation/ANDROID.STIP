package com.stip.stip.iphome.adapter

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.databinding.ItemRegistrationNumberBinding
import com.stip.stip.iphome.constants.IpDetailInfo
import com.stip.stip.iphome.model.IpListingItem

class IpHomeInfoDetailAdapter(private var items: List<IpListingItem>) :
    RecyclerView.Adapter<IpHomeInfoDetailAdapter.IpHomeInfoDetailViewHolder>() {

    inner class IpHomeInfoDetailViewHolder(private val binding: ItemRegistrationNumberBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // bind 함수 수정: SNS 부분 로직 변경
        fun bind(item: IpListingItem) {
            // --- item_registration_number.xml (10개 항목 버전) 의 ID 를 사용 ---

            // --- 법인명, 대표자 바인딩 없음 (주석 유지) ---
            // binding.tvValueCorporationName.text = item.companyName
            // binding.tvValueRepresentative.text = item.representative ?: "-"

            // --- 나머지 4개 기본 항목 바인딩 ---
            // tickerToBusinessNumber 맵에서 등록번호 가져오기
            val businessNumber = IpDetailInfo.getBusinessNumberForTicker(item.ticker)
            binding.tvValueRegistrationNumber.text = businessNumber.ifBlank { item.registrationNumber ?: "-" } // 등록번호
            
            // 티커별 카테고리 설정: AXNO는 BM, 나머지는 특허
            val category = when (item.ticker) {
                "AXNO" -> "BM"
                else -> "특허"
            }
            binding.tvValueBusinessType.text = category // 업태(카테고리)
            
            binding.tvValueContactEmail.text = item.contactEmail ?: "-" // 연락처
            binding.tvValueAddressDetail.text = item.address ?: "-" // 소재지

            // --- ▼▼▼ SNS 항목 바인딩 수정 ▼▼▼ ---
            val context = binding.root.context // Context 가져오기

            // Twitter
            binding.tvValueSnsTwitter.text = context.getString(R.string.sns_twitter) // 기본 텍스트: 플랫폼 이름
            binding.tvValueSnsTwitter.setOnClickListener {
                handleSnsClick(item.snsTwitter, context.getString(R.string.sns_twitter) + " 링크가 없습니다.")
            }

            // Instagram
            binding.tvValueSnsInstagram.text = context.getString(R.string.sns_instagram) // 기본 텍스트: 플랫폼 이름
            binding.tvValueSnsInstagram.setOnClickListener {
                handleSnsClick(item.snsInstagram, context.getString(R.string.sns_instagram) + " 링크가 없습니다.")
            }

            // KakaoTalk
            binding.tvValueSnsKakaotalk.text = context.getString(R.string.sns_kakaotalk) // 기본 텍스트: 플랫폼 이름
            binding.tvValueSnsKakaotalk.setOnClickListener {
                handleSnsClick(item.snsKakaoTalk, context.getString(R.string.sns_kakaotalk) + " 링크가 없습니다.")
            }

            // Telegram
            binding.tvValueSnsTelegram.text = context.getString(R.string.sns_telegram) // 기본 텍스트: 플랫폼 이름
            binding.tvValueSnsTelegram.setOnClickListener {
                handleSnsClick(item.snsTelegram, context.getString(R.string.sns_telegram) + " 링크가 없습니다.")
            }

            // LinkedIn
            binding.tvValueSnsLinkedin.text = context.getString(R.string.sns_linkedin) // 기본 텍스트: 플랫폼 이름
            binding.tvValueSnsLinkedin.setOnClickListener {
                handleSnsClick(item.snsLinkedIn, context.getString(R.string.sns_linkedin) + " 링크가 없습니다.")
            }

            // WeChat
            binding.tvValueSnsWechat.text = context.getString(R.string.sns_wechat) // 기본 텍스트: 플랫폼 이름
            binding.tvValueSnsWechat.setOnClickListener {
                handleSnsClick(item.snsWeChat, context.getString(R.string.sns_wechat) + " 정보가 없습니다.")
            }
            // --- ▲▲▲ SNS 항목 바인딩 수정 끝 ▲▲▲ ---
        }

        // SNS 클릭 처리 헬퍼 함수 (ViewHolder 내부에 추가)
        private fun handleSnsClick(url: String?, noLinkMessage: String) {
            val context = binding.root.context
            if (!url.isNullOrBlank()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()))
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "링크를 열 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("IpHomeInfoDetailAdapter", "URL 열기 실패: $url", e)
                    Toast.makeText(context, "링크 형식이 잘못되었거나 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, noLinkMessage, Toast.LENGTH_SHORT).show()
            }
        }
    } // End of ViewHolder

    // --- onCreateViewHolder, onBindViewHolder, getItemCount, updateData 함수는 동일 ---
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IpHomeInfoDetailViewHolder {
        val binding = ItemRegistrationNumberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IpHomeInfoDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IpHomeInfoDetailViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<IpListingItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}