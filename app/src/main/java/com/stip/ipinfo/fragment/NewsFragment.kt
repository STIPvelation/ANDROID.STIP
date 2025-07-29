package com.stip.stip.ipinfo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.stip.stip.databinding.FragmentNewsPageBinding
import com.stip.stip.ipinfo.adapter.NewsListAdapter
import com.stip.stip.ipinfo.model.NewsItem

class NewsFragment : Fragment() {

    private lateinit var mainViewModel: com.stip.stip.MainViewModel
    private var newsList: List<NewsItem> = emptyList()
    private var _binding: FragmentNewsPageBinding? = null
    private val binding get() = _binding!!
    private var onNewsClick: ((NewsItem) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            newsList = BundleCompat.getParcelableArrayList(it, "newsList", NewsItem::class.java) ?: emptyList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mainViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.stip.stip.MainViewModel::class.java]
        _binding = FragmentNewsPageBinding.inflate(inflater, container, false)

        val adapter = NewsListAdapter(newsList) { item ->
            onNewsClick?.invoke(item)
        }

        binding.recyclerNewsPage.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerNewsPage.adapter = adapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel.memberInfo.observe(viewLifecycleOwner) { memberInfo ->
            // TODO: 회원정보를 UI에 반영하는 코드 작성
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setOnNewsClickListener(listener: (NewsItem) -> Unit) {
        this.onNewsClick = listener
    }

    companion object {
        fun newInstance(newsList: List<NewsItem>): NewsFragment {
            val fragment = NewsFragment()
            fragment.arguments = Bundle().apply {
                putParcelableArrayList("newsList", ArrayList(newsList))
            }
            return fragment
        }
    }
}