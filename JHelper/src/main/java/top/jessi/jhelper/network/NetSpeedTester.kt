package top.jessi.jhelper.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.jessi.jhelper.time.Time
import top.jessi.jhelper.time.Time.diffNow
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

/**
 * 智能播放源选择器
 *
 * 功能：
 * 1. 并发测速多个播放源
 * 2. 计算每个源的响应耗时
 * 3. 自动选出最快的可用源
 * 4. 结果回调到主线程（可直接更新 UI）
 *
 * 线程模型：
 * - 测速逻辑运行在 Dispatchers.IO（子线程）
 * - 回调运行在 Dispatchers.Main（主线程）
 *
 * 适用场景：
 * - 播放器启动前选源
 * - 多 CDN / 多域名调度
 */
object NetSpeedTester {

    /**
     * 单个源测速结果
     *
     * @param source 对应的播放源
     * @param speed 测速耗时（毫秒）
     *              Long.MAX_VALUE 表示测速失败（不可用）
     */
    data class TestResult(val source: String, val speed: Long)

    /**
     * 多源选择结果
     *
     * @param results 所有源的测速结果（包含成功和失败）
     * @param best 最优源（耗时最短的可用源），可能为 null（全部失败）
     */
    data class SelectionResult(val results: List<TestResult>, val best: TestResult?)

    /** TCP连接超时时间（毫秒） */
    private const val CONNECT_TIMEOUT = 2000

    // 探测字节
    private const val TCP_PROBE_BYTE: Byte = 0x1

    // 读取缓冲区大小
    private const val TCP_PROBE_READ_SIZE = 32

    // TCP 读取超时时间
    private const val TCP_READ_TIMEOUT_MS = 1000


    /**
     * 智能选择最佳播放源（推荐入口方法）
     *
     * 执行流程：
     * 1. 在 IO 线程并发测速所有源
     * 2. 收集每个源的耗时
     * 3. 过滤失败源（Long.MAX_VALUE）
     * 4. 选出耗时最短的源
     * 5. 切回主线程返回结果
     *
     * @param sources 播放源列表
     * @param callback 回调结果（主线程）
     */
    @JvmStatic
    fun selectBestNet(sources: List<String>, callback: (SelectionResult) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            // 并发测速（每个源一个协程）
            val results = sources.map { source ->
                async {
                    val speed = testSpeed(source)
                    TestResult(source, speed)
                }
            }.awaitAll()

            // 选出最快的可用源（排除失败）
            val best = results
                    .filter { it.speed != Long.MAX_VALUE }
                    .minByOrNull { it.speed }

            // 切回主线程返回结果（安全更新UI）
            withContext(Dispatchers.Main) {
                callback(SelectionResult(results, best))
            }
        }
    }

    /**
     * 对外暴露的测速方法（单个源）
     *
     * 使用场景：
     * - 调试某个域名速度
     * - 手动测速
     */
    @JvmStatic
    fun testSingleSpeed(url: String): Long = testSpeed(url)

    private fun testSpeed(url: String): Long {
        val uri = URL(url)
        val host = uri.host
        val port = if (uri.port != -1) uri.port else if (uri.protocol == "https") 443 else 80
        val results = mutableListOf<Long>()
        val testCount = 3
        var successCount = 0
        /* 测速3次 抗抖动 更接近真实链路质量 */
        repeat(testCount) {
            val cost = try {
                val start = Time.currentTimeMillis()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT)
                    // 向服务器发送极小数据包 触发真实通信路径（CDN / NAT / 代理） 避免“只connect但不通信”的假成功连接
                    socket.getOutputStream().write(byteArrayOf(TCP_PROBE_BYTE))
                    // 读数据的最大等待时间 防止 CDN / 防火墙连接建立但不返回数据导致卡死 控制测速最大等待时间（1秒）
                    socket.soTimeout = TCP_READ_TIMEOUT_MS
                    // 尝试读取少量数据（最多32字节） 判断是否有真实响应能力（不是死连接）
                    runCatching {
                        socket.getInputStream().read(ByteArray(TCP_PROBE_READ_SIZE))
                    }
                }
                successCount++
                start.diffNow()
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
            results.add(cost)
        }
        val valid = results.filter { it != Long.MAX_VALUE }
        if (valid.isEmpty()) return Long.MAX_VALUE
        val min = valid.minOrNull()!!
        val avg = valid.average()
        val successRate = successCount.toDouble() / testCount
        // 如果有某次测速失败 -- 相对于增加点返回的延迟
        val penalty = when {
            successRate >= 1.0 -> 0
            successRate >= 0.66 -> 50
            successRate >= 0.33 -> 200
            else -> 500
        }
        return (((min + avg) / 2) + penalty).toLong()
    }
}