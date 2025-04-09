package top.jessi.jhelper.util

import android.Manifest.permission
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresPermission
import java.io.IOException
import java.net.ServerSocket
import java.util.Locale


/**
 * Created by Jessi on 2024/10/31 10:49
 * Email：17324719944@189.cn
 * Describe：常用工具类
 */
object Functions {

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

}