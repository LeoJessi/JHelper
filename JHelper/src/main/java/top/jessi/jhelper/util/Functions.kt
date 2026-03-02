package top.jessi.jhelper.util

import android.Manifest.permission
import android.app.UiModeManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import androidx.annotation.RequiresPermission
import java.io.IOException
import java.net.ServerSocket
import java.util.Locale
import java.util.regex.Pattern


/**
 * Created by Jessi on 2024/10/31 10:49
 * Email：17324719944@189.cn
 * Describe：常用工具类
 */
object Functions {

    /**
     * 判断当前线程是否在主线程
     */
    @JvmStatic
    fun inMainThread() = Looper.getMainLooper().thread == Thread.currentThread()

    /**
     * 判断网络是否连接
     *
     * @param context 上下文
     * @return 0：未连接    1：有线    2：无线    3：其他
     */
    @JvmStatic
    @RequiresPermission(anyOf = [permission.ACCESS_NETWORK_STATE])
    fun getNetworkStatus(context: Context): Int {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT < 23) {
            val networkInfo = cm.activeNetworkInfo
            if (networkInfo != null) {
                when (networkInfo.type) {
                    ConnectivityManager.TYPE_ETHERNET -> return 1
                    ConnectivityManager.TYPE_WIFI -> return 2
                    ConnectivityManager.TYPE_MOBILE -> return 3
                }
            }
        } else {
            val network = cm.activeNetwork
            if (network != null) {
                val nc = cm.getNetworkCapabilities(network)
                if (nc != null) {
                    if (nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        return 1
                    } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return 2
                    } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        return 3
                    }
                }
            }
        }
        return 0
    }

    /**
     * 格式化文件大小
     *
     * @param size 字节大小
     * @return 格式化大小
     */
    @JvmStatic
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val fSize: Float
        val unit: String
        if (size < 1024) {
            fSize = size.toFloat()
            unit = " B"
        } else if (size < 1024 * 1024) {
            fSize = size / 1024F
            unit = " K"
        } else if (size < 1024 * 1024 * 1024) {
            fSize = size / (1024 * 1024F)
            unit = " M"
        } else {
            fSize = size / (1024 * 1024 * 1024F)
            unit = " G"
        }
        // 之所以不用Locale.getDefault()是因为有一些语言貌似没有 . 符号?  结果显示成了逗号,
        return String.format(Locale.ENGLISH, "%.1f", fSize) + unit
    }

    /**
     * 检查端口是否可用
     */
    @JvmStatic
    fun checkPortAvailable(port: Int): Boolean {
        // 尝试绑定到指定端口
        try {
            // 成功绑定，说明端口可用
            ServerSocket(port).use { return true }
        } catch (e: IOException) {
            // 绑定失败，端口可能被占用
            return false
        }
    }

    /**
     * 判断当前设备是否是TV设备
     */
    @JvmStatic
    fun isTv(context: Context): Boolean {
        // 1. 检查系统特性
        val pm = context.packageManager
        val isFeatureTv = pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
        val isFeatureLeanback = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        // 2. 检查 UI 模式
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isTvMode = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        // // 3. 检查输入设备
        // val hasTouchScreen = pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        // // 是否支持导航 如DPAD_UP
        // val hasDpad = context.resources.configuration.navigation == Configuration.NAVIGATION_DPAD
        // 判断逻辑
        // return (isFeatureTv || isFeatureLeanback || isTvMode) && (!hasTouchScreen && hasDpad)

        return (isFeatureTv || isFeatureLeanback || isTvMode)
    }

    /**
     * 根据包名打开apk
     */
    @JvmStatic
    fun openApp(context: Context, pkg: String): Boolean = try {
        val pm = context.packageManager
        var intent = pm.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            /*
             * 获取AndroidTV上Leanback的启动方式
             * 有的ATV软件只注册了 <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
             * 所以上面 <category android:name="android.intent.category.LAUNCHER" /> 搜索不到
             * */
            intent = pm.getLeanbackLaunchIntentForPackage(pkg)
        }
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }


    /**
     * 打开具体包的具体类
     * <p>
     * 当从外部想要打开具体活动时，需要添加以下，包括同一个程序中使用工具类调用也一样
     * intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
     *
     * @param context 上下文
     * @param pkg     包名
     * @param clazz   类名
     */
    @JvmStatic
    fun openClass(context: Context, pkg: String, clazz: String): Boolean = try {
        val intent = Intent()
        val componentName = ComponentName(pkg, clazz)
        intent.component = componentName
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    /**
     * 打开系统wifi设置
     */
    @JvmStatic
    fun openWifiSettings(context: Context) = try {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    /**
     * 打开系统设置
     */
    @JvmStatic
    fun openSystemSettings(context: Context) = try {
        val intent = Intent(Settings.ACTION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    /**
     * 判断字符串是否是数字
     * @param str   要判断的字符串
     */
    @JvmStatic
    fun isNumber(str: String): Boolean {
        val regexInt = "^-?[1-9]\\d*$"
        val regexDouble = "^-?([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0+|0)$"
        val isInt = Pattern.compile(regexInt).matcher(str).find()
        val isDouble = Pattern.compile(regexDouble).matcher(str).find()
        return isInt || isDouble
    }

    /**
     * 字符串补零
     *
     * @param str     原有字符串
     * @param len  需要补齐长度
     * @param isFront 是否补在前面
     * @return 已补零字符串
     */
    @JvmStatic
    fun addZeros(str: String, len: Int, isFront: Boolean): String {
        val sb = StringBuilder()
        if (isFront) {
            for (i in 1..len - str.length) {
                sb.append("0")
            }
            sb.append(str)
        } else {
            sb.append(str)
            for (i in 1..len - str.length) {
                sb.append("0")
            }
        }
        return sb.toString()
    }

    /**
     * 校验url
     */
    @JvmStatic
    fun checkUrl(url: String): Boolean {
        return !TextUtils.isEmpty(url) && !url.contains(" ") && url.matches("^https?://.*$".toRegex())
    }

    /**
     * 提取字符串中的协议部分 （协议 主机名 端口）
     */
    @JvmStatic
    fun extractHost(url: String): String {
        val urlPattern = "^(https?://[^/?#]+)"
        val pattern = Pattern.compile(urlPattern)
        val matcher = pattern.matcher(url)
        return if (matcher.find()) matcher.group() else ""
    }

    /**
     * 提取字符串中的数字
     */
    @JvmStatic
    fun extractNumber(str: String): String {
        val urlPattern = "-?\\d+(\\.\\d+)?"
        val pattern = Pattern.compile(urlPattern)
        val matcher = pattern.matcher(str)
        return if (matcher.find()) matcher.group() else ""
    }

}