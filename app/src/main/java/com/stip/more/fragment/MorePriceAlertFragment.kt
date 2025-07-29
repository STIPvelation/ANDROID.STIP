package com.stip.stip.more.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.stip.stip.databinding.FragmentMorePriceAlertBinding
import com.stip.stip.MainViewModel
import com.stip.stip.R

class MorePriceAlertFragment : Fragment() {

    private lateinit var binding: FragmentMorePriceAlertBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMorePriceAlertBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ 헤더 타이틀 및 뒤로가기 설정
        viewModel.updateHeaderTitle(getString(R.string.title_price_alert))
        viewModel.enableBackNavigation {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // ✅ 탭 + ViewPager2 설정
        val fragments = listOf(
            StandardAlertFragment(),
            MarketPriceAlertFragment()
        )
        val titles = listOf(
            getString(R.string.tab_standard),
            getString(R.string.tab_market)
        )


        binding.viewPager.adapter = object : FragmentStateAdapter(requireActivity()) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }
}
