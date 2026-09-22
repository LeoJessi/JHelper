package top.jessi.jhelper.time

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by Jessi on 2026/9/21 14:18
 * Email：17324719944@189.cn
 * Describe：获取NTP真实时间
 *
 * 使用方式：
 * ```kotlin
 * // 获取实例并自动开始同步
 * val fetcher = FetchRealTime.getInstance()
 *
 * // 获取真实时间戳（同步前返回系统时间）
 * val realTime = fetcher.getRealTimeMillis()
 *
 * // 手动同步（可选）
 * fetcher.syncTime { success, offset ->
 *     if (success) {
 *         println("时间同步成功，偏移：${offset}ms")
 *     }
 * }
 *
 * // 检查是否已同步
 * if (fetcher.isSynced()) {
 *     // 已同步，时间可靠
 * }
 * ```
 *
 * Java 使用方式：
 * ```java
 * FetchRealTime fetcher = FetchRealTime.getInstance();
 * long realTime = fetcher.getRealTimeMillis();
 *
 * fetcher.syncTime((success, offset) -> {
 *     if (success) {
 *         System.out.println("时间同步成功，偏移：" + offset + "ms");
 *     }
 *     return null;
 * });
 * ```
 */
class FetchRealTime private constructor() {

    companion object {
        private const val TAG = "FetchRealTime"

        /** NTP 端口 */
        private const val NTP_PORT = 123

        /** NTP 时间基准偏移（1900-01-01 到 1970-01-01 的秒数） */
        private const val OFFSET_1900_TO_1970 = 2208988800L

        /** 单个服务器超时时间（毫秒） */
        private const val TIMEOUT_PER_SERVER = 3000

        /** 全局超时时间（毫秒） */
        private const val GLOBAL_TIMEOUT = 5000L

        @Volatile
        private var instance: FetchRealTime? = null

        /**
         * 获取单例实例（首次调用会自动异步同步时间）
         */
        @JvmStatic
        fun getInstance(): FetchRealTime {
            return instance ?: synchronized(this) {
                instance ?: FetchRealTime().also {
                    instance = it
                    // App 启动时自动异步同步
                    it.syncTimeIfNeeded()
                }
            }
        }
    }

    // ======================== 配置 ========================

    /**
     * NTP 服务器列表（全球通用，国内外均可访问）
     *
     * 优先级说明：
     * 1. time.cloudflare.com - Cloudflare 全球 Anycast，国内外速度最快
     * 2. ntp.aliyun.com - 阿里云 NTP，国内速度极快
     * 3. pool.ntp.org - 全球最大 NTP 集群，高可用
     * 4. time.windows.com - 微软官方，国内可访问
     * 5. ntp.tencent.com - 腾讯云 NTP，国内备用
     * 6. time.apple.com - Apple 官方，全球可用
     * 7. time.nist.gov - 国国家标准与技术研究院提供的公共时间服务
     * 8. time.google.com - Google
     */
    var ntpServers: List<String> = listOf(
        "time.cloudflare.com",
        "ntp.aliyun.com",
        "pool.ntp.org",
        "time.windows.com",
        "ntp.tencent.com",
        "time.apple.com",
        "time.nist.gov",
        "time.google.com"
    )

    /**
     * HTTP 备用时间源（当 NTP 全部失败时使用）
     *
     * 说明：通过 HEAD 请求获取响应头的 Date 字段
     * 注意：使用 HTTP（非 HTTPS）避免证书验证依赖系统时间
     * 优先级：根据实测速度排序
     */
    var httpFallbackUrls: List<String> = listOf(
        "http://www.amazon.com/",
        "http://www.google.com/",
        "http://www.apple.com",
        "http://www.cloudflare.com",
        "http://www.alibaba.com",
        "http://www.163.com",
        "http://www.qq.com",
        "http://www.baidu.com"
    )

    // ======================== 内存缓存 ========================

    /** 网络同步的时间戳（毫秒） */
    @Volatile
    private var networkTimeMillis: Long = 0L

    /** 本地参考时间点（SystemClock.elapsedRealtime()） */
    @Volatile
    private var localReferenceMillis: Long = 0L

    /** 是否已同步 */
    @Volatile
    private var synced: Boolean = false

    /** 是否正在同步 */
    @Volatile
    private var syncing: Boolean = false

    // ======================== 线程管理 ========================

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newCachedThreadPool()

    // ======================== 公共接口 ========================

    /**
     * 获取真实时间戳（毫秒）
     *
     * @return 如果已同步，返回校准后的时间；否则返回系统时间
     */
    fun getRealTimeMillis(): Long {
        if (!synced) {
            // 未同步时触发同步（如果尚未开始）
            syncTimeIfNeeded()
            return System.currentTimeMillis()
        }

        // 计算真实时间：网络时间 + 经过的时间
        val elapsed = SystemClock.elapsedRealtime() - localReferenceMillis
        return networkTimeMillis + elapsed
    }

    /**
     * 获取时间偏移量（真实时间 - 系统时间）
     *
     * @return 偏移量（毫秒），未同步时返回 0
     */
    fun getOffset(): Long {
        if (!synced) return 0L
        return getRealTimeMillis() - System.currentTimeMillis()
    }

    /**
     * 是否已同步
     */
    fun isSynced(): Boolean = synced

    /**
     * 是否正在同步
     */
    fun isSyncing(): Boolean = syncing

    /**
     * 同步时间（异步执行）
     *
     * @param callback 同步完成回调（在主线程执行），参数：(success: Boolean, offset: Long)
     */
    fun syncTime(callback: ((Boolean, Long) -> Unit)? = null) {
        if (syncing) {
            callback?.let { mainHandler.post { it(false, 0L) } }
            return
        }

        syncing = true
        executor.execute {
            try {
                // 1. 尝试 NTP 多线程竞速
                var timestamp = fetchNtpTimeRace()

                // 2. NTP 失败，降级到 HTTP
                if (timestamp == null) {
                    timestamp = fetchHttpTimeRace()
                }

                // 3. 更新缓存
                if (timestamp != null && timestamp > 0) {
                    updateTimeReference(timestamp)
                    val offset = getOffset()
                    mainHandler.post {
                        callback?.invoke(true, offset)
                    }
                } else {
                    mainHandler.post {
                        callback?.invoke(false, 0L)
                    }
                }
            } finally {
                syncing = false
            }
        }
    }

    // ======================== 内部方法 ========================

    /**
     * 如果需要则同步时间（内部调用，静默同步）
     */
    private fun syncTimeIfNeeded() {
        if (!synced && !syncing) {
            syncTime(callback = null)
        }
    }

    /**
     * 更新时间参考点
     */
    private fun updateTimeReference(networkTime: Long) {
        networkTimeMillis = networkTime
        localReferenceMillis = SystemClock.elapsedRealtime()
        synced = true
        Log.d(TAG, "updateTimeReference: !!")
    }

    /**
     * NTP 多线程竞速模式
     *
     * @return 第一个成功响应的时间戳，全部失败返回 null
     */
    private fun fetchNtpTimeRace(): Long? {
        if (ntpServers.isEmpty()) {
            Log.w(TAG, "NTP 服务器列表为空")
            return null
        }

        val pool = Executors.newFixedThreadPool(ntpServers.size)
        val result = AtomicLong(-1L)
        val hasResult = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        ntpServers.forEach { server ->
            pool.execute {
                if (!hasResult.get()) {
                    try {
                        val timestamp = fetchNtpTime(server)
                        if (timestamp != null && timestamp > 0) {
                            if (hasResult.compareAndSet(false, true)) {
                                result.set(timestamp)
                                latch.countDown()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "NTP 服务器 $server 异常", e)
                    }
                }
            }
        }

        try {
            latch.await(GLOBAL_TIMEOUT, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Log.w(TAG, "NTP 竞速等待中断")
        }

        pool.shutdownNow()
        return if (result.get() > 0) result.get() else null
    }

    /**
     * HTTP 多线程竞速模式（备用方案）
     */
    private fun fetchHttpTimeRace(): Long? {
        if (httpFallbackUrls.isEmpty()) {
            Log.w(TAG, "HTTP URL 列表为空")
            return null
        }

        val pool = Executors.newFixedThreadPool(httpFallbackUrls.size)
        val result = AtomicLong(-1L)
        val hasResult = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        httpFallbackUrls.forEach { url ->
            pool.execute {
                if (!hasResult.get()) {
                    try {
                        val timestamp = fetchHttpTime(url)
                        if (timestamp != null && timestamp > 0) {
                            if (hasResult.compareAndSet(false, true)) {
                                result.set(timestamp)
                                latch.countDown()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "HTTP $url 异常", e)
                    }
                }
            }
        }

        try {
            latch.await(GLOBAL_TIMEOUT, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Log.w(TAG, "HTTP 竞速等待中断")
        }

        pool.shutdownNow()
        return if (result.get() > 0) result.get() else null
    }

    /**
     * 从单个 NTP 服务器获取时间
     *
     * @param server NTP 服务器地址
     * @return 时间戳（毫秒），失败返回 null
     */
    private fun fetchNtpTime(server: String): Long? {
        var socket: DatagramSocket? = null
        try {
            // 1. 创建 UDP Socket
            socket = DatagramSocket()
            socket.soTimeout = TIMEOUT_PER_SERVER

            // 2. 构建 NTP 请求包（48 字节）
            val request = ByteArray(48)
            request[0] = 0x1B.toByte()  // LI=0, VN=3, Mode=3

            // 3. 发送请求
            val address = InetAddress.getByName(server)
            val requestPacket = DatagramPacket(request, request.size, address, NTP_PORT)
            socket.send(requestPacket)

            // 4. 接收响应
            val response = ByteArray(48)
            val responsePacket = DatagramPacket(response, response.size)
            socket.receive(responsePacket)

            // 5. 解析时间戳（字节 40-43 是秒部分，44-47 是秒的小数部分）
            val seconds = ((response[40].toLong() and 0xFF) shl 24) or
                    ((response[41].toLong() and 0xFF) shl 16) or
                    ((response[42].toLong() and 0xFF) shl 8) or
                    (response[43].toLong() and 0xFF)

            val fraction = ((response[44].toLong() and 0xFF) shl 24) or
                    ((response[45].toLong() and 0xFF) shl 16) or
                    ((response[46].toLong() and 0xFF) shl 8) or
                    (response[47].toLong() and 0xFF)

            // 6. 转换为 Unix 时间戳（毫秒）
            val unixSeconds = seconds - OFFSET_1900_TO_1970
            val milliseconds = (fraction * 1000L) / 0x100000000L
            return unixSeconds * 1000L + milliseconds

        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "NTP 服务器 $server 超时")
            return null
        } catch (e: IOException) {
            Log.w(TAG, "NTP 服务器 $server IO异常: ${e.message}")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "NTP 服务器 $server 异常", e)
            return null
        } finally {
            socket?.close()
        }
    }

    /**
     * 从 HTTP 服务器获取时间（解析 Date 响应头）
     *
     * @param urlString HTTP(S) URL
     * @return 时间戳（毫秒），失败返回 null
     */
    private fun fetchHttpTime(urlString: String): Long? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "HEAD"  // 只获取响应头，节省流量
                connectTimeout = TIMEOUT_PER_SERVER
                readTimeout = TIMEOUT_PER_SERVER
                instanceFollowRedirects = true
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache")
            }

            connection.connect()

            // 解析 Date 响应头
            val dateHeader = connection.getHeaderField("Date")
            if (!dateHeader.isNullOrEmpty()) {
                return parseHttpDate(dateHeader)
            }

            return null
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "HTTP $urlString 超时")
            return null
        } catch (e: IOException) {
            Log.w(TAG, "HTTP $urlString IO异常: ${e.message}")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "HTTP $urlString 异常", e)
            return null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * 解析 HTTP Date 响应头
     *
     * 格式：Wed, 21 Sep 2026 14:30:00 GMT
     */
    private fun parseHttpDate(dateString: String): Long? {
        return try {
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("GMT")
            val date = sdf.parse(dateString)
            date?.time
        } catch (e: Exception) {
            null
        }
    }
}
