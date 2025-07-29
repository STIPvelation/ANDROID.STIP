package com.stip.stip.ipinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.stip.stip.databinding.FragmentIpNewsBinding

class FragmentNewsPage : Fragment() {

    private var page: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            page = it.getInt(ARG_PAGE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentIpNewsBinding.inflate(inflater, container, false)
        // 페이지별 RecyclerView 구성
        return binding.root
    }

    companion object {
        private const val ARG_PAGE = "page_number"

        @JvmStatic
        fun newInstance(page: Int) =
            FragmentNewsPage().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PAGE, page)
                }
            }
    }
}