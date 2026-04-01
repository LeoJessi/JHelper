package top.jessi.jhelper.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.jessi.jhelper.time.Time
import top.jessi.jhelper.time.Time.diffNow
import java.net.HttpURLConnection
import java.net.URL

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

    /** TCP连接超时时间（毫秒） */
    private const val CONNECT_TIMEOUT = 2000

    /** 数据读取超时时间（毫秒） */
    private const val READ_TIMEOUT = 3000

    /**
     * 测试数据大小（字节）
     *
     * 为什么只请求100KB？
     * - 避免下载完整视频（节省流量）
     * - 足够评估首包 + 带宽情况
     */
    private const val TEST_SIZE = 100 * 1024 // 100KB

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
    fun testSpeedPublic(url: String): Long = testSpeed(url)

    /**
     * 核心测速实现（私有）
     *
     * 测速逻辑：
     * 1. 建立 HTTP 连接
     * 2. 请求前 TEST_SIZE 字节数据（Range）
     * 3. 读取数据直到达到指定大小
     * 4. 计算总耗时
     *
     * 为什么使用 Range 请求：
     * - 避免下载整个视频文件
     * - 更接近真实播放首包速度
     *
     * @param url 测试地址
     * @return 耗时（毫秒），失败返回 Long.MAX_VALUE
     */
    private fun testSpeed(url: String): Long {
        val start = Time.currentTimeMillis()
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = "GET"
                // 只请求部分数据（用于测速）
                setRequestProperty("Range", "bytes=0-$TEST_SIZE")
                // 允许重定向（CDN / Cloudflare 场景常见）
                instanceFollowRedirects = true
            }
            conn.connect()
            // 读取数据（限制大小）
            conn.inputStream.use { stream ->
                val buffer = ByteArray(4096)
                var total = 0

                while (stream.read(buffer).also { total += it } != -1) {
                    if (total >= TEST_SIZE) break
                }
            }
            // 返回耗时
            start.diffNow()
        } catch (e: Exception) {
            // 任意异常都认为测速失败
            Long.MAX_VALUE
        }
    }
}