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
import com.stip.stip.more.fragment.ipentertainment.model.SwapModel

class SwapAdapter : ListAdapter<SwapModel, SwapAdapter.SwapViewHolder>(SwapDiffCallback()) {

    private var onItemClickListener: ((SwapModel) -> Unit)? = null

    fun setOnItemClickListener(listener: (SwapModel) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwapViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ip_swap, parent, false)
        return SwapViewHolder(view)
    }

    override fun onBindViewHolder(holder: SwapViewHolder, position: Int) {
        val swap = getItem(position)
        holder.bind(swap)
    }

    inner class SwapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageIp: ImageView = itemView.findViewById(R.id.imageIp)
        private val textIpDescription: TextView = itemView.findViewById(R.id.textIpDescription)
        private val textIpNumber: TextView = itemView.findViewById(R.id.textIpNumber)
        private val textRemainingPeriod: TextView = itemView.findViewById(R.id.textRemainingPeriod)
        private val textSwapTarget: TextView = itemView.findViewById(R.id.textSwapTarget)
        private val textSwapAvailable: TextView = itemView.findViewById(R.id.textSwapAvailable)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(getItem(position))
                }
            }
        }

        fun bind(swap: SwapModel) {
            textIpDescription.text = swap.title
            textIpNumber.text = "IP번호: US-${swap.id.padStart(2, '0')}-${(1000000..9999999).random()}"
            textSwapTarget.text = swap.description
            textRemainingPeriod.text = "${(3..10).random()}년"
            
            // Display "스왑 가능" badge for all items
            textSwapAvailable.visibility = View.VISIBLE
            
            // 이미지 로딩
            Glide.with(itemView.context)
                .load(swap.imageUrl)
                .placeholder(R.drawable.ic_ipentertainment_swap)
                .error(R.drawable.ic_ipentertainment_swap)
                .into(imageIp)
        }
    }

    class SwapDiffCallback : DiffUtil.ItemCallback<SwapModel>() {
        override fun areItemsTheSame(oldItem: SwapModel, newItem: SwapModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SwapModel, newItem: SwapModel): Boolean {
            return oldItem == newItem
        }
    }
}
