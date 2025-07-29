package com.stip.stip.signup.signup.bank.select

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.signup.base.BaseViewHolder
import com.stip.stip.databinding.ItemRvBankBinding
import com.stip.stip.signup.model.BankData
import com.stip.stip.signup.utils.GlideUtils

class SignUpBankSelectAdapter(
    private val itemList: List<BankData>,
    private val itemClick: ((BankData) -> Unit)
): RecyclerView.Adapter<BaseViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val binding = ItemRvBankBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BankViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class BankViewHolder(private var binding: ItemRvBankBinding): BaseViewHolder(binding.root) {
        override fun bind(position: Int) {
            val itemData = itemList[position]

            GlideUtils.loadImage(binding.ivSignUpBank, itemData.icon)
            binding.tvSignUpBank.text = itemData.name
            binding.root.setOnClickListener {
                itemClick.invoke(itemData)
            }
        }
    }
}