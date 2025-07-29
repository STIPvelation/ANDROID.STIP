package com.stip.stip.ipinfo.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.stip.stip.ipinfo.fragment.NewsFragment
import com.stip.stip.ipinfo.model.NewsItem

class NewsPageAdapter(
    fragment: Fragment,
    private val onNewsClick: (NewsItem) -> Unit
) : FragmentStateAdapter(fragment) {

    private var pages: List<List<NewsItem>> = emptyList()

    fun submitList(newsPages: List<List<NewsItem>>) {
        pages = newsPages
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment {
        return NewsFragment.Companion.newInstance(pages[position]).apply {
            setOnNewsClickListener(onNewsClick)
        }
    }
}