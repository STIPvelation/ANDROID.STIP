package com.stip.stip.signup.signup.kyc.inform

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.databinding.ItemRvKycInformBinding
import com.stip.stip.signup.base.BaseViewHolder

class SignUpKYCInformAdapter(
    private var itemList: List<String>,
    private val itemClick: ((String) -> Unit)
) : RecyclerView.Adapter<BaseViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val binding = ItemRvKycInformBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KYCInformViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class KYCInformViewHolder(private var binding: ItemRvKycInformBinding) : BaseViewHolder(binding.root) {
        override fun bind(position: Int) {
            val itemData = itemList[position]

            binding.tvKycInformSelect.text = itemData
            binding.root.setOnClickListener {
                itemClick.invoke(itemData)
            }
        }
    }
}