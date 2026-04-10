package top.jessi.jhelper.time

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong

/**
 * Created by Jessi on 2022/11/22 16:49
 * Email：17324719944@189.cn
 * Describe：时间工具类
 *
 *
 * #################################时间格式化 START########################################
 * ### 日期部分
 * - `Y` - 周年（与 `y` 相同，但用于 ISO 8601 计算）
 * - `y` - 年份（例如：2024）
 * - `M` - 月份（1-12）
 * - `d` - 一个月中的天数（1-31）
 * - `D` - 一年中的天数（1-366）
 * - `E` - 星期几的名称（例如：星期一）
 * - `F` - 月份的完整名称（例如：十月）
 * - `w` - 一年中的周数（1-52）
 * ### 时间部分
 * - `H` - 小时（0-23）
 * - `h` - 小时（1-12，AM/PM）
 * - `m` - 分钟（0-59）
 * - `s` - 秒（0-59）
 * - `S` - 毫秒（0-999）
 * - `a` - AM/PM 标记
 * ### 时区
 * - `z` - 时区的通用缩写形式（例如：PST, CST, EST）
 * - `Z` - 时区的 RFC 822 时区表示法（例如：-0800, +0100）
 * - `X` - ISO 8601 时区（例如：+01:00, Z, -08:00）
 * ### 其他
 * - `G` - 时代标记（AD 或 BC）
 * - `k` - 小时（1-24）
 * - `K` - 小时（0-11，AM/PM）
 * - `L` - 十分之一秒（0-9）
 * - `N` - 纳秒（0-999,999,999）
 * - `Q` - 季度（1-4）
 * - `W` - 一个月中的周数（1-4）
 * - `u` - 一年中的周数（1-53），根据 ISO 8601 标准
 * - `EEEE` - 星期几的全称（例如：Sunday, Monday）
 * - `F` - 月份的完整名称（例如：January, February）
 * #################################时间格式化 END########################################
 */
object Time {


    /** 当前时间戳（毫秒） */
    @JvmStatic
    fun currentTimeMillis(): Long = System.currentTimeMillis()

    /** 当前时间戳（秒） */
    @JvmStatic
    fun currentTimeSeconds(): Long = System.currentTimeMillis() / 1000

    /**
     * 扩展函数 将小时数转换为毫秒（milliseconds）。
     *
     * 示例：
     * - 1 → 3600000L
     * - 2 → 7200000L
     *
     * 适用场景：
     * - 时间间隔计算（如回播时长、缓存时间等）
     * - 3.hours / Time.getHours(3)
     * @return 对应的毫秒值
     */
    @JvmStatic
    val Int.hour get() = this * 3_600_000L

    @JvmStatic
    val Double.hour get() = (this * 3_600_000L).roundToLong()

    /**
     * 将分钟数转换为毫秒（milliseconds）。
     */
    @JvmStatic
    val Int.minute get() = this * 60_000L

    @JvmStatic
    val Double.minute get() = (this * 60_000L).roundToLong()

    /**
     * 将秒数转换为毫秒（milliseconds）。
     */
    @JvmStatic
    val Int.second get() = this * 1_000L

    @JvmStatic
    val Double.second get() = (this * 1_000L).roundToLong()

    /**
     * 计算时间差
     */
    @JvmStatic
    fun Long.diffNow(): Long {
        return System.currentTimeMillis() - this
    }

    /**
     * 获取当前系统是否是24小时制
     *
     * @param context 上下文
     * @return 是否24小时制
     */
    @JvmStatic
    fun getIs24HourFormat(context: Context): Boolean {
        val hourFormat = Settings.System.getString(context.contentResolver, Settings.System.TIME_12_24)
        return "24" == hourFormat
    }

    /**
     * 获取当前时间
     *
     * @param format 时间格式
     * @return 格式化后的时间
     */
    @JvmStatic
    fun getCurrent(format: String?): String {
        if (format.isNullOrBlank()) return ""
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /*版本允许则使用此方法   线程更安全*/
            val localDateTime = LocalDateTime.now() // 获取当前时间
            val dateTimeFormatter = DateTimeFormatter.ofPattern(format)
            localDateTime.format(dateTimeFormatter)
        } else {
            val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
            val date = Date(System.currentTimeMillis()) // 获取当前时间戳
            simpleDateFormat.format(date)
        }
    }

    /**
     * 时间戳转换成日期格式字符串
     *
     * @param millisecond 精确到毫秒
     * @param format      格式
     * @param timeZoneId 时区 - 默认使用设备时区
     * @return 格式化时间
     */
    @JvmStatic
    @JvmOverloads
    fun formatTimestamp(millisecond: Long, format: String = "yyyy-MM-dd HH:mm:ss", timeZoneId: String = TimeZone.getDefault().id): String {
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone(timeZoneId)
        return sdf.format(Date(millisecond))
    }

    @JvmStatic
    fun formatTimestamp(millisecond: Long, format: String = "yyyy-MM-dd HH:mm:ss", timeZone: TimeZone = TimeZone.getDefault()): String {
        return formatTimestamp(millisecond, format, timeZone.id)
    }

    /**
     * 日期格式字符串转换成时间戳
     *
     * @param date   字符串日期 -- 20240923190000 +0800
     * @param format 如：20240923190000 +0800  ---  yyyyMMddHHmmss Z  ---  2024年09月23日19时00分00秒 在+8时区的时间
     * @return 时间戳 --- 精确到毫秒
     */
    @JvmStatic
    fun parseToTimestamp(date: String?, format: String?): Long {
        if (date.isNullOrBlank()) return -1
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        try {
            val parsedDate = sdf.parse(date)
            if (parsedDate != null) return parsedDate.time
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return -1
    }

    /**
     * 将毫秒转换为播放时长字符串（mm:ss / HH:mm:ss）
     *
     * 示例：
     * - 65000L   → "01:05"
     * - 3605000L → "01:00:05"
     *
     * 适用场景：
     * - 播放器进度显示
     * - 音视频时长格式化
     */
    @JvmStatic
    fun Long.formatDuration(): String {
        val absMillis = kotlin.math.abs(this)
        val totalSeconds = absMillis / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        val result = if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
        return if (this < 0) "-$result" else result
    }

    /**
     * 获取指定时区的 GMT 偏移字符串（用于展示）。
     *
     * 示例：
     * - "+08:00"（中国标准时间）
     * - "-05:00"（美国东部时间）
     * - "+05:30"（印度标准时间）
     *
     * 说明：
     * - 该方法用于 UI 展示或日志输出
     * - 不建议用于数值计算
     *
     * @param timeZone 目标时区，默认使用系统时区（TimeZone.getDefault()）
     * @return GMT 偏移字符串，格式为 ±HH:mm
     *
     * @see getGmtOffsetHours
     */
    @JvmStatic
    @JvmOverloads
    fun getGmtOffsetString(timeZone: TimeZone = TimeZone.getDefault()): String {
        val offsetMinutes = timeZone.getOffset(System.currentTimeMillis()) / 60000
        val hours = offsetMinutes / 60
        val minutes = kotlin.math.abs(offsetMinutes % 60)
        return "%+d:%02d".format(hours, minutes)
    }

    /**
     * 获取指定时区的 GMT 偏移（以小时为单位，支持小数）。
     *
     * 示例：
     * - 8.0   （GMT+08:00）
     * - -5.0  （GMT-05:00）
     * - 5.5   （GMT+05:30）
     *
     * 说明：
     * - 该方法用于计算或业务逻辑处理
     * - 不建议直接用于 UI 展示（格式不规范）
     *
     * @param timeZone 目标时区，默认使用系统时区（TimeZone.getDefault()）
     * @return GMT 偏移值（单位：小时，可能包含小数）
     *
     * @see getGmtOffsetString
     */
    @JvmStatic
    @JvmOverloads
    fun getGmtOffsetHours(timeZone: TimeZone = TimeZone.getDefault()): Double {
        val offset = timeZone.getOffset(System.currentTimeMillis())
        return offset / (60.0 * 60 * 1000)
    }
}