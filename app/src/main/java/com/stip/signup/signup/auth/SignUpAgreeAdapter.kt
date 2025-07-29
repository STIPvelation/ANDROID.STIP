package com.stip.stip.signup.signup.auth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.signup.base.BaseViewHolder
import com.stip.stip.databinding.ItemRvAgreeBinding
import com.stip.stip.signup.model.SignUpAgreeData

class SignUpAgreeAdapter(
    private val itemList: MutableList<SignUpAgreeData>,
    private val onItemClick: (MutableList<SignUpAgreeData>) -> Unit,
    private val contentClick: ((SignUpAgreeData) -> Unit)
): RecyclerView.Adapter<BaseViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val binding = ItemRvAgreeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AgreeViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class AgreeViewHolder(private var binding: ItemRvAgreeBinding): BaseViewHolder(binding.root) {
        override fun bind(position: Int) {
            val itemData = itemList[position]

            binding.tvSignUpAgree.text = itemData.title
            binding.ivSignUpAgree.isChecked = itemData.isCheck
            binding.ivSignUpAgree.setOnClickListener {
                itemData.isCheck = !itemData.isCheck
                onItemClick.invoke(itemList)
            }
            binding.ivSignUpAgreeTerm.setOnClickListener {
                contentClick.invoke(itemData)
            }
        }
    }

    fun setAllAgreeClick(isCheck: Boolean) {
        itemList.forEach {
            it.isCheck = isCheck
        }
        notifyDataSetChanged()
    }

}