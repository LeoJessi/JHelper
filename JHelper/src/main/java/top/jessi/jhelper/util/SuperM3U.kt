package top.jessi.jhelper.util

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * Created by Jessi on 2026/2/26 14:28
 * Email：17324719944@189.cn
 * Describe：M3U文件解析
 *
 * #EXTINF:<duration> [tvg-id="<id>"] [tvg-name="<name>"] [tvg-logo="<logo_url>"] [group-title="<group_name>"] [tvg-language="<language>"] [tvg-country="<country>"] [tvg-region="<region>"],<channel_name>
 * <url>
 */
object SuperM3U {

    /* ===================== 数据模型 ===================== */

    /**
     * 频道数据
     */
    data class Channel(
        var name: String = "",     // 频道名称
        var url: String = "",      // 播放地址
        var group: String = "",    // 分组
        var logo: String = "",  // logo
        var tvgId: String = ""  // tvg-id
    )

    /* ===================== 主解析入口 ===================== */

    /**
     * 解析 M3U
     *
     * @param source 本地路径 或 网络地址
     * @param removeDuplicate 是否去重（推荐开启）
     *
     * @return Map<分组, 频道列表>
     */
    @JvmStatic
    fun parse(source: String, removeDuplicate: Boolean = true): Map<String, List<Channel>> {
        // 最终结果：按 group 分组
        val result = HashMap<String, MutableList<Channel>>()
        // URL 去重（避免直播源重复）
        val urlSet = if (removeDuplicate) HashSet<String>() else null
        // 打开文件或网络流
        openReader(source).use { reader ->
            var line: String?
            var current: Channel? = null
            // 按行读取，避免 OOM
            while (reader.readLine().also { line = it } != null) {
                val text = line!!.trim()
                if (text.isEmpty()) continue
                if (text.startsWith("#EXTINF")) {
                    // EXTINF 行（频道信息）
                    current = parseExtInf(text)
                } else if (!text.startsWith("#")) {
                    // URL 行
                    current?.let { ch ->
                        ch.url = processUrl(text)  // 处理 URL
                        // 去重（大量直播源非常重要）
                        if (urlSet != null) {
                            if (!urlSet.add(ch.url)) {
                                current = null
                                return@let
                            }
                        }
                        // 分组（没有 group-title 就归到 Other）
                        val group = ch.group.ifEmpty { "Default" }
                        val list = result.getOrPut(group) { mutableListOf() }
                        list.add(ch)
                    }
                    current = null
                }
            }
        }
        return result
    }

    /* ===================== EXTINF 解析 ===================== */
    /**
     * 解析 #EXTINF 行
     *
     * 示例：
     * #EXTINF:-1 tvg-id="cctv1" group-title="News",CCTV1
     */
    private fun parseExtInf(line: String): Channel {
        val ch = Channel()
        // 频道名称（逗号后面）
        val comma = line.indexOf(",")
        if (comma != -1) {
            ch.name = line.substring(comma + 1).trim()
        }
        // 提取属性
        ch.group = find(line, "group-title") ?: "Default"
        ch.logo = find(line, "tvg-logo") ?: ""
        ch.tvgId = find(line, "tvg-id") ?: ""
        return ch
    }

    /**
     * 提取 EXTINF 属性
     */
    private fun find(line: String, key: String): String? {
        val start = line.indexOf("$key=\"")
        if (start == -1) return null
        val end = line.indexOf("\"", start + key.length + 2)
        if (end == -1) return null
        return line.substring(start + key.length + 2, end)
    }


    /* ===================== URL 处理 ===================== */

    /**
     * 处理播放 URL，加入 @ 符号（针对 udp:// 和 rtp://）
     */
    private fun processUrl(playUrl: String): String {
        var updatedUrl = playUrl
        // 如果 URL 是 udp:// 或 rtp:// 且不包含 '@'，则加上 '@'
        if (updatedUrl.startsWith("udp://") && !updatedUrl.contains("@")) {
            updatedUrl = updatedUrl.replaceFirst("udp://", "udp://@")
        }

        if (updatedUrl.startsWith("rtp://") && !updatedUrl.contains("@")) {
            updatedUrl = updatedUrl.replaceFirst("rtp://", "rtp://@")
        }
        return updatedUrl
    }

    /* ===================== Reader 支持 ===================== */
    /**
     * 支持：
     * 1. 本地文件
     * 2. http / https
     * 3. gzip 压缩
     */
    private fun openReader(source: String): BufferedReader {
        val input: InputStream = when {
            source.startsWith("http") -> {
                URL(source).openStream()
            }

            else -> {
                FileInputStream(File(source))
            }
        }
        // gzip 支持
        val stream = if (source.endsWith(".gz")) {
            GZIPInputStream(input)
        } else {
            input
        }
        // 32KB 缓冲，性能更好
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8), 32 * 1024)
    }
}