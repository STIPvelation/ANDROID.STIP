package com.stip.stip.more.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.more.adapter.SectorFilterAdapter
import com.stip.stip.more.util.GridSpacingItemDecoration

class SectorFilterDialog(
    context: Context,
    private val sectors: List<String>,
    private val initialSelection: String? = null,
    private val onSectorSelected: (String) -> Unit
) : Dialog(context) {

    private lateinit var adapter: SectorFilterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_sector_filter)

        // Set dialog window attributes
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            attributes?.windowAnimations = R.style.DialogAnimation
        }

        // Setup RecyclerView with GridLayout (2 columns) for a modern look
        val recyclerView = findViewById<RecyclerView>(R.id.rvSectors)
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 2)
        recyclerView.addItemDecoration(GridSpacingItemDecoration(2, 8, true))
        adapter = SectorFilterAdapter(sectors) { }
        recyclerView.adapter = adapter

        // Select initial sector if provided
        initialSelection?.let {
            adapter.setSelectedSector(it)
        }

        // Setup search functionality
        val searchEditText = findViewById<EditText>(R.id.etSearch)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter.filter(s.toString())
            }
        })

        // Setup apply button
        val applyButton = findViewById<Button>(R.id.btnApply)
        applyButton.setOnClickListener {
            val selectedSector = adapter.getSelectedSector()
            if (selectedSector != null) {
                onSectorSelected(selectedSector)
                dismiss()
            }
        }
    }
}
