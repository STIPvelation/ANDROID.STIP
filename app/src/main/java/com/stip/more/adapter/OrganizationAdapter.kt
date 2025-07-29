package com.stip.stip.more.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R

class OrganizationAdapter(
    private val organizations: List<String>
) : RecyclerView.Adapter<OrganizationAdapter.ViewHolder>() {

    private var selectedPosition = -1

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrganizationName: TextView = itemView.findViewById(R.id.tvOrganizationName)
        val radioButton: RadioButton = itemView.findViewById(R.id.radioSelected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_organization, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvOrganizationName.text = organizations[position]
        holder.radioButton.isChecked = position == selectedPosition

        holder.itemView.setOnClickListener {
            selectPosition(position)
        }

        holder.radioButton.setOnClickListener {
            selectPosition(position)
        }
    }

    override fun getItemCount() = organizations.size

    private fun selectPosition(position: Int) {
        val previousSelected = selectedPosition
        selectedPosition = position
        
        // 이전 선택을 업데이트
        if (previousSelected != -1) {
            notifyItemChanged(previousSelected)
        }
        
        // 새로운 선택을 업데이트
        notifyItemChanged(position)
    }
    
    fun getSelectedOrganization(): String? {
        return if (selectedPosition != -1) organizations[selectedPosition] else null
    }
}
