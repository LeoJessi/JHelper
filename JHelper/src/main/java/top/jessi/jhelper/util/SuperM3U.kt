package top.jessi.jhelper.util

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream
import javax.net.ssl.SSLException

/**
 * Created by Jessi on 2026/2/26 14:28
 * Email：17324719944@189.cn
 * Describe：M3U文件解析
 *
 * #EXTINF:<duration> [tvg-id="<id>"] [tvg-name="<name>"] [tvg-logo="<logo_url>"] [group-title="<group_name>"] [tvg-language="<language>"] [tvg-country="<country>"] [tvg-region="<region>"],<channel_name>
 * <url>
 */
object SuperM3U {
    private const val TAG = "SuperM3U"

    /* ================= 数据模型 ================= */

    data class Channel(
            var name: String = "",
            var url: String = "",
            var group: String = "",
            var logo: String = "",
            var tvgId: String = "",
            var tvgName: String = "",
            var vlcOptions: MutableList<String> = mutableListOf()
    )

    /* ================= EXTINF属性解析正则 ================= */

    private val ATTR_PATTERN = Pattern.compile("(\\w+-?\\w*)=\"(.*?)\"")

    /* ================= 主解析入口 ================= */

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
        val reader = openReader(source) ?: return emptyMap()
        // 最终结果：按 group 分组
        val result = LinkedHashMap<String, MutableList<Channel>>()
        // URL 去重（避免直播源重复）
        val urlSet = if (removeDuplicate) HashSet<String>() else null
        reader.use {
            var line: String?
            var current: Channel? = null
            // 按行读取，避免 OOM
            while (reader.readLine().also { line = it } != null) {
                val text = line!!.trim()
                if (text.isEmpty()) continue
                when {
                    text.startsWith("#EXTINF") -> {
                        current = parseExtInf(text)
                    }

                    text.startsWith("#EXTGRP") -> {
                        current?.group = text.substringAfter(":").trim()
                    }

                    text.startsWith("#EXTVLCOPT") -> {
                        current?.vlcOptions?.add(
                                text.substringAfter(":")
                        )
                    }

                    !text.startsWith("#") -> {
                        current?.let { ch ->
                            ch.url = processUrl(text)
                            // 去重（大量直播源非常重要）
                            if (urlSet != null) {
                                if (!urlSet.add(ch.url)) {
                                    current = null
                                    return@let
                                }
                            }
                            // 分组（没有 group-title 就归到 Default）
                            val group = ch.group.ifEmpty {
                                ch.group = "Default"
                                "Default"
                            }
                            val list = result.getOrPut(group) { ArrayList() }
                            list.add(ch)
                        }
                        current = null
                    }
                }
            }
        }
        return result
    }

    /* ================= EXTINF解析 ================= */
    private fun parseExtInf(line: String): Channel {
        val channel = Channel()
        // 频道名称（逗号后面）
        val comma = line.indexOf(',')
        if (comma != -1) {
            channel.name = line.substring(comma + 1).trim()
        }
        // 提取属性
        val matcher = ATTR_PATTERN.matcher(line)
        while (matcher.find()) {
            val key = matcher.group(1)
            val value = matcher.group(2)
            when (key) {
                "tvg-id" -> channel.tvgId = value ?: ""
                "tvg-name" -> channel.tvgName = value ?: ""
                "tvg-logo" -> channel.logo = value ?: ""
                "group-title" -> channel.group = value ?: "Default"
            }
        }
        return channel
    }

    /* ================= URL处理 ================= */

    /**
     * 处理播放 URL，加入 @ 符号（针对 udp:// 和 rtp://）
     */
    private fun processUrl(url: String): String {
        var playUrl = url
        // 如果 URL 是 udp:// 或 rtp:// 且不包含 '@'，则加上 '@'
        if (playUrl.startsWith("udp://") && !playUrl.contains("@")) {
            playUrl = playUrl.replaceFirst("udp://", "udp://@")
        }
        if (playUrl.startsWith("rtp://") && !playUrl.contains("@")) {
            playUrl = playUrl.replaceFirst("rtp://", "rtp://@")
        }
        return playUrl
    }

    /* ================= Reader创建 ================= */

    private fun openReader(source: String): BufferedReader? {
        return try {
            val input = if (source.startsWith("http")) {
                openHttpStream(source)
            } else {
                FileInputStream(File(source))
            }
            if (input == null) return null
            val stream = if (source.endsWith(".gz")) {
                GZIPInputStream(input)
            } else {
                input
            }
            BufferedReader(InputStreamReader(stream, detectCharset(stream)), 64 * 1024)
        } catch (e: Exception) {
            Log.w(TAG, "openReader error", e)
            null
        }
    }

    /* ================= HTTP请求 ================= */

    private fun openHttpStream(url: String): InputStream? {
        try {
            Log.w(TAG, "openHttpStream: $url")
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 10000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
            conn.setRequestProperty("Accept-Encoding", "gzip")
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "HTTP $code : $url")
                return null
            }
            val stream = conn.inputStream
            return if ("gzip".equals(conn.contentEncoding, true)) {
                GZIPInputStream(stream)
            } else {
                stream
            }
        } catch (e: SSLException) {
            // SSL失败时，尝试降级到 http
            if (url.startsWith("https://")) {
                val httpUrl = "http://" + url.removePrefix("https://")
                Log.w(TAG, "HTTPS fallback")
                return openHttpStream(httpUrl)
            }
        } catch (e: UnknownHostException) {
            Log.w(TAG, "DNS fail : $url")
        } catch (e: Exception) {
            Log.w(TAG, "HTTP error", e)
        }
        return null
    }

    /* ================= 编码识别 ================= */

    private fun detectCharset(input: InputStream): Charset {
        return try {
            input.mark(4096)
            val buffer = ByteArray(4096)
            val len = input.read(buffer)
            input.reset()
            if (len >= 3 && buffer[0] == 0xEF.toByte() && buffer[1] == 0xBB.toByte() && buffer[2] == 0xBF.toByte()) {
                return Charsets.UTF_8
            }
            val text = String(buffer, 0, len, Charsets.UTF_8)
            if (text.contains("#EXTM3U")) {
                Charsets.UTF_8
            } else {
                Charset.forName("GBK")
            }
        } catch (e: Exception) {
            Charsets.UTF_8
        }
    }
}