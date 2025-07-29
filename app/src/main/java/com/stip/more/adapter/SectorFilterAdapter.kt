package com.stip.stip.more.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import java.util.*

class SectorFilterAdapter(
    private val items: List<String>,
    private val onItemSelectedListener: (String) -> Unit
) : RecyclerView.Adapter<SectorFilterAdapter.SectorViewHolder>(), Filterable {

    private var selectedPosition = -1
    private var filteredItems: List<String> = items
    private var selectedSector: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sector_filter, parent, false)
        return SectorViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectorViewHolder, position: Int) {
        val sector = filteredItems[position]
        val isSelected = sector == selectedSector
        holder.bind(sector, isSelected)

        holder.itemView.setOnClickListener {
            selectedSector = sector
            notifyDataSetChanged()
            onItemSelectedListener(sector)
        }
    }

    override fun getItemCount(): Int = filteredItems.size

    fun setSelectedSector(sector: String) {
        if (items.contains(sector)) {
            selectedSector = sector
            notifyDataSetChanged()
        }
    }

    fun getSelectedSector(): String? = selectedSector

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase(Locale.ROOT) ?: ""
                val filtered = if (query.isEmpty()) {
                    items
                } else {
                    items.filter { it.lowercase(Locale.ROOT).contains(query) }
                }

                return FilterResults().apply {
                    values = filtered
                    count = filtered.size
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = results?.values as? List<String> ?: items
                notifyDataSetChanged()
            }
        }
    }

    inner class SectorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardSector)
        private val tvSectorName: TextView = itemView.findViewById(R.id.tvSectorName)
        private val checkmark: View = itemView.findViewById(R.id.ivCheckmark)

        fun bind(sector: String, isSelected: Boolean) {
            tvSectorName.text = sector

            if (isSelected) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.blue_50))
                cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.blue_200)
                tvSectorName.setTextColor(ContextCompat.getColor(itemView.context, R.color.blue_500))
                checkmark.visibility = View.VISIBLE
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.gray_200)
                tvSectorName.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray_600))
                checkmark.visibility = View.GONE
            }
        }
    }
}
