package top.jessi.jhelper.time

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Formatter
import java.util.Locale
import java.util.TimeZone

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
     * @return 格式化时间
     */
    @JvmStatic
    fun timestampToDate(millisecond: Long, format: String?): String {
        var tempFormat = format
        if (TextUtils.isEmpty(tempFormat)) tempFormat = "yyyy-MM-dd HH:mm:ss"
        val sdf = SimpleDateFormat(tempFormat, Locale.getDefault())
        return sdf.format(Date(millisecond))
    }

    /**
     * 日期格式字符串转换成时间戳
     *
     * @param date   字符串日期
     * @param format 如：20240923190000 +0800  ---  yyyyMMddHHmmss Z  ---  2024年09月23日19时00分00秒 在+8时区的时间
     * @return 时间戳 --- 精确到毫秒
     */
    @JvmStatic
    fun dateToTimestamp(date: String?, format: String?): Long {
        if (date.isNullOrBlank()) return 0
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        try {
            val parsedDate = sdf.parse(date)
            if (parsedDate != null) return parsedDate.time
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return 0
    }

    /**
     * 格式化时间 小时:分钟:秒
     *
     * @param millisecond 时间 --- 精确到毫秒
     * @return 格式化后的字符串
     */
    @JvmStatic
    fun formatDuration(millisecond: Int): String {
        val mFormatter = Formatter(Locale.getDefault())
        val totalSeconds = millisecond / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            mFormatter.format("%02d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    /**
     * 获取时区 如 +8  -3
     *
     * @param timeZoneID 时区对象ID
     * @return 对应时区
     */
    @JvmStatic
    fun getNumberTimeZone(timeZoneID: String?): Int {
        val timeZone = TimeZone.getTimeZone(timeZoneID)
        // 获取与0时区的时间戳偏移量
        val offset = timeZone.getOffset(System.currentTimeMillis())
        return offset / (3600 * 1000)
    }
}