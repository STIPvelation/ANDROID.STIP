package com.stip.stip.iptransaction.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.DialogFragment
import com.stip.stip.R
import kotlinx.coroutines.launch

/**
 * 티커 선택을 위한 다이얼로그 프래그먼트
 */
class TickerSelectionDialogFragment : DialogFragment() {

    // 선택된 티커를 전달하기 위한 콜백 인터페이스
    interface TickerSelectionListener {
        fun onTickerSelected(ticker: String)
    }

    private var listener: TickerSelectionListener? = null

    // API에서 가져온 티커 목록을 저장할 변수
    private var tickersList = mutableListOf<String>()
    
    companion object {

        fun newInstance(listener: TickerSelectionListener): TickerSelectionDialogFragment {
            return TickerSelectionDialogFragment().apply {
                this.listener = listener
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_ticker_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 다이얼로그 크기 설정
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // API에서 티커 목록 가져오기
        loadTickersFromApi(view)
    }
    
    /**
     * API에서 티커 목록을 가져와서 리스트뷰에 설정
     */
    private fun loadTickersFromApi(view: View) {
        val listView = view.findViewById<ListView>(R.id.listView_tickers)
        
        // 로딩 중 표시
        tickersList.clear()
        tickersList.add("전체") // "전체" 옵션은 항상 첫 번째에 추가
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, tickersList)
        listView.adapter = adapter
        
        // API 호출
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val marketPairsService = com.stip.stip.api.RetrofitClient.createTapiService(
                    com.stip.stip.api.service.MarketPairsService::class.java
                )
                val response = marketPairsService.getMarketPairs()
                
                // UI 스레드에서 결과 처리
                requireActivity().runOnUiThread {
                    // API 응답에서 baseAsset(티커) 추출
                    val apiTickers = response.map { it.baseAsset }.sorted()
                    tickersList.clear()
                    tickersList.add("전체") // "전체" 옵션은 항상 첫 번째에 추가
                    tickersList.addAll(apiTickers)
                    
                    // 어댑터 업데이트
                    adapter.notifyDataSetChanged()
                    
                    // 아이템 클릭 리스너 설정
                    listView.setOnItemClickListener { _, _, position, _ ->
                        val selectedTicker = tickersList[position]
                        listener?.onTickerSelected(selectedTicker)
                        dismiss()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TickerSelectionDialog", "API 호출 실패: ${e.message}", e)
                // API 호출 실패 시 기본 티커 목록 사용
                requireActivity().runOnUiThread {
                    val defaultTickers = listOf(
                        "전체", "AXNO", "CDM", "IJECT", "JWV", "KATV", "KCOT", 
                        "MDM", "MSK", "SLEEP", "SMT", "WETALK"
                    )
                    tickersList.clear()
                    tickersList.addAll(defaultTickers)
                    adapter.notifyDataSetChanged()
                    
                    // 아이템 클릭 리스너 설정
                    listView.setOnItemClickListener { _, _, position, _ ->
                        val selectedTicker = tickersList[position]
                        listener?.onTickerSelected(selectedTicker)
                        dismiss()
                    }
                }
            }
        }
    }
}
