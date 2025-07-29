package com.stip.ipasset.usd.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R

class NumericKeypadAdapter(private val onKeyPressed: (String) -> Unit) : 
    RecyclerView.Adapter<NumericKeypadAdapter.KeypadViewHolder>() {
    
    // 키패드 키 목록
    private val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "완료", "0", "<")
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeypadViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rv_keypad, parent, false)
        return KeypadViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: KeypadViewHolder, position: Int) {
        val key = keys[position]
        holder.bind(key)
    }
    
    override fun getItemCount() = keys.size
    
    inner class KeypadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val numberContainer = itemView.findViewById<ConstraintLayout>(R.id.cl_keypad_number_container)
        private val numberText = itemView.findViewById<TextView>(R.id.tv_keypad_number)
        private val delContainer = itemView.findViewById<ConstraintLayout>(R.id.cl_keypad_del_container)
        
        fun bind(key: String) {
            // 삭제 버튼이면
            if (key == "<") {
                numberContainer.visibility = View.GONE
                delContainer.visibility = View.VISIBLE
                delContainer.setOnClickListener { onKeyPressed(key) }
            } else {
                // 숫자나 소수점이면
                numberContainer.visibility = View.VISIBLE
                delContainer.visibility = View.GONE
                numberText.text = key
                numberContainer.setOnClickListener { onKeyPressed(key) }
            }
            
            // 전체 아이템 클릭 이벤트도 설정
            itemView.setOnClickListener { onKeyPressed(key) }
        }
    }
}
