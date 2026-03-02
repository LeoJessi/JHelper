package top.jessi.jhelper.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import java.lang.ref.WeakReference
import java.security.AccessController.getContext

/**
 * Created by Jessi on 2026/2/28 10:21
 * Email：17324719944@189.cn
 * Describe：
 */
@SuppressLint("StaticFieldLeak")
object IToast {

    private var mTextView: TextView? = null
    private var context: WeakReference<Context>? = null
    private var sToast: Toast? = null
    private var mGravity: Int = -1
    private var mXOffset: Int = 0
    private var mYOffset: Int = 80

    private fun getContext(): Context {
        return context?.get() ?: throw IllegalStateException("Please call init() in Application")
    }

    // 初始化时传入 ApplicationContext（避免持有 Activity Context 引发内存泄漏）
    @JvmStatic
    fun init(context: Context) {
        this.context = WeakReference(context.applicationContext)  // 使用 ApplicationContext 以避免内存泄漏
    }

    @JvmStatic
    fun setTextView(textView: TextView?): IToast {
        this.mTextView = textView
        return this
    }

    @JvmStatic
    fun setGravity(gravity: Int = Gravity.BOTTOM, xOffset: Int = 0, yOffset: Int = 80): IToast {
        this.mGravity = gravity
        this.mXOffset = xOffset
        this.mYOffset = yOffset
        return this
    }

    @JvmStatic
    fun shorts(message: CharSequence) {
        show(message, Toast.LENGTH_SHORT)
    }

    @JvmStatic
    fun shorts(@StringRes resId: Int) {
        show(getContext().getString(resId), Toast.LENGTH_SHORT)
    }

    @JvmStatic
    fun longs(message: CharSequence) {
        show(message, Toast.LENGTH_LONG)
    }

    @JvmStatic
    fun longs(@StringRes resId: Int) {
        show(getContext().getString(resId), Toast.LENGTH_LONG)
    }

    @JvmStatic
    fun show(
        message: CharSequence, duration: Int, textView: TextView? = mTextView,
        gravity: Int = mGravity, xOffset: Int = mXOffset, yOffset: Int = mYOffset
    ) {
        if (TextUtils.isEmpty(message)) return
        if (Looper.getMainLooper().thread === Thread.currentThread()) {
            showToast(message, duration, textView, gravity, xOffset, yOffset)
        } else {
            Handler(Looper.getMainLooper()).post {
                showToast(message, duration, textView, gravity, xOffset, yOffset)
            }
        }
    }

    // 取消当前显示的 Toast
    @JvmStatic
    fun cancel() {
        sToast?.cancel()
    }

    private fun showToast(
        message: CharSequence, duration: Int, textView: TextView? = mTextView,
        gravity: Int = mGravity, xOffset: Int = mXOffset, yOffset: Int = mYOffset
    ) {
        val context = getContext()
        cancel()
        sToast = Toast.makeText(context, message, duration).apply {
            if (textView != null) {
                // 如果已经有父视图，先移除它
                val parent = textView.parent as? ViewGroup
                parent?.removeView(textView)
                textView.text = message
                val root = LinearLayout(context)
                root.addView(textView)
                root.gravity = Gravity.CENTER_HORIZONTAL
                setView(root)
            }
            if (gravity != -1) setGravity(gravity, xOffset, yOffset)
            show()
        }
    }
}