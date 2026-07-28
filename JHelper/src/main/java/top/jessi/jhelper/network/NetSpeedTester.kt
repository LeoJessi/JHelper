package top.jessi.jhelper.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * 网络测速工具类
 *
 * Java 使用方式：
 * ```
 * SpeedTestManager speedTest = new SpeedTestManager();
 * speedTest.setCallback(new SpeedTestManager.Callback() {
 *     @Override
 *     public void onProgress(double currentSpeedMbps, int progress) { }
 *
 *     @Override
 *     public void onResult(double speedMbps) { }
 *
 *     @Override
 *     public void onError(SpeedTestManager.Error error) { }
 * });
 * speedTest.start();
 *
 * // 取消测速
 * speedTest.cancel();
 * ```
 *
 * Kotlin 使用方式：
 * ```
 * val speedTest = SpeedTestManager()
 * speedTest.callback = object : SpeedTestManager.Callback {
 *     override fun onProgress(currentSpeedMbps: Double, progress: Int) { }
 *     override fun onResult(speedMbps: Double) { }
 *     override fun onError(error: SpeedTestManager.Error) { }
 * }
 * speedTest.start()
 * ```
 *
 * Created by Jessi on 2026/7/27
 * Email：17324719944@189.cn
 */
class NetSpeedTester {

    // ======================== 配置 ========================

    companion object {
        private const val TAG = "SpeedTestManager"
    }

    /**
     * 测速文件大小（字节），默认 50MB
     */
    var testFileSize: Long = 50L * 1024 * 1024

    /**
     * 最大测速时间（毫秒），默认 15 秒
     */
    var maxTestDurationMs: Long = 15_000L

    /**
     * 连接超时时间（毫秒），默认 15 秒
     */
    var connectTimeoutMs: Int = 15_000

    /**
     * 读取超时时间（毫秒），默认 15 秒
     */
    var readTimeoutMs: Int = 15_000

    /**
     * 测速 URL 列表（按优先级排序）
     */
    var speedTestUrls: List<String> = listOf(
        "https://speed.cloudflare.com/__down?bytes=$testFileSize",
        "https://speed.hetzner.de/50MB.bin",
        "https://proof.ovh.net/files/50Mb.dat",
        "http://cachefly.cachefly.net/50mb.test"
    )

    /**
     * 并发线程数，默认 6
     * - 设为 1 时使用单线程模式
     * - 建议范围 1-8，超过 8 线程边际效益递减
     */
    var threadCount: Int = 6

    // ======================== 回调 ========================

    /**
     * 测速回调（所有回调均在主线程）
     */
    var callback: Callback? = null

    // ======================== 线程 ========================

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    /**
     * 是否正在测速
     */
    var isTesting = false
        private set

    // ======================== 多线程管理（新增） ========================

    /** 多线程模式的线程池 */
    private var multiThreadExecutor: ExecutorService? = null

    /** 当前所有活动的连接，用于取消时快速中断 */
    private val activeConnections = mutableListOf<HttpURLConnection>()

    /** 多线程模式下总下载字节数 */
    private val totalDownloadedBytes = AtomicLong(0L)

    /** 多线程模式下取消标志 */
    @Volatile
    private var isMultiThreadCancelled = false

    // ======================== 控制 ========================

    /**
     * 开始测速
     *
     * @param customUrl 自定义测速 URL（可选），传入后仅使用该 URL 测速
     */
    fun start(customUrl: String? = null) {
        if (isTesting) return
        isTesting = true
        executor.execute {
            if (threadCount <= 1) {
                performSingleThreadTest(customUrl)
            } else {
                performMultiThreadTest(customUrl)
            }
        }
    }

    /**
     * 取消测速
     */
    fun cancel() {
        isTesting = false
        isMultiThreadCancelled = true
        // 立即关闭所有连接，让阻塞中的 read() 快速返回
        synchronized(activeConnections) {
            activeConnections.forEach { it.disconnect() }
            activeConnections.clear()
        }
        // 单线程模式的连接
        currentConnection?.disconnect()
        // 中断多线程
        multiThreadExecutor?.shutdownNow()
    }

    // 当前正在使用的连接，用于取消时快速中断（单线程模式）
    @Volatile
    private var currentConnection: HttpURLConnection? = null

    // ======================== 核心逻辑 ========================

    /**
     * 单线程测速（复用原有逻辑）
     */
    private fun performSingleThreadTest(customUrl: String? = null) {
        // 优先使用自定义 URL，否则使用默认列表
        val urls = if (!customUrl.isNullOrEmpty()) {
            listOf(customUrl)
        } else {
            speedTestUrls
        }
        var finalSpeedMbps = 0.0
        var resultError: Error? = null
        var isTimeout = false

        for ((index, url) in urls.withIndex()) {
            if (!isTesting) {
                resultError = Error.CANCELLED
                break
            }

            Log.d(TAG, "单线程测速 [$index/${urls.size - 1}]: $url")

            when (val result = tryDownload(url)) {
                is DownloadResult.Success -> {
                    finalSpeedMbps = result.speedMbps
                    resultError = null
                    break
                }

                is DownloadResult.Timeout -> {
                    isTimeout = true
                }

                is DownloadResult.Error -> {

                }
            }
        }

        // 确定最终错误类型
        if (resultError == null && finalSpeedMbps <= 0) {
            resultError = if (isTimeout) Error.TIMEOUT else Error.FAILED
        }

        // 回调到主线程
        val finalSpeed = finalSpeedMbps
        val finalError = resultError
        mainHandler.post {
            isTesting = false
            if (finalError == null) {
                callback?.onResult(finalSpeed)
            } else {
                callback?.onError(finalError)
            }
        }
    }

    /**
     * 多线程并发测速
     */
    private fun performMultiThreadTest(customUrl: String? = null) {
        // 优先使用自定义 URL，否则使用默认列表
        val urls = if (!customUrl.isNullOrEmpty()) {
            listOf(customUrl)
        } else {
            speedTestUrls
        }

        var finalSpeedMbps = 0.0
        var resultError: Error? = null
        var isTimeout = false

        for ((index, url) in urls.withIndex()) {
            if (!isTesting) {
                resultError = Error.CANCELLED
                break
            }

            Log.d(TAG, "多线程测速 [$index/${urls.size - 1}]: $url, 线程数: $threadCount")

            // 重置多线程状态
            isMultiThreadCancelled = false
            totalDownloadedBytes.set(0L)
            synchronized(activeConnections) {
                activeConnections.clear()
            }

            when (val result = tryMultiThreadDownload(url)) {
                is MultiThreadResult.Success -> {
                    finalSpeedMbps = result.speedMbps
                    resultError = null
                    break
                }

                is MultiThreadResult.Timeout -> {
                    isTimeout = true
                }

                is MultiThreadResult.Error -> {

                }
            }
        }

        // 确定最终错误类型
        if (resultError == null && finalSpeedMbps <= 0) {
            resultError = if (isTimeout) Error.TIMEOUT else Error.FAILED
        }

        // 回调到主线程
        val finalSpeed = finalSpeedMbps
        val finalError = resultError
        mainHandler.post {
            isTesting = false
            if (finalError == null) {
                callback?.onResult(finalSpeed)
            } else {
                callback?.onError(finalError)
            }
        }
    }

    private fun tryDownload(urlString: String): DownloadResult {
        val startTime = System.currentTimeMillis()
        var firstByteTime = 0L  // 第一个字节到达时间
        var downloadedBytes = 0L
        var connection: HttpURLConnection? = null

        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            currentConnection = connection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                instanceFollowRedirects = true
                setRequestProperty("Connection", "close")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val buffer = ByteArray(16 * 1024)
                var bytesRead: Int
                var lastUpdateTime = startTime

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (!isTesting) {
                        inputStream.close()
                        return DownloadResult.Error(IOException("cancelled"))
                    }
                    // 记录第一个字节到达时间
                    if (bytesRead > 0 && firstByteTime == 0L) {
                        firstByteTime = System.currentTimeMillis()
                    }
                    downloadedBytes += bytesRead
                    val now = System.currentTimeMillis()
                    val elapsed = now - startTime

                    // 每 200ms 回调进度
                    if (now - lastUpdateTime > 200) {
                        lastUpdateTime = now
                        val currentSpeed = (downloadedBytes * 8.0) / (elapsed / 1000.0) / 1_000_000.0
                        val progress = ((elapsed.toFloat() / maxTestDurationMs) * 100).toInt().coerceIn(0, 100)
                        mainHandler.post {
                            callback?.onProgress(currentSpeed, progress)
                        }
                    }

                    // 达到最大测速时间，停止
                    if (elapsed >= maxTestDurationMs) {
                        break
                    }
                }
                inputStream.close()

                // 从第一个字节开始计算有效时间
                val effectiveStart = if (firstByteTime > 0) firstByteTime else startTime
                val totalElapsed = System.currentTimeMillis() - effectiveStart
                return if (downloadedBytes > 0 && totalElapsed > 0) {
                    val speedMbps = (downloadedBytes * 8.0) / (totalElapsed / 1000.0) / 1_000_000.0
                    DownloadResult.Success(speedMbps)
                } else {
                    DownloadResult.Error(IOException("no data"))
                }
            } else {
                return DownloadResult.Error(IOException("HTTP $responseCode"))
            }
        } catch (e: SocketTimeoutException) {
            return DownloadResult.Timeout
        } catch (e: IOException) {
            return if (e.message?.lowercase()?.contains("timeout") == true) {
                DownloadResult.Timeout
            } else {
                DownloadResult.Error(e)
            }
        } catch (e: Exception) {
            return DownloadResult.Error(IOException(e.message))
        } finally {
            currentConnection = null
            connection?.disconnect()
        }
    }

    // ======================== 多线程下载实现 ========================

    /**
     * 多线程下载结果
     */
    private sealed class MultiThreadResult {
        data class Success(val speedMbps: Double) : MultiThreadResult()
        data class Error(val error: IOException) : MultiThreadResult()
        object Timeout : MultiThreadResult()
    }

    /**
     * 多线程并发下载
     */
    private fun tryMultiThreadDownload(urlString: String): MultiThreadResult {
        val startTime = System.currentTimeMillis()
        val firstByteTime = AtomicLong(0L)  // 第一个字节到达的时间
        var hasTimeout = false

        // 创建线程池
        val pool = Executors.newFixedThreadPool(threadCount)
        multiThreadExecutor = pool

        // 用于等待所有线程完成
        val completedThreads = java.util.concurrent.atomic.AtomicInteger(0)
        val totalThreads = threadCount
        val lock = Object()

        // 启动进度回调线程
        val progressThread = Thread {
            while (!isMultiThreadCancelled && isTesting) {
                try {
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                    break
                }
                val now = System.currentTimeMillis()
                val firstByte = firstByteTime.get()
                // 第一个字节到达前不回调进度，避免 URL 切换时进度跳回
                if (firstByte <= 0) {
                    continue
                }
                // 从第一个字节开始计算有效时间，effectiveStart 确定后不再变化
                val elapsed = now - firstByte
                if (elapsed > 0) {
                    val bytes = totalDownloadedBytes.get()
                    val currentSpeedMbps = (bytes * 8.0) / (elapsed / 1000.0) / 1_000_000.0
                    val totalElapsed = now - startTime
                    val progress = ((totalElapsed.toFloat() / maxTestDurationMs) * 100).toInt().coerceIn(0, 100)
                    mainHandler.post {
                        if (isTesting) {
                            callback?.onProgress(currentSpeedMbps, progress)
                        }
                    }
                }
            }
        }
        progressThread.isDaemon = true
        progressThread.start()

        // 提交下载任务
        for (i in 0 until threadCount) {
            pool.execute {
                try {
                    downloadTask(urlString, startTime) { bytesRead ->
                        // 第一次收到数据时记录 firstByteTime（只设置一次）
                        if (bytesRead > 0) {
                            firstByteTime.compareAndSet(0L, System.currentTimeMillis())
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    hasTimeout = true
                } catch (e: IOException) {
                    if (e.message?.lowercase()?.contains("timeout") == true) {
                        hasTimeout = true
                    }
                } catch (e: Exception) {
                    // 其他异常忽略，最终通过 totalBytes 判断是否成功
                } finally {
                    val completed = completedThreads.incrementAndGet()
                    if (completed >= totalThreads) {
                        synchronized(lock) {
                            lock.notifyAll()
                        }
                    }
                }
            }
        }

        // 等待所有线程完成或超时
        synchronized(lock) {
            val remainingTime = maxTestDurationMs - (System.currentTimeMillis() - startTime)
            if (remainingTime > 0) {
                lock.wait(remainingTime)
            }
        }

        // 标记取消，停止进度线程
        isMultiThreadCancelled = true
        pool.shutdownNow()

        // 等待进度线程完全停止，避免 URL 切换时旧线程仍在回调
        try {
            progressThread.join(300)
        } catch (e: InterruptedException) {
            // ignore
        }

        // 关闭所有连接
        synchronized(activeConnections) {
            activeConnections.forEach { it.disconnect() }
            activeConnections.clear()
        }

        // 使用 firstByteTime 计算有效时间（排除连接建立开销）
        val firstByte = firstByteTime.get()
        val effectiveStart = if (firstByte > 0) firstByte else startTime
        val totalElapsed = System.currentTimeMillis() - effectiveStart
        val totalBytes = totalDownloadedBytes.get()

        return if (totalBytes > 0 && totalElapsed > 0) {
            val speedMbps = (totalBytes * 8.0) / (totalElapsed / 1000.0) / 1_000_000.0
            MultiThreadResult.Success(speedMbps)
        } else if (hasTimeout) {
            MultiThreadResult.Timeout
        } else {
            MultiThreadResult.Error(IOException("download failed"))
        }
    }

    /**
     * 单个线程的下载任务
     *
     * @param urlString 下载 URL
     * @param globalStartTime 全局开始时间（用于超时判断）
     * @param onBytesRead 每读取一次数据的回调，用于记录第一个字节时间
     */
    private fun downloadTask(urlString: String, globalStartTime: Long, onBytesRead: (Int) -> Unit = {}) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection

            // 注册到活动连接列表
            synchronized(activeConnections) {
                activeConnections.add(connection)
            }

            connection.apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                instanceFollowRedirects = true
                setRequestProperty("Connection", "close")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val buffer = ByteArray(16 * 1024)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (!isTesting || isMultiThreadCancelled) {
                        inputStream.close()
                        return
                    }

                    totalDownloadedBytes.addAndGet(bytesRead.toLong())
                    onBytesRead(bytesRead)

                    // 检查是否超时
                    val elapsed = System.currentTimeMillis() - globalStartTime
                    if (elapsed >= maxTestDurationMs) {
                        inputStream.close()
                        return
                    }
                }
                inputStream.close()
            }
        } finally {
            // 从活动连接列表移除
            synchronized(activeConnections) {
                connection?.let { activeConnections.remove(it) }
            }
            connection?.disconnect()
        }
    }

    // ======================== 接口 & 枚举 ========================

    /**
     * 测速回调（所有回调均在主线程）
     */
    interface Callback {
        /**
         * 测速进度更新
         *
         * @param currentSpeedMbps 当前速度（Mbps），除以 8 即为 MB/s
         * @param progress         进度（0-100）
         */
        fun onProgress(currentSpeedMbps: Double, progress: Int)

        /**
         * 测速成功
         *
         * @param speedMbps 最终速度（Mbps）
         */
        fun onResult(speedMbps: Double)

        /**
         * 测速失败
         */
        fun onError(error: Error)
    }

    /**
     * 错误类型
     */
    enum class Error {
        /** 测速失败（所有节点都不可达） */
        FAILED,

        /** 测速超时 */
        TIMEOUT,

        /** 用户取消 */
        CANCELLED
    }

    // ======================== 内部类 ========================

    private sealed class DownloadResult {
        data class Success(val speedMbps: Double) : DownloadResult()
        data class Error(val error: IOException) : DownloadResult()
        object Timeout : DownloadResult()
    }
}
