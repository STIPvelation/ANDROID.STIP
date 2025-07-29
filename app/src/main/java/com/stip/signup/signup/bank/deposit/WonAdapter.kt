package com.stip.stip.signup.signup.bank.deposit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.signup.base.BaseViewHolder
import com.stip.stip.databinding.ItemRv1WonBinding

class WonAdapter(
    private var itemList: MutableList<String> = MutableList(6) { "" },
    private val itemClick: (() -> Unit)
): RecyclerView.Adapter<BaseViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val binding = ItemRv1WonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WonViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class WonViewHolder(private var binding: ItemRv1WonBinding): BaseViewHolder(binding.root) {
        override fun bind(position: Int) {
            val itemData = itemList[position]

            binding.tvNumber.text = itemData
            binding.root.setOnClickListener {
                itemClick.invoke()
            }
        }
    }

    fun updateNumber(number: String) {
        itemList = number.padEnd(6, ' ').map { it.toString() }.toMutableList()
        notifyDataSetChanged()
    }
}