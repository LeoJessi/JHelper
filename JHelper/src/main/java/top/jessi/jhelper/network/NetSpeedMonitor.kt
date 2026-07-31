package top.jessi.jhelper.network

import android.net.TrafficStats
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * 实时网速监测工具
 *
 * 通过 [TrafficStats] 周期性采样接收字节数，计算当前网速，
 * 结果通过 [Callback.onSpeed] 回调到**主线程**。
 *
 * 支持两种统计口径：
 * - [Mode.APP]（默认）：仅统计本 App 流量，适合评估视频播放是否卡顿
 * - [Mode.DEVICE]：统计设备全局流量，适合评估整体网络质量
 *
 * 使用示例（Java）：
 * ```
 * // 默认：App 级、1 秒间隔
 * NetSpeedMonitor monitor = new NetSpeedMonitor(bytesPerSecond -> {
 *     textView.setText(formatSpeed(bytesPerSecond));
 * });
 *
 * // 设备级、2 秒间隔
 * NetSpeedMonitor monitor = new NetSpeedMonitor(
 *     bytesPerSecond -> textView.setText(formatSpeed(bytesPerSecond)),
 *     NetSpeedMonitor.Mode.DEVICE,
 *     2000L
 * );
 * monitor.start();
 * // ...
 * monitor.close();
 * ```
 *
 * 注意：务必在适当时机调用 [stop] / [close]，避免线程泄漏。
 */
class NetSpeedMonitor @JvmOverloads constructor(
    private val callback: Callback,
    private val mode: Mode = Mode.DEVICE,
    private val intervalMs: Long = 1000L
) : AutoCloseable {

    init {
        require(intervalMs > 0) { "intervalMs must be positive, but was $intervalMs" }
    }

    /** 统计口径 */
    enum class Mode {
        /** 设备全局流量（TrafficStats.getTotalRxBytes） */
        DEVICE,

        /** 本 App 流量（TrafficStats.getUidRxBytes） */
        APP
    }

    /** 网速回调（SAM 接口，Java 调用方可使用 lambda） */
    fun interface Callback {
        /** @param bytesPerSecond 当前网速值（字节/秒） */
        fun onSpeed(bytesPerSecond: Long)
    }

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private val lastRxBytes = AtomicLong(0L)
    private val lastTimeStamp = AtomicLong(0L)
    private val currentSpeed = AtomicLong(0L)

    private val uid = Process.myUid()

    @Volatile
    private var executor = createExecutor()

    @Volatile
    private var future: ScheduledFuture<*>? = null

    @Volatile
    private var isPaused = false

    @Volatile
    private var isStopped = true

    /** 当前网速值（字节/秒），供调用方同步读取。 */
    val bytesPerSecond: Long
        get() = currentSpeed.get()

    /** 开始监测。若已停止则会重建线程池并重新调度；若正在运行则不重复启动。 */
    fun start() {
        if (isStopped) {
            // stop() 后线程池已关闭，需重建
            if (executor.isShutdown) {
                executor = createExecutor()
            }
            isStopped = false
            isPaused = false
            scheduleTask()
        }
    }

    /** 暂停监测，保留 executor 以便恢复。 */
    fun pause() {
        if (!isPaused && !isStopped) {
            future?.cancel(false)
            future = null
            isPaused = true
        }
    }

    /**
     * 恢复监测。
     * 同时重置采样基准值，避免暂停期间累积的流量导致恢复后网速虚高。
     */
    fun resume() {
        if (isPaused && !isStopped) {
            lastRxBytes.set(0L)
            lastTimeStamp.set(0L)
            currentSpeed.set(0L)
            isPaused = false
            scheduleTask()
        }
    }

    /** 停止监测并释放线程资源。 */
    fun stop() {
        if (!isStopped) {
            isStopped = true
            future?.cancel(true)
            future = null
            executor.shutdown()
        }
    }

    /** 等同于 [stop]，支持 Java 7+ 的 try-with-resources。 */
    override fun close() = stop()

    fun isPaused(): Boolean = isPaused

    fun isStopped(): Boolean = isStopped

    private fun scheduleTask() {
        future = executor.scheduleWithFixedDelay({
            try {
                // 防止 stop() 后仍在执行的任务触发回调
                if (isStopped) return@scheduleWithFixedDelay

                val nowBytes = getReceiveBytes()
                // 设备不支持流量统计时跳过本次采样
                if (nowBytes == TrafficStats.UNSUPPORTED.toLong()) return@scheduleWithFixedDelay

                // 使用 elapsedRealtime() 避免网络授时导致的时间跳变
                val nowTime = SystemClock.elapsedRealtime()
                val prevBytes = lastRxBytes.getAndSet(nowBytes)
                val prevTime = lastTimeStamp.getAndSet(nowTime)

                val rawSpeed: Long

                if (prevTime == 0L || nowBytes < prevBytes) {
                    // 首次采样 / 流量计数器回绕或设备重启
                    rawSpeed = 0L
                } else {
                    val deltaTime = nowTime - prevTime
                    rawSpeed = if (deltaTime <= 0) 0L
                    else (nowBytes - prevBytes) * 1000 / deltaTime
                }

                currentSpeed.set(rawSpeed)

                handler.post {
                    // 再次检查，防止任务已停止但 post 已提交
                    if (isStopped) return@post
                    try {
                        callback.onSpeed(rawSpeed)
                    } catch (e: Exception) {
                        // 防止调用方异常导致调度静默停止
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                // 防止采样逻辑异常导致调度静默停止
                e.printStackTrace()
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS)
    }

    private fun createExecutor() = Executors.newSingleThreadScheduledExecutor {
        Thread(it, "net-speed-monitor").apply { isDaemon = true }
    }

    private fun getReceiveBytes(): Long {
        return when (mode) {
            Mode.DEVICE -> TrafficStats.getTotalRxBytes()
            Mode.APP -> TrafficStats.getUidRxBytes(uid)
        }
    }
}
