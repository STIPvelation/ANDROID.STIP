package com.stip.stip.ipinfo.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.stip.stip.MainActivity
import com.stip.stip.R
import com.stip.stip.ScrollableToTop
import com.stip.stip.databinding.FragmentIpNewsBinding
// DipListingInfoFragment import removed
// DipNewsAdapter import removed
import com.stip.stip.ipinfo.adapter.NewsCardAdapter
// NewsPageAdapter import removed
// DipNewsItem import removed
import com.stip.stip.ipinfo.model.NewsItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class IpNewsFragment : Fragment(), ScrollableToTop {

    private lateinit var mainViewModel: com.stip.stip.MainViewModel

    private var _binding: FragmentIpNewsBinding? = null
    private val binding get() = _binding!!

    // Removed newsPageAdapter as viewpager was removed
    private var timeUpdateJob: Job? = null
    private var currentBreakingNews: NewsItem? = null
    private val switchHandler = Handler(Looper.getMainLooper())
    private var currentNewsIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIpNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.stip.stip.MainViewModel::class.java]
        mainViewModel.memberInfo.observe(viewLifecycleOwner) { memberInfo ->
            // TODO: 회원정보를 UI에 반영하는 코드 작성
        }

        super.onViewCreated(view, savedInstanceState)

        // Viewpager and indicators removed from layout

        binding.textSingleNewsSwiper.isSelected = true
        binding.textSingleNewsSwiper.setOnClickListener {
            currentBreakingNews?.let { openLink(it.link) }
        }

        // DIP LISTING feature removed

        // NewsRepository에서 미리 캐싱된 뉴스가 있으면 바로 표시, 아니면 비동기로 로딩
        val cached = com.stip.stip.ipinfo.NewsRepository.getCachedNews()
        if (cached != null) {
            displayNews(cached)
        } else {
            lifecycleScope.launch {
                val news = com.stip.stip.ipinfo.NewsRepository.getNews()
                displayNews(news)
            }
        }

        // DIP LISTING feature removed
        startAutoUpdateTime()
    }

    private val switchRunnable = object : Runnable {
        override fun run() {
            val currentList = com.stip.stip.ipinfo.NewsRepository.getCachedNews() ?: return
            if (currentList.isNotEmpty()) {
                currentBreakingNews = currentList[currentNewsIndex]
                binding.textSingleNewsSwiper.text = currentBreakingNews?.title ?: ""
                currentNewsIndex = (currentNewsIndex + 1) % currentList.size
            }
            switchHandler.postDelayed(this, 4000)
        }
    }

    // Removed updateDotIndicator as dot indicators were removed from layout

    private fun displayNews(newsList: List<NewsItem>) {
        // NewsPages for viewpager removed from layout

        currentBreakingNews = newsList.firstOrNull()
        binding.textSingleNewsSwiper.text = currentBreakingNews?.title ?: ""
        switchHandler.removeCallbacks(switchRunnable)
        switchHandler.post(switchRunnable)
        
        // Setup news card items
        binding.recyclerViewNewsItems.apply {
            adapter = NewsCardAdapter(newsList) { newsItem ->
                // Handle click on news item
                openLink(newsItem.link)
            }
        }
    }

    private fun openLink(link: String?) {
        if (link == null) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.error_open_link_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun parsePubDate(pubDate: String?): Long? {
        if (pubDate.isNullOrBlank()) return null
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (format in formats) {
            try {
                val dateFormat = SimpleDateFormat(format, Locale.ENGLISH).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                return dateFormat.parse(pubDate)?.time
            } catch (_: Exception) {}
        }
        return null
    }

    private fun calculateRelativeTime(pubDate: String?): String {
        val publishedTime = parsePubDate(pubDate) ?: return getString(R.string.time_just_now)
        val now = System.currentTimeMillis()
        val diffMillis = now - publishedTime
        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)

        val pubCal = Calendar.getInstance().apply { timeInMillis = publishedTime }
        val nowCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DATE, -1) }

        return when {
            diffMinutes < 1 -> getString(R.string.time_just_now)
            diffMinutes < 60 -> getString(R.string.time_minutes_ago, diffMinutes.toInt())
            isSameDay(pubCal, nowCal) -> getString(R.string.time_hours_ago, diffHours.toInt())
            isSameDay(pubCal, yesterdayCal) -> getString(R.string.time_yesterday, formatTime(pubCal))
            else -> formatTime(pubCal)
        }
    }

    private fun formatTime(cal: Calendar): String {
        val dateFormat = SimpleDateFormat("a h:mm", Locale.KOREAN).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
        }
        return dateFormat.format(cal.time)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun startAutoUpdateTime() {
        timeUpdateJob?.cancel()
        timeUpdateJob = lifecycleScope.launch {
            while (isActive) {
                delay(60_000)
                com.stip.stip.ipinfo.NewsRepository.getCachedNews()?.let { original ->
                    val updated = original.map {
                        it.copy(date = calculateRelativeTime(it.pubDate))
                    }
                    displayNews(updated)
                }
            }
        }
    }

    override fun scrollToTop() {
        _binding?.scrollView?.smoothScrollTo(0, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        timeUpdateJob?.cancel()
        switchHandler.removeCallbacks(switchRunnable)
    }

    companion object {
        @JvmStatic
        fun newInstance(): IpNewsFragment = IpNewsFragment()
    }
}