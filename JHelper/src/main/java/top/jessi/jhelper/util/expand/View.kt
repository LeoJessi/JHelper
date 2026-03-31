package top.jessi.jhelper.util.expand

import android.view.View

object View {
    /**
     * 空间显示
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
}