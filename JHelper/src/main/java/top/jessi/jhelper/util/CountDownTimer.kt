package top.jessi.jhelper.util

import android.os.Handler
import android.os.Looper

/**
 * 倒计时器
 *
 * 基于 Handler 每秒回调一次，实现简单倒计时功能。
 * 适用于对精度要求不高的场景（如验证码倒计时、限时操作等）。
 *
 * 用法示例：
 * ```
 * val timer = CountDownTimer(30, object : CountDownTimer.CountDownCallback {
 *     override fun onTick(remainingSeconds: Int, totalSeconds: Int) {
 *         // remainingSeconds: 剩余秒数（从 totalSeconds 递减到 1）
 *         // totalSeconds: 总秒数（即构造时传入的值）
 *     }
 *     override fun onFinish() {
 *         // 倒计时归零
 *     }
 * })
 *
 * timer.start()
 * // ...
 * timer.cancel()  // 生命周期结束时调用，防止内存泄漏
 * ```
 *
 * ⚠️ 所有方法必须在主线程调用。
 * ⚠️ 请在宿主生命周期结束时（如 Activity.onDestroy）调用 cancel()，防止内存泄漏。
 */
class CountDownTimer(private val totalSeconds: Int, private val callback: CountDownCallback) {

    /**
     * 倒计时回调接口
     *
     * - [onTick] 每秒回调一次（默认空实现，可选覆盖）
     * - [onFinish] 倒计时归零时回调（必须实现）
     */
    interface CountDownCallback {
        /**
         * 每秒回调一次
         * @param remainingSeconds 剩余秒数（从 totalSeconds 递减到 1）
         * @param totalSeconds 总秒数（构造时传入的值）
         */
        fun onTick(remainingSeconds: Int, totalSeconds: Int) {}

        /**
         * 倒计时归零完成
         */
        fun onFinish()
    }

    private val handler = Handler(Looper.getMainLooper())

    private var currentRemaining = 0
    private var isRunning = false
    private var isPaused = false

    // ==================== 核心逻辑 ====================

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (isPaused || !isRunning) return

            try {
                callback.onTick(currentRemaining, totalSeconds)
            } catch (e: Exception) {
                // 防止回调异常导致调度链断裂
                e.printStackTrace()
            }

            currentRemaining--

            if (currentRemaining <= 0) {
                isRunning = false
                try {
                    callback.onFinish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    // ==================== 公开方法（主线程调用） ====================

    /**
     * 开始倒计时
     * 若正在运行或已暂停则不重复启动
     */
    fun start() {
        if (isRunning || isPaused) return
        if (totalSeconds <= 0) {
            callback.onFinish()
            return
        }
        isRunning = true
        currentRemaining = totalSeconds
        handler.post(tickRunnable)
    }

    /**
     * 暂停倒计时
     */
    fun pause() {
        if (!isRunning || isPaused) return
        handler.removeCallbacks(tickRunnable)
        isRunning = false
        isPaused = true
    }

    /**
     * 恢复倒计时
     */
    fun resume() {
        if (!isPaused) return
        if (currentRemaining <= 0) {
            isPaused = false
            callback.onFinish()
            return
        }
        isPaused = false
        isRunning = true
        handler.post(tickRunnable)
    }

    /**
     * 取消倒计时并重置状态
     */
    fun cancel() {
        handler.removeCallbacks(tickRunnable)
        isRunning = false
        isPaused = false
        currentRemaining = 0
    }

    /**
     * 重新开始倒计时（取消当前并立即启动）
     */
    fun restart() {
        cancel()
        start()
    }

    fun isRunning(): Boolean = isRunning

    fun isPaused(): Boolean = isPaused

    /**
     * 获取当前剩余秒数
     */
    fun getRemainingSeconds(): Int = if (isRunning || isPaused) currentRemaining else 0
}
