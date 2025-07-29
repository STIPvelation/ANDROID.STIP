package com.stip.stip.signup

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.text.InputFilter
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.Group
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun Context.getColorEx(resourceId: Int): Int {
    return androidx.core.content.ContextCompat.getColor(this, resourceId)
}

fun Date.dateToString(format: String, local : Locale = Locale.getDefault()): String {
    val formatter = SimpleDateFormat(format, local)
    return formatter.format(this)
}

fun Long.dDayCalculator(): Long {
    val endTime: Long = Calendar.getInstance().apply {
        timeInMillis = this@dDayCalculator

        set(Calendar.HOUR_OF_DAY,0)
        set(Calendar.MINUTE,0)
        set(Calendar.SECOND,0)
        set(Calendar.MILLISECOND,0)
    }.timeInMillis

    val currentTime: Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY,0)
        set(Calendar.MINUTE,0)
        set(Calendar.SECOND,0)
        set(Calendar.MILLISECOND,0)
    }.timeInMillis

    return (endTime - currentTime) / (24 * 60 * 60 * 1000)
}

fun Long.isParticipation(): Boolean {
    val difference = this.dDayCalculator().toInt()
    return difference >= 0
}

fun AppCompatEditText.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun AppCompatEditText.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(this.windowToken, 0)
}

fun Group.setAllOnClickListener(listener: (View) -> Unit) {
    referencedIds.forEach { id ->
        rootView.findViewById<View>(id).setOnClickListener(listener)
    }
}

fun RecyclerView.addOnScrollTopListener(isTopCallback: (Boolean) -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

            if (!recyclerView.canScrollVertically(-1)) {
                // direction -> -1 이면 최상단 1이면 최하단
                isTopCallback.invoke(true)
            } else {
                isTopCallback.invoke(false)
            }
        }
    })
}

fun NestedScrollView.addOnScrollTopListener(isTopCallback: (Boolean) -> Unit) {
    this.viewTreeObserver.addOnScrollChangedListener {
        if (this.scrollY == 0) {
            isTopCallback.invoke(true)
        } else {
            isTopCallback.invoke(false)
        }
    }
}

fun String.getLines(): Int {
    return this.lines().size
}

/*fun EditText.textListenerChangeBackground(context: Context) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            if (s.isNullOrBlank()){
                this@textListenerChangeBackground.background = ContextCompat.getDrawable(context, R.drawable.round_rect_edit_text_off_bg)
            } else {
                this@textListenerChangeBackground.background = ContextCompat.getDrawable(context, R.drawable.round_rect_edit_text_on_bg)
            }
        }
    })
}

fun EditText.textListenerReviewChangeBackground(context: Context) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            if (s.isNullOrBlank()){
                this@textListenerReviewChangeBackground.background = ContextCompat.getDrawable(context, R.drawable.round_rect_review_edit_text_off_bg)
            } else {
                this@textListenerReviewChangeBackground.background = ContextCompat.getDrawable(context, R.drawable.round_rect_review_edit_text_on_bg)
            }
        }
    })
}

fun EditText.isEnableChangeBackground(context: Context) {
    if (this.isEnabled) {
        this.background = ContextCompat.getDrawable(context, R.drawable.round_rect_edit_text_on_bg)
    } else {
        this.background = ContextCompat.getDrawable(context, R.drawable.round_rect_edit_text_off_bg)
    }
}*/

fun AppCompatEditText.clear() {
    this.text?.clear()
}

fun AppCompatEditText.getTextString(): String = this.text.toString().trim()

fun AppCompatEditText.focusCallback(callback: (Boolean) -> Unit) {
    this.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        callback.invoke(hasFocus)
    }
}

fun <T, K, R> LiveData<T>.combineWith(
    liveData: LiveData<K>,
    block: (T?, K?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block(this.value, liveData.value)
    }
    result.addSource(liveData) {
        result.value = block(this.value, liveData.value)
    }
    return result
}

fun AppCompatEditText.setNoSpaceEnter() {
    val noSpaceFilter = InputFilter { source, start, end, dest, dstart, dend ->
        for (i in start until end) {
            if (Character.isWhitespace(source[i])) {
                return@InputFilter ""
            }
        }
        null
    }

    this.filters += noSpaceFilter
}

fun View.keyboardEventListener(view :View, callback: (Boolean) -> Unit){
    var lastStateOpen = false
    val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val r = Rect()
        view.getWindowVisibleDisplayFrame(r)
        val screenHeight = view.rootView.height
        val keypadHeight = screenHeight - r.bottom
        val isOpen = keypadHeight > screenHeight * 0.15
        if(isOpen != lastStateOpen){
            lastStateOpen = isOpen
            callback(isOpen)
        }
    }

    this.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
}

fun LifecycleOwner.repeatOnStarted(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED, block)
    }
}

fun Context.getFragmentActivity(): androidx.fragment.app.FragmentActivity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is androidx.fragment.app.FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}