package com.stip.stip.more.fragment.ipentertainment

import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.stip.more.fragment.UnderConstructionDialogFragment
import com.stip.stip.MainViewModel
import com.stip.stip.R
import com.stip.stip.databinding.FragmentMoreIpToonBinding
import com.stip.stip.more.fragment.ipentertainment.adapter.WebtoonAdapter
import com.stip.stip.more.fragment.ipentertainment.model.WebtoonModel

class IPToonFragment : Fragment() {
    private var _binding: FragmentMoreIpToonBinding? = null
    private val binding get() = _binding!!
    private val activityViewModel: MainViewModel by activityViewModels()
    private val TAG = "IPToonFragment_Debug"
    
    private lateinit var webtoonAdapter: WebtoonAdapter
    private var webtoonList = mutableListOf<WebtoonModel>()
    private var filteredWebtoonList = mutableListOf<WebtoonModel>()
    
    private var selectedCategory: String = "전체"
    private var activeTab: Int = 0
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Modern replacement for setHasOptionsMenu will be in onViewCreated
    }
    
    // 옵션 메뉴 처리는 시스템에 매기고 직접 네비게이션 리스너로만 처리
    // 해당 코드 제거 - setNavigationOnClickListener로 처리
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreIpToonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 액티비티의 기본 액션바를 숨김
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        
        // 준비중 다이얼로그 표시
        showUnderConstructionDialog()
        
        // Modern replacement for setHasOptionsMenu - using menu provider instead
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.toolbar_menu, menu)
            }
            
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false // Return true if you handle the item selection
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
        
        setupSearchBar()
        setupCategoryChips()
        setupTabLayout()
        setupRecyclerView()
        loadDummyData() // 실제 앱에서는 API 호출 등으로 대체
        filterAndUpdateWebtoons()
    }



    
    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString()
                binding.clearSearchButton.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                filterAndUpdateWebtoons()
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        binding.clearSearchButton.setOnClickListener {
            binding.searchEditText.text.clear()
        }
    }
    
    private fun setupCategoryChips() {
        // 초기 상태 설정 - 전체 카테고리 선택
        binding.chipAll.isChecked = true
        selectedCategory = "전체"
        
        // 카테고리 칩 선택 처리
        @Suppress("DEPRECATION")
        binding.categoriesChipGroup.setOnCheckedChangeListener { group, checkedId ->
            val category = when (checkedId) {
                R.id.chipAll -> "전체"
                R.id.chipAction -> "액션"
                R.id.chipRomance -> "로맨스"
                R.id.chipComedy -> "코미디"
                R.id.chipFantasy -> "판타지"
                else -> "전체"
            }
            
            // 선택된 카테고리 변경 및 필터링 적용
            selectedCategory = category
            Log.d(TAG, "카테고리 선택 변경: $selectedCategory")
            filterAndUpdateWebtoons()
        }
        
        // 칩 직접 클릭 처리 (Single Selection 모드가 제대로 작동하지 않을 경우를 위한 보완)
        val chips = listOf(binding.chipAll, binding.chipAction, binding.chipRomance, binding.chipComedy, binding.chipFantasy)
        chips.forEach { chip ->
            chip.setOnClickListener {
                // 모든 칩 선택 해제
                chips.forEach { it.isChecked = false }
                // 현재 칩 선택
                chip.isChecked = true
                
                // 해당 칩에 맞는 카테고리 선택
                val category = when (chip.id) {
                    R.id.chipAll -> "전체"
                    R.id.chipAction -> "액션"
                    R.id.chipRomance -> "로맨스"
                    R.id.chipComedy -> "코미디"
                    R.id.chipFantasy -> "판타지"
                    else -> "전체"
                }
                
                selectedCategory = category
                Log.d(TAG, "칩 직접 클릭: $selectedCategory 카테고리 선택")
                filterAndUpdateWebtoons()
            }
        }
    }
    
    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val newTabPosition = tab?.position ?: 0
                
                if (activeTab != newTabPosition) {
                    activeTab = newTabPosition
                    
                    // 탭 선택에 따른 표시 메시지
                    val tabName = when(activeTab) {
                        0 -> "인기"
                        1 -> "신작"
                        2 -> "완결"
                        else -> "인기"
                    }
                    
                    Log.d(TAG, "탭 선택: $tabName")
                    filterAndUpdateWebtoons()
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // 탭 재선택시 컨텐츠 새로고침 효과 추가 가능
                Toast.makeText(context, "콘텐츠 새로 불러오는 중...", Toast.LENGTH_SHORT).show()
                filterAndUpdateWebtoons()
            }
        })
    }
    
    private fun setupRecyclerView() {
        webtoonAdapter = WebtoonAdapter()
        
        // 아이템 간격을 위한 데코레이션 클래스
        class GridSpacingItemDecoration(private val spanCount: Int, private val spacing: Int) : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val position = parent.getChildAdapterPosition(view)
                
                // 한 줄에 2개씩 아이템 배치
                outRect.left = spacing / 2
                outRect.right = spacing / 2
                outRect.bottom = spacing
                
                // 첫 번째 줄에는 위쪽 여백 추가
                if (position < spanCount) {
                    outRect.top = spacing
                }
            }
        }
        
        // 리사이클러뷰 설정
        binding.webtoonsRecyclerView.apply {
            adapter = webtoonAdapter
            layoutManager = GridLayoutManager(context, 2) // 2열 그리드로 변경
            
            // 기존 데코레이션 제거 후 새로 추가
            if (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }
            
            val spacing = resources.getDimensionPixelSize(R.dimen.item_offset)
            addItemDecoration(GridSpacingItemDecoration(2, spacing))
        }
        
        webtoonAdapter.setOnItemClickListener { webtoon ->
            Toast.makeText(context, "${webtoon.title} 선택됨", Toast.LENGTH_SHORT).show()
            // TODO: 웹툰 상세 화면으로 이동
        }
    }
    
    private fun loadDummyData() {
        // 실제 앱에서는 API 호출로 데이터를 로드합니다
        webtoonList.clear()
        webtoonList.addAll(
            listOf(
                // 액션 카테고리
                WebtoonModel("1", "전설의 요원", "현도", 4.9f, "https://picsum.photos/300/400?random=1", isNew = false, isUpdated = true, isCompleted = false, category = "액션"),
                WebtoonModel("2", "광식의 전사", "현도", 2.8f, "https://picsum.photos/300/400?random=2", isNew = true, isUpdated = false, isCompleted = false, category = "액션"),
                WebtoonModel("3", "마법의 검사 [완결]", "현도", 4.7f, "https://picsum.photos/300/400?random=3", isNew = false, isUpdated = false, isCompleted = true, category = "액션"),
                
                // 로맨스 카테고리
                WebtoonModel("4", "봄의 연인", "현도", 4.9f, "https://picsum.photos/300/400?random=4", isNew = false, isUpdated = true, isCompleted = false, category = "로맨스"),
                WebtoonModel("5", "초일반의 연애", "현도", 4.7f, "https://picsum.photos/300/400?random=5", isNew = true, isUpdated = false, isCompleted = false, category = "로맨스"),
                WebtoonModel("6", "그대와의 추억 [완결]", "현도", 4.8f, "https://picsum.photos/300/400?random=6", isNew = false, isUpdated = false, isCompleted = true, category = "로맨스"),
                
                // 코미디 카테고리
                WebtoonModel("7", "회사의 웃긴 사뭐",  "현도", 4.8f, "https://picsum.photos/300/400?random=7", isNew = false, isUpdated = true, isCompleted = false, category = "코미디"),
                WebtoonModel("8", "우리동네 기색단", "현도", 1.6f, "https://picsum.photos/300/400?random=8", isNew = true, isUpdated = false, isCompleted = false, category = "코미디"),
                WebtoonModel("9", "하국유학생 [완결]", "현도", 4.7f, "https://picsum.photos/300/400?random=9", isNew = false, isUpdated = false, isCompleted = true, category = "코미디"),
                
                // 판타지 카테고리
                WebtoonModel("10", "마법학교의 아이들", "현도", 4.9f, "https://picsum.photos/300/400?random=10", isNew = false, isUpdated = true, isCompleted = false, category = "판타지"),
                WebtoonModel("11", "오막의 울림", "현도", 4.8f, "https://picsum.photos/300/400?random=11", isNew = true, isUpdated = false, isCompleted = false, category = "판타지"),
                WebtoonModel("12", "성찰의 노래 [완결]", "현도", 4.7f, "https://picsum.photos/300/400?random=12", isNew = false, isUpdated = false, isCompleted = true, category = "판타지"),
                
                // 추가 카테고리 - 추가 장르
                WebtoonModel("13", "영웅의 전쟁", "현도", 1.7f, "https://picsum.photos/300/400?random=13", isNew = false, isUpdated = false, isCompleted = false, category = "전쟁"),
                WebtoonModel("14", "공포의 집", "현도", 4.5f, "https://picsum.photos/300/400?random=14", isNew = false, isUpdated = false, isCompleted = false, category = "공포"),
                WebtoonModel("15", "추리 추적기", "현도", 4.8f, "https://picsum.photos/300/400?random=15", isNew = true, isUpdated = false, isCompleted = false, category = "추리"),
                
                // 인기작 추가 데이터 (높은 평점)
                WebtoonModel("16", "전설의 검사", "현도", 5.0f, "https://picsum.photos/300/400?random=16", isNew = false, isUpdated = true, isCompleted = false, category = "판타지"),
                WebtoonModel("17", "암흑의 존재", "현도", 4.9f, "https://picsum.photos/300/400?random=17", isNew = false, isUpdated = true, isCompleted = false, category = "공포"),
                WebtoonModel("18", "색경 하루", "현도", 4.9f, "https://picsum.photos/300/400?random=18", isNew = false, isUpdated = true, isCompleted = false, category = "일상"),
                
                // 신작 추가 데이터
                WebtoonModel("19", "기억상상", "현도", 4.5f, "https://picsum.photos/300/400?random=19", isNew = true, isUpdated = false, isCompleted = false, category = "판타지"),
                WebtoonModel("20", "영원한 소녀", "현도", 4.6f, "https://picsum.photos/300/400?random=20", isNew = true, isUpdated = false, isCompleted = false, category = "로맨스"),
                WebtoonModel("21", "특수전사", "현도", 3.7f, "https://picsum.photos/300/400?random=21", isNew = true, isUpdated = false, isCompleted = false, category = "액션"),
                
                // 완결작 추가 데이터
                WebtoonModel("22", "삼국지 [완결]", "현도", 2.8f, "https://picsum.photos/300/400?random=22", isNew = false, isUpdated = false, isCompleted = true, category = "역사"),
                WebtoonModel("23", "악마의 기사 [완결]", "현도", 1.6f, "https://picsum.photos/300/400?random=23", isNew = false, isUpdated = false, isCompleted = true, category = "판타지"),
                WebtoonModel("24", "스마트폰 소녀 [완결]", "현도", 4.7f, "https://picsum.photos/300/400?random=24", isNew = false, isUpdated = false, isCompleted = true, category = "일상")
            )
        )
    }
    
    private fun filterAndUpdateWebtoons() {
        Log.d(TAG, "필터링 시작 - 카테고리: $selectedCategory, 탭: $activeTab, 검색어: $searchQuery")
        filteredWebtoonList.clear()
        
        // 1단계: 총 웹툰 목록에서 필터링한 목록 생성
        val tempList = webtoonList.filter { webtoon ->
            // 검색어 필터링
            val matchesSearch = if (searchQuery.isNotEmpty()) {
                webtoon.title.contains(searchQuery, ignoreCase = true) || 
                webtoon.author.contains(searchQuery, ignoreCase = true)
            } else true
            
            // 카테고리 필터링 (전체가 아니면 해당 카테고리만 필터링)
            val matchesCategory = if (selectedCategory != "전체") {
                Log.d(TAG, "필터링 검사: ${webtoon.title} - 웹툰 카테고리: ${webtoon.category}, 선택된 카테고리: $selectedCategory")
                webtoon.category.equals(selectedCategory, ignoreCase = true)
            } else true
            
            // 탭 필터링 (탭에 따라 적절한 웹툰만 필터링, 조깴 완화)
            val matchesTab = when (activeTab) {
                0 -> true        // 인기탭: 모든 웹툰 표시 (하단에서 높은 평점순 정렬로 처리)
                1 -> webtoon.isNew                // 신작: 신작 플래그가 true인 것만 표시
                2 -> webtoon.isCompleted          // 완결: 완결 플래그가 true인 것만 표시
                else -> true
            }
            
            val result = matchesSearch && matchesCategory && matchesTab
            if (matchesCategory && matchesTab) {
                Log.d(TAG, "필터링 결과: ${webtoon.title} - 카테고리: ${webtoon.category} - 포함 여부: $result")
            }
            
            result
        }
        
        Log.d(TAG, "필터링 후 수량: ${tempList.size}")
        
        // 2단계: 탭에 따라 정렬
        val sortedList = when (activeTab) {
            0 -> tempList.sortedByDescending { it.rating }        // 인기탭: 평점 높은 순
            1 -> tempList.sortedWith(                            // 신작탭: 신작 우선 후 평점 순
                compareByDescending<WebtoonModel> { it.isNew }
                    .thenByDescending { it.rating }
            )
            2 -> tempList.sortedBy { it.title }                  // 완결탭: 제목 순
            else -> tempList
        }
        
        // 3단계: 결과를 리사이클러뷰에 표시
        filteredWebtoonList.clear() // 먼저 목록 비우고
        filteredWebtoonList.addAll(sortedList) // 새 결과 추가
        webtoonAdapter.submitList(null) // 리스트 초기화
        webtoonAdapter.submitList(ArrayList(filteredWebtoonList)) // 새 리스트로 업데이트
        
        // 필터링 결과 로그
        val countText = if (filteredWebtoonList.isEmpty()) {
            "현재 필터에 해당하는 웹툰이 없습니다."
        } else {
            "필터링 결과: 총 ${filteredWebtoonList.size}개의 웹툰"
        }
        
        Log.d(TAG, countText)
    }

    override fun onResume() {
        super.onResume()
        // 현재 프래그먼트가 보이는 경우 헤더 타이틀을 "IP웹툰"으로 업데이트
        activityViewModel.updateHeaderTitle("IP웹툰")
        
        // 헤더 왼쪽에 뒤로가기 버튼 표시 (전역 네비게이션 아이콘 설정)
        activityViewModel.updateNavigationIcon(R.drawable.ic_arrow_back)
        
        // 뒤로가기 버튼 클릭 리스너 설정 - MainViewModel을 통해 전역적으로 설정
        activityViewModel.updateNavigationClickListener {
            Log.d(TAG, "뒤로가기 버튼 클릭 - MainViewModel 해들러 호출")
            try {
                parentFragmentManager.popBackStack()
                Log.d(TAG, "성공적으로 뒤로가기 완료")
            } catch (e: Exception) {
                Log.e(TAG, "뒤로가기 오류: ${e.message}")
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
        
        // 프래그먼트 자체 툴바를 사용하므로 액티비티 툴바 숨김
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        
        Log.d(TAG, "onResume: 헤더 타이틀을 IP웹툰으로 설정, 뒤로가기 버튼 추가")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // 프래그먼트가 종료될 때 액션바 상태 복원
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }
    
    /**
     * 준비중 안내 다이얼로그 표시
     */
    private fun showUnderConstructionDialog() {
        val dialogFragment = UnderConstructionDialogFragment.newInstance(
            "준비중",
            "IP 툰 서비스는 현재 준비중입니다."
        )
        dialogFragment.show(childFragmentManager, "under_construction_dialog")
    }

    // Removed deprecated onCreateOptionsMenu - now using MenuProvider in onViewCreated
}
