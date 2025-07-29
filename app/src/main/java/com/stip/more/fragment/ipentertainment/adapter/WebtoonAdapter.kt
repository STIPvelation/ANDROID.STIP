package com.stip.stip.more.fragment.ipentertainment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stip.stip.R
import com.stip.stip.more.fragment.ipentertainment.model.WebtoonModel

class WebtoonAdapter : ListAdapter<WebtoonModel, WebtoonAdapter.WebtoonViewHolder>(WebtoonDiffCallback()) {

    private var onItemClickListener: ((WebtoonModel) -> Unit)? = null

    fun setOnItemClickListener(listener: (WebtoonModel) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebtoonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_webtoon, parent, false)
        return WebtoonViewHolder(view)
    }

    override fun onBindViewHolder(holder: WebtoonViewHolder, position: Int) {
        val webtoon = getItem(position)
        holder.bind(webtoon)
    }

    inner class WebtoonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageWebtoon: ImageView = itemView.findViewById(R.id.imageWebtoon)
        private val textWebtoonTitle: TextView = itemView.findViewById(R.id.textWebtoonTitle)
        private val textWebtoonAuthor: TextView = itemView.findViewById(R.id.textWebtoonAuthor)
        private val starIcon: ImageView = itemView.findViewById(R.id.starIcon)
        private val textRatingValue: TextView = itemView.findViewById(R.id.textRatingValue)
        private val badgeNew: TextView = itemView.findViewById(R.id.badgeNew)
        private val badgeUpdated: TextView = itemView.findViewById(R.id.badgeUpdated)
        private val badgeCompleted: TextView = itemView.findViewById(R.id.badgeCompleted)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(getItem(position))
                }
            }
        }

        fun bind(webtoon: WebtoonModel) {
            // 새로운 타이틀 형식 설정 - 단순하게 제목만 표시
            textWebtoonTitle.text = webtoon.title
            textWebtoonAuthor.text = webtoon.author
            
            // 평점 표시
            textRatingValue.text = String.format("%.1f", webtoon.rating)
            
            // 이미지 로딩 (Glide 사용)
            Glide.with(itemView.context)
                .load(webtoon.imageUrl)
                .placeholder(R.drawable.ic_ipentertainment_toon)
                .error(R.drawable.ic_ipentertainment_toon)
                .into(imageWebtoon)
            
            // 뱃지 표시 설정
            badgeNew.visibility = if (webtoon.isNew) View.VISIBLE else View.GONE
            badgeUpdated.visibility = if (webtoon.isUpdated) View.VISIBLE else View.GONE
            badgeCompleted.visibility = if (webtoon.isCompleted) View.VISIBLE else View.GONE
        }
    }

    class WebtoonDiffCallback : DiffUtil.ItemCallback<WebtoonModel>() {
        override fun areItemsTheSame(oldItem: WebtoonModel, newItem: WebtoonModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WebtoonModel, newItem: WebtoonModel): Boolean {
            return oldItem == newItem
        }
    }
}
