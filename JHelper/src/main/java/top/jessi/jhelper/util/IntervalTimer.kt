package top.jessi.jhelper.util

import android.os.Handler
import android.os.Looper

/**
 * 周期性间隔定时器
 *
 * 基于 Handler 以固定间隔循环回调，支持暂停与恢复。
 * 适用于轮询、定时刷新、周期任务等场景。
 *
 * 特性：
 * - [start] 后立即回调一次，之后每隔指定间隔回调
 * - 支持 [pause] / [resume] 暂停与恢复
 * - 回调异常不会导致调度链断裂
 *
 * 用法示例：
 * ```
 * val timer = IntervalTimer(1000L, object : IntervalTimer.IntervalCallback {
 *     override fun onTick(count: Int) {
 *         // count: 第几次回调（从 1 开始）
 *         Log.d("Timer", "第 $count 次回调")
 *     }
 * })
 *
 * timer.start()   // 立即回调 count=1，之后每秒回调一次
 * // ...
 * timer.pause()   // 暂停
 * timer.resume()  // 恢复（等待一个间隔后继续回调）
 * timer.cancel()  // 生命周期结束时调用，防止内存泄漏
 * ```
 *
 * ⚠️ 所有方法必须在主线程调用。
 * ⚠️ 请在宿主生命周期结束时（如 Activity.onDestroy）调用 cancel()，防止内存泄漏。
 */
class IntervalTimer(
    private val intervalMillis: Long,
    private val callback: IntervalCallback
) {

    /**
     * 回调间隔必须大于 0
     */
    init {
        require(intervalMillis > 0) { "intervalMillis must be > 0, but was $intervalMillis" }
    }

    /**
     * 定时器回调接口
     *
     * - [onTick] 每次触发时回调
     */
    fun interface IntervalCallback {
        /**
         * 定时器回调
         * @param count 第几次回调（从 1 开始，每次递增）
         */
        fun onTick(count: Int)
    }

    /**
     * 定时器状态
     */
    private enum class State {
        IDLE,      // 初始 / 已取消
        RUNNING,   // 运行中
        PAUSED     // 已暂停
    }

    private val handler = Handler(Looper.getMainLooper())

    private var state = State.IDLE
    private var count = 0

    // ==================== 核心逻辑 ====================

    private val tickRunnable = object : Runnable {
        override fun run() {
            count++
            try {
                callback.onTick(count)
            } catch (e: Exception) {
                // 防止回调异常导致调度链断裂
                e.printStackTrace()
            }
            // 只有在仍处于运行状态时才继续调度，
            // 防止用户在 onTick 中调用 cancel()/pause() 导致多余调度
            if (state == State.RUNNING) {
                handler.postDelayed(this, intervalMillis)
            }
        }
    }

    // ==================== 公开方法（主线程调用） ====================

    /**
     * 启动定时器
     *
     * 立即回调一次（count=1），之后每隔 [intervalMillis] 回调一次。
     * 若当前不在 IDLE 状态则不执行任何操作。
     */
    fun start() {
        if (state != State.IDLE) return
        state = State.RUNNING
        count = 0
        tickRunnable.run()
    }

    /**
     * 暂停定时器
     *
     * 移除待执行的调度任务，不再触发回调。
     * 若当前不在 RUNNING 状态则不执行任何操作。
     */
    fun pause() {
        if (state != State.RUNNING) return
        handler.removeCallbacks(tickRunnable)
        state = State.PAUSED
    }

    /**
     * 恢复定时器
     *
     * 等待一个完整间隔后继续回调，count 继续递增。
     * 若当前不在 PAUSED 状态则不执行任何操作。
     */
    fun resume() {
        if (state != State.PAUSED) return
        state = State.RUNNING
        handler.postDelayed(tickRunnable, intervalMillis)
    }

    /**
     * 取消定时器并重置状态
     *
     * 移除调度任务，重置计数器。取消后可通过 [start] 重新启动。
     */
    fun cancel() {
        handler.removeCallbacks(tickRunnable)
        state = State.IDLE
        count = 0
    }

    /**
     * 重新开始定时器（取消当前并立即启动）
     *
     * count 从 1 重新开始，且立即回调一次。
     */
    fun restart() {
        cancel()
        start()
    }

    /**
     * 是否正在运行
     */
    fun isRunning(): Boolean = state == State.RUNNING

    /**
     * 是否已暂停
     */
    fun isPaused(): Boolean = state == State.PAUSED

    /**
     * 获取已回调次数
     *
     * - IDLE 状态返回 0
     * - RUNNING / PAUSED 状态返回当前 count 值
     */
    fun getElapsedCount(): Int = count

    /**
     * 获取设定的回调间隔（毫秒）
     */
    fun getIntervalMillis(): Long = intervalMillis
}
