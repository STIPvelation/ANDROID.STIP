package com.stip.stip.signup.main.investment

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.stip.stip.R

class InvestmentHistoryFragment : Fragment() {

    companion object {
        fun newInstance() = InvestmentHistoryFragment()
    }

    private val viewModel: InvestmentHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_investment_history, container, false)
    }
}