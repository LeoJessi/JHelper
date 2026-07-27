package top.jessi.jhelper.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Executors

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
     * 测速文件大小（字节），默认 20MB
     */
    var testFileSize: Long = 20L * 1024 * 1024

    /**
     * 最大测速时间（毫秒），默认 15 秒（含 2 秒预热）
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
        "http://cachefly.cachefly.net/20mb.test",
        "https://speedtest-sg1.digitalocean.com/20mb.test",
        "https://speedtest.tele2.net/20MB.zip"
    )

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

    // ======================== 控制 ========================

    /**
     * 开始测速
     *
     * @param customUrl 自定义测速 URL（可选），传入后仅使用该 URL 测速
     */
    fun start(customUrl: String? = null) {
        if (isTesting) return
        isTesting = true
        executor.execute { performSpeedTest(customUrl) }
    }

    /**
     * 取消测速
     */
    fun cancel() {
        isTesting = false
        // 立即关闭连接，让阻塞中的 read() 快速返回
        currentConnection?.disconnect()
    }

    // 当前正在使用的连接，用于取消时快速中断
    @Volatile
    private var currentConnection: HttpURLConnection? = null

    // ======================== 核心逻辑 ========================

    private fun performSpeedTest(customUrl: String? = null) {
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

            Log.d(TAG, "start speed test [$index/${urls.size - 1}]")

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

    private fun tryDownload(urlString: String): DownloadResult {
        val startTime = System.currentTimeMillis()
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

                val totalElapsed = System.currentTimeMillis() - startTime
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
