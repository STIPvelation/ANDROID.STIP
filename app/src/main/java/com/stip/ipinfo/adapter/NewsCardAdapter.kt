package com.stip.stip.ipinfo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stip.stip.R
import com.stip.stip.ipinfo.model.NewsItem

class NewsCardAdapter(
    private val newsList: List<NewsItem>,
    private val onItemClick: (NewsItem) -> Unit
) : RecyclerView.Adapter<NewsCardAdapter.NewsViewHolder>() {

    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val newsTitle: TextView = itemView.findViewById(R.id.news_title)
        val newsDate: TextView = itemView.findViewById(R.id.news_date)
        val newsImage: ImageView = itemView.findViewById(R.id.news_image)

        fun bind(newsItem: NewsItem) {
            newsTitle.text = newsItem.title
            newsDate.text = newsItem.pubDate
            
            // 뉴스 제목의 해시코드에 따라 두 가지 로고 이미지 중 하나를 선택
            val isEven = Math.abs(newsItem.title.hashCode()) % 2 == 0
            
            // 짝수/홀수 해시코드에 따라 다른 로고 이미지 사용
            val logoResource = if (isEven) {
                R.drawable.stiplogoblue
            } else {
                R.drawable.stiplogo
            }
            
            // 이미지 설정
            newsImage.setImageResource(logoResource)

            // 클릭 리스너 설정
            itemView.setOnClickListener {
                onItemClick(newsItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_news_item_card, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(newsList[position])
    }

    override fun getItemCount() = newsList.size
}
