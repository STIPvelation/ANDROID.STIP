package com.stip.stip.custom

import android.content.Context
import android.util.AttributeSet
import android.widget.HorizontalScrollView
import java.lang.ref.WeakReference

class SyncScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : HorizontalScrollView(context, attrs) {

    private var isSyncing = false

    // 동기화 타겟 리스트 (메모리 누수 방지를 위해 WeakReference 사용)
    private val syncTargets = mutableSetOf<WeakReference<SyncScrollView>>()

    /**
     * 동기화 타겟을 연결
     */
    fun bindSyncTarget(target: SyncScrollView) {
        if (target != this && syncTargets.none { it.get() == target }) {
            syncTargets.add(WeakReference(target))
            target.bindSyncTarget(this) // 쌍방 연결
        }
    }

    /**
     * 동기화 해제
     */
    fun removeSyncTarget(target: SyncScrollView) {
        syncTargets.removeAll { it.get() == target || it.get() == null }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)

        if (!isSyncing) {
            isSyncing = true
            syncTargets.forEach { ref ->
                ref.get()?.let { it.syncScrollTo(l) }
            }
            isSyncing = false
        }
    }

    /**
     * 외부에서 호출 시 스크롤 위치 강제 이동
     */
    fun syncScrollTo(x: Int) {
        if (!isSyncing) {
            isSyncing = true
            scrollTo(x, 0)
            isSyncing = false
        }
    }
}
