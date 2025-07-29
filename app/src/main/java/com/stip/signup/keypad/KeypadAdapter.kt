package com.stip.stip.signup.keypad

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.signup.base.BaseViewHolder
import com.stip.stip.databinding.ItemRvKeypadBinding

class KeypadAdapter(
    private var itemList: MutableList<KeypadItem>,
    private val onItemClick: (KeypadItem) -> Unit
): RecyclerView.Adapter<BaseViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val binding = ItemRvKeypadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KeyPadViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class KeyPadViewHolder(private var binding: ItemRvKeypadBinding): BaseViewHolder(binding.root) {
        override fun bind(position: Int) {
            val itemData = itemList[position]

            binding.clKeypadDelContainer.visibility = View.GONE
            binding.clKeypadNumberContainer.visibility = View.GONE
            binding.clKeypadDoneContainer.visibility = View.GONE
            
            when(itemData.type) {
                KeypadType.NUMBER -> {
                    binding.clKeypadNumberContainer.visibility = View.VISIBLE
                    binding.tvKeypadNumber.text = itemData.value
                    binding.tvKeypadNumber.setTextColor(binding.root.context.getColor(android.R.color.black))
                }
                KeypadType.SHUFFLE -> {
                    // 재배열 버튼은 사용하지 않음
                    binding.clKeypadDoneContainer.visibility = View.VISIBLE
                }
                KeypadType.DELETE -> {
                    binding.clKeypadDelContainer.visibility = View.VISIBLE
                }
                KeypadType.DONE -> {
                    // 완료 버튼도 숫자 버튼과 동일한 원형 스타일로 표시
                    binding.clKeypadNumberContainer.visibility = View.VISIBLE
                    binding.tvKeypadNumber.text = itemData.value
                    binding.tvKeypadNumber.setTextColor(binding.root.context.getColor(android.R.color.black))
                }
            }

            binding.root.setOnClickListener {
                onItemClick.invoke(itemData)
            }
        }
    }

    fun setupWithDoneButton() {
        // 1부터 9까지의 숫자 아이템 생성
        val numberItems = (1..9).map { KeypadItem(it.toString(), KeypadType.NUMBER) }
        
        // DELETE, 0, DONE 순서로 아이템 생성
        val bottomRowItems = listOf(
            KeypadItem("", KeypadType.DELETE, R.drawable.ic_del_white_31dp),
            KeypadItem("0", KeypadType.NUMBER),
            KeypadItem("완료", KeypadType.DONE)
        )

        // 1-9 숫자 + (DELETE, 0, DONE) 순서로 배치
        itemList = (numberItems + bottomRowItems).toMutableList()
        notifyDataSetChanged()
    }
}