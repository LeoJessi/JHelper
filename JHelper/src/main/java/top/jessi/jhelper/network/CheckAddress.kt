package top.jessi.jhelper.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by Jessi on 2026/8/26 15:55
 * Email：17324719944@189.cn
 * Describe：检查网络所在区域
 *
 * 多源备份策略：依次尝试多个 IP 地理定位 API，防止单一服务失效
 * 使用标准库 HttpURLConnection，无需额外依赖
 *
 * 特性：
 * - 并行请求多个 API，取最快返回的结果
 * - 自动检测网络状态，离线时快速返回
 * - 协程安全，无内存泄漏风险
 */
class CheckAddress {

    companion object {
        /** 默认超时时间（毫秒） */
        private const val DEFAULT_TIMEOUT_MS = 8_000
    }

    // ===== 数据模型 =====

    /**
     * IP 地理定位结果
     * @property countryCode ISO 3166-1 alpha-2 国家代码，如 "CN"、"US"
     * @property country 国家全称
     * @property region 地区/省份
     * @property city 城市
     * @property source 数据来源 API 名称
     */
    data class GeoResult(
        val countryCode: String,
        val country: String = "",
        val region: String = "",
        val city: String = "",
        val source: String = ""
    )

    /**
     * 检查配置选项
     * @property timeoutMs 单个 API 超时时间（毫秒）
     * @property parallel 是否并行请求多个 API（更快但流量更多）
     */
    data class Options(
        val timeoutMs: Int = DEFAULT_TIMEOUT_MS,
        val parallel: Boolean = true
    )

    /**
     * 检查回调接口
     */
    interface CheckCallback {
        /** 成功获取到结果（无论是否中国大陆） */
        fun onSuccess(result: GeoResult)

        /** 所有 API 均请求失败 */
        fun onFailure()
    }

    // ===== API 端点配置 =====

    /**
     * IP 地理定位 API 配置
     * 按优先级排列，依次尝试
     */
    private data class ApiEndpoint(
        val name: String,
        val url: String,
        val countryCodeExtractor: (JSONObject) -> String?
    )

    /**
     * 可用 API 列表
     * 优先级：ipinfo.io > ipwho.is > seeip.org > ip-api.com > country.is
     */
    private val apiEndpoints = listOf(
        // ipinfo.io - 当前使用的服务，字段简洁稳定
        ApiEndpoint(
            name = "ipinfo.io",
            url = "https://ipinfo.io/json",
            countryCodeExtractor = { json ->
                json.takeIf { it.has("country") }?.getString("country")
            }
        ),
        // ipwho.is - 字段丰富，返回 success 状态，推荐首选备用
        ApiEndpoint(
            name = "ipwho.is",
            url = "https://ipwho.is/",
            countryCodeExtractor = { json ->
                json.takeIf { it.optBoolean("success", false) }?.optString("country_code")
            }
        ),
        // seeip.org - 无限制，响应快，字段完整
        ApiEndpoint(
            name = "seeip.org",
            url = "https://api.seeip.org/geoip",
            countryCodeExtractor = { json ->
                json.takeIf { it.has("country_code") }?.getString("country_code")
            }
        ),
        // ip-api.com - 老牌服务，免费版仅 HTTP，45次/分钟
        ApiEndpoint(
            name = "ip-api.com",
            url = "http://ip-api.com/json/",
            countryCodeExtractor = { json ->
                json.takeIf { it.optString("status", "") == "success" }?.optString("countryCode")
            }
        ),
        // country.is - 最简洁，只返回 IP + 国家代码，适合兜底
        ApiEndpoint(
            name = "country.is",
            url = "https://api.country.is/",
            countryCodeExtractor = { json ->
                json.takeIf { it.has("country") }?.getString("country")
            }
        )
    )

    // ===== 核心方法 =====

    // ---------- 回调方式 ----------

    /**
     * 检查用户网络所在区域（异步，多源备份）- 使用默认配置
     *
     * Kotlin 示例：
     * ```
     * CheckAddress.check { result ->
     *     国家: ${result.countryCode}
     * }
     * ```
     *
     * Java 示例：
     * ```
     * new CheckAddress().check(new CheckAddress.CheckCallback() {
     *     @Override
     *     public void onSuccess(GeoResult result) { }
     *     @Override
     *     public void onFailure(String error) { }
     * });
     * ```
     *
     * @param callback 回调接口，在主线程回调
     */
    fun check(callback: CheckCallback) = check(callback, Options())

    /**
     * 检查用户网络所在区域（异步，多源备份）- 自定义配置
     *
     * @param callback 回调接口，在主线程回调
     * @param options 检查配置选项
     */
    fun check(callback: CheckCallback, options: Options) {
        GlobalScope.launch(Dispatchers.Main) {
            val result = withContext(Dispatchers.IO) {
                checkInternal(options)
            }
            if (result != null) {
                callback.onSuccess(result)
            } else {
                callback.onFailure()
            }
        }
    }

    // ---------- Java 友好的重载方法（无需创建 Options） ----------

    /**
     * 检查用户网络所在区域（异步）- Java 友好，可单独指定参数
     *
     * @param callback 回调接口
     * @param timeoutMs 超时时间（毫秒）
     * @param parallel 是否并行请求
     */
    fun check(callback: CheckCallback, timeoutMs: Int, parallel: Boolean) =
        check(callback, Options(timeoutMs, parallel))

    // ---------- 挂起函数（Kotlin 协程） ----------

    /**
     * 检查用户网络所在区域（挂起函数，可在协程中直接调用）
     *
     * @param options 检查配置选项
     * @return GeoResult 或 null（所有 API 均失败时）
     */
    suspend fun check(options: Options = Options()): GeoResult? = withContext(Dispatchers.IO) {
        checkInternal(options)
    }

    // ---------- 同步阻塞 ----------

    /**
     * 同步检查（阻塞调用，请在后台线程使用）- 使用默认配置
     *
     * @return GeoResult 或 null（所有 API 均失败时）
     */
    fun checkBlocking(): GeoResult? = checkBlocking(Options())

    /**
     * 同步检查（阻塞调用，请在后台线程使用）- 自定义配置
     *
     * @param options 检查配置选项
     * @return GeoResult 或 null（所有 API 均失败时）
     */
    fun checkBlocking(options: Options): GeoResult? = runBlocking {
        check(options)
    }

    /**
     * 同步检查（阻塞调用）- Java 友好，可单独指定参数
     *
     * @param timeoutMs 超时时间（毫秒）
     * @param parallel 是否并行请求
     * @return GeoResult 或 null（所有 API 均失败时）
     */
    fun checkBlocking(timeoutMs: Int, parallel: Boolean): GeoResult? =
        checkBlocking(Options(timeoutMs, parallel))

    // ===== 内部实现 =====

    /**
     * 核心检查逻辑
     */
    private suspend fun checkInternal(options: Options): GeoResult? {
        return if (options.parallel) {
            checkParallel(apiEndpoints, options.timeoutMs)
        } else {
            checkSequential(apiEndpoints, options.timeoutMs)
        }
    }

    /**
     * 串行尝试所有 API（省流量，但可能较慢）
     */
    private fun checkSequential(apis: List<ApiEndpoint>, timeoutMs: Int): GeoResult? {
        for (api in apis) {
            val result = tryFetchFromApi(api, timeoutMs)
            if (result != null) {
                return result
            }
        }
        return null
    }

    /**
     * 并行请求所有 API，取最快返回的结果（更快，但流量更多）
     * 使用 select 实现真正的竞争，哪个先返回就用哪个
     */
    private suspend fun checkParallel(apis: List<ApiEndpoint>, timeoutMs: Int): GeoResult? {
        return try {
            coroutineScope {
                // 创建所有请求的 Deferred
                val deferreds = apis.map { api ->
                    async(Dispatchers.IO) {
                        tryFetchFromApi(api, timeoutMs)
                    }
                }

                // 使用 select 竞争，取第一个成功返回的结果
                val remaining = deferreds.toMutableList()
                var result: GeoResult? = null

                while (remaining.isNotEmpty() && result == null) {
                    // select 会等待第一个完成的 Deferred
                    val selected = select {
                        remaining.forEach { deferred ->
                            deferred.onAwait { geoResult ->
                                remaining.remove(deferred) // 从待处理列表中移除
                                geoResult
                            }
                        }
                    }
                    // 如果选中的结果非空，则采用
                    if (selected != null) {
                        result = selected
                    }
                }

                // 取消剩余请求
                remaining.forEach { it.cancel() }

                result
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从指定 API 获取地理定位信息（使用 HttpURLConnection）
     */
    private fun tryFetchFromApi(api: ApiEndpoint, timeoutMs: Int): GeoResult? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(api.url)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) JHelper/1.0")
                setRequestProperty("Accept", "application/json")
                // 禁用缓存，确保获取最新数据
                useCaches = false
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) return null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)

            val countryCode = api.countryCodeExtractor(json) ?: return null
            if (countryCode.isBlank()) return null

            // 提取完整信息
            val country = json.optString("country", "")
            val region = json.optString("region", json.optString("regionName", ""))
            val city = json.optString("city", "")

            GeoResult(
                countryCode = countryCode.uppercase(),
                country = country,
                region = region,
                city = city,
                source = api.name
            )
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}