package top.jessi.jhelper.util.expand

import android.view.View
import android.view.ViewGroup

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

    /**
     * 设置控件宽度
     * @param width 宽度值（px）
     */
    fun View.width(width: Int) {
        val params = layoutParams
        params.width = width
        layoutParams = params
    }

    /**
     * 设置控件高度
     * @param height 高度值（px）
     */
    fun View.height(height: Int) {
        val params = layoutParams
        params.height = height
        layoutParams = params
    }

    /**
     * 同时设置控件宽高
     * @param width 宽度值（px）
     * @param height 高度值（px）
     */
    fun View.setSize(width: Int, height: Int) {
        val params = layoutParams
        params.width = width
        params.height = height
        layoutParams = params
    }

    /**
     * 设置控件外边距（px）
     * @param left 左外边距
     * @param top 上外边距
     * @param right 右外边距
     * @param bottom 下外边距
     */
    fun View.setMargin(left: Int, top: Int, right: Int, bottom: Int) {
        if (layoutParams is ViewGroup.MarginLayoutParams) {
            val params = layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(left, top, right, bottom)
            layoutParams = params
        }
    }

    /**
     * 设置控件四周外边距相同（px）
     * @param margin 外边距值
     */
    fun View.setMargin(margin: Int) {
        setMargin(margin, margin, margin, margin)
    }
}