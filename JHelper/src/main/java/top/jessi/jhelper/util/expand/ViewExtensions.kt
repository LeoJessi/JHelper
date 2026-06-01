package top.jessi.jhelper.util.expand

import android.view.View

object ViewExtensions {
    /**
     * 控件显示
     */
    fun View.visible() {
        visibility = View.VISIBLE
    }

    /**
     * 控件移除视图
     */
    fun View.gone() {
        visibility = View.GONE
    }

    /**
     * 控件隐藏视图
     */
    fun View.invisible() {
        visibility = View.INVISIBLE
    }

    /**
     * 防抖点击，防止快速重复点击
     * @param interval 防抖间隔，默认 500ms
     * @param onClick 点击回调
     */
    fun View.setOnSingleClickListener(interval: Long = 500L, onClick: (View) -> Unit) {
        var lastClickTime = 0L
        setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > interval) {
                lastClickTime = currentTime
                onClick(it)
            }
        }
    }
}