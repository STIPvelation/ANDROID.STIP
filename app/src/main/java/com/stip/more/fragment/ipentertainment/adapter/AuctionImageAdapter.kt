package com.stip.stip.more.fragment.ipentertainment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.stip.stip.R

/**
 * 경매 상세화면의 이미지 슬라이더를 위한 ViewPager2 어댑터
 */
class AuctionImageAdapter(imageUrls: List<String> = emptyList()) : RecyclerView.Adapter<AuctionImageAdapter.ImageViewHolder>() {
    
    // 이미지 URL 목록
    private val images: MutableList<String> = imageUrls.toMutableList()
    
    // 단일 이미지 URL인 경우 이 생성자 사용
    constructor(imageUrl: String) : this(listOf(imageUrl))

    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_auction_image, parent, false)
        return ImageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = images[position]
        holder.bind(imageUrl)
    }
    
    override fun getItemCount(): Int = images.size
    
    fun updateImages(newImages: List<String>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }
    
    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.auctionImageView)
        
        fun bind(imageUrl: String) {
            Glide.with(imageView.context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)
        }
    }
}
