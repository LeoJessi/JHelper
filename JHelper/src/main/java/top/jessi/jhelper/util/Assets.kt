package top.jessi.jhelper.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Typeface
import android.util.Log
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Jessi on 2026/6/5 16:42
 * Email：17324719944@189.cn
 * Describe：对Assets进行操作的工具类
 */
object Assets {

    private const val TAG = "JHelper-Assets"
    private const val BUFFER_SIZE = 8192

    // 缓存Asset文件列表，避免重复读取
    private val assetListCache = ConcurrentHashMap<String, Array<String>>()

    // 缓存字体，避免重复加载
    private val fontCache = ConcurrentHashMap<String, Typeface>()

    /* ========================= 基础读取操作 ========================= */

    /**
     * 读取文件内容为字符串
     *
     * @param context 上下文
     * @param assetPath Asset文件路径
     * @return 文件内容字符串，失败返回null
     */
    @JvmStatic
    fun readFile(context: Context, assetPath: String): String? {
        return try {
            context.assets.open(assetPath).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val sb = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line).append("\n")
                    }
                    sb.toString().trimEnd('\n')
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "读取Asset文件失败: $assetPath", e)
            null
        }
    }

    /**
     * 读取文件并解析为指定类型的对象
     *
     * @param context 上下文
     * @param assetPath Asset文件路径
     * @param type 目标类型
     * @return 解析后的对象，失败返回null
     */
    @JvmStatic
    fun <T> readFile(context: Context, assetPath: String, type: Class<T>): T? {
        return try {
            val jsonString = readFile(context, assetPath)
            if (jsonString.isNullOrEmpty()) {
                Log.w(TAG, "读取Asset文件内容为空: $assetPath")
                return null
            }
            IGson.fromJson(jsonString, type)
        } catch (e: Exception) {
            Log.w(TAG, "解析Asset文件失败: $assetPath -> ${type.simpleName}", e)
            null
        }
    }

    /**
     * 读取文件并解析为指定类型的对象（支持泛型）
     *
     * @param context 上下文
     * @param assetPath Asset文件路径
     * @param typeToken 类型标记
     * @return 解析后的对象，失败返回null
     */
    @JvmStatic
    fun <T> readFile(context: Context, assetPath: String, typeToken: TypeToken<T>): T? {
        return try {
            val jsonString = readFile(context, assetPath)
            if (jsonString.isNullOrEmpty()) {
                Log.w(TAG, "读取Asset文件内容为空: $assetPath")
                return null
            }
            IGson.fromJson(jsonString, typeToken.type)
        } catch (e: Exception) {
            Log.w(TAG, "解析Asset文件失败: $assetPath -> ${typeToken.type}", e)
            null
        }
    }



    /**
     * 读取文件为字节数组
     *
     * @param context 上下文
     * @param assetPath Asset文件路径
     * @return 字节数组，失败返回null
     */
    @JvmStatic
    fun readBytes(context: Context, assetPath: String): ByteArray? {
        return try {
            context.assets.open(assetPath).use { inputStream ->
                val buffer = ByteArrayOutputStream()
                val data = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (inputStream.read(data).also { bytesRead = it } != -1) {
                    buffer.write(data, 0, bytesRead)
                }
                buffer.toByteArray()
            }
        } catch (e: IOException) {
            Log.w(TAG, "读取Asset字节失败: $assetPath", e)
            null
        }
    }


    /**
     * 获取文件的InputStream
     *
     * @param context 上下文
     * @param assetPath Asset文件路径
     * @return InputStream，失败返回null
     */
    @JvmStatic
    fun openStream(context: Context, assetPath: String): InputStream? {
        return try {
            context.assets.open(assetPath)
        } catch (e: IOException) {
            Log.w(TAG, "打开Asset流失败: $assetPath", e)
            null
        }
    }

    /**
     * 获取文件的AssetFileDescriptor
     *
     * @param context 上下文
     * @param assetPath Asset文件路径
     * @return AssetFileDescriptor，失败返回null
     */
    @JvmStatic
    fun openFileDescriptor(context: Context, assetPath: String): AssetFileDescriptor? {
        return try {
            context.assets.openFd(assetPath)
        } catch (e: IOException) {
            Log.w(TAG, "打开Asset文件描述符失败: $assetPath", e)
            null
        }
    }


    /* ========================= 文件操作 ========================= */

    /**
     * 复制文件到指定路径
     *
     * @param context 上下文
     * @param assetPath Asset文件路径
     * @param destPath 目标文件路径
     * @return true表示成功，false表示失败
     */
    @JvmStatic
    fun copyTo(context: Context, assetPath: String, destPath: String): Boolean {
        return try {
            context.assets.open(assetPath).use { inputStream ->
                FileOutputStream(destPath).use { outputStream ->
                    copyStream(inputStream, outputStream)
                }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "复制Asset文件失败: $assetPath -> $destPath", e)
            false
        }
    }

    /**
     * 复制目录到指定目录
     *
     * @param context 上下文
     * @param assetDir Asset目录路径
     * @param destDir 目标目录路径
     * @return true表示成功，false表示失败
     */
    @JvmStatic
    fun copyDirectory(context: Context, assetDir: String, destDir: String): Boolean {
        return try {
            val assetFiles = listFiles(context, assetDir)
            if (assetFiles.isEmpty()) {
                Log.w(TAG, "Asset目录为空: $assetDir")
                return false
            }

            val destDirFile = File(destDir)
            if (!destDirFile.exists()) {
                destDirFile.mkdirs()
            }

            var successCount = 0
            for (assetFile in assetFiles) {
                val fullAssetPath = if (assetDir.isEmpty()) assetFile else "$assetDir/$assetFile"
                val fullDestPath = "$destDir/$assetFile"

                // 如果是目录，递归复制
                if (isDirectory(context, fullAssetPath)) {
                    if (copyDirectory(context, fullAssetPath, fullDestPath)) {
                        successCount++
                    }
                } else {
                    // 如果是文件，直接复制
                    if (copyTo(context, fullAssetPath, fullDestPath)) {
                        successCount++
                    }
                }
            }

            val success = successCount == assetFiles.size
            success
        } catch (e: Exception) {
            Log.w(TAG, "复制Asset目录失败: $assetDir -> $destDir", e)
            false
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param context 上下文
     * @param assetPath Asset文件路径
     * @return true表示存在，false表示不存在
     */
    @JvmStatic
    fun exists(context: Context, assetPath: String): Boolean {
        return try {
            context.assets.open(assetPath).use { true }
        } catch (e: IOException) {
            false
        }
    }


    /* ========================= 目录操作 ========================= */

    /**
     * 列出目录下的所有文件
     *
     * @param context 上下文
     * @param assetDir Asset目录路径
     * @return 文件名数组，失败返回空数组
     */
    @JvmStatic
    fun listFiles(context: Context, assetDir: String): Array<String> {
        return try {
            // 检查缓存
            val cacheKey = "${context.packageName}_$assetDir"
            assetListCache[cacheKey]?.let { return it }

            val files = context.assets.list(assetDir) ?: emptyArray()
            // 存入缓存
            assetListCache[cacheKey] = files
            files
        } catch (e: IOException) {
            Log.w(TAG, "列出Asset文件失败: $assetDir", e)
            emptyArray()
        }
    }

    /**
     * 列出目录下的文件（包含子目录）
     *
     * @param context 上下文
     * @param assetDir Asset目录路径
     * @return 文件路径列表
     */
    @JvmStatic
    fun listFilesRecursive(context: Context, assetDir: String): List<String> {
        val result = mutableListOf<String>()
        try {
            val files = listFiles(context, assetDir)
            for (file in files) {
                val fullPath = if (assetDir.isEmpty()) file else "$assetDir/$file"
                result.add(fullPath)

                // 如果是目录，递归列出
                if (isDirectory(context, fullPath)) {
                    result.addAll(listFilesRecursive(context, fullPath))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "递归列出Asset文件失败: $assetDir", e)
        }
        return result
    }


    /* ========================= 文件信息 ========================= */

    /**
     * 获取文件大小
     *
     * @param context 上下文
     * @param assetPath Asset文件路径
     * @return 文件大小（字节），失败返回-1
     */
    @JvmStatic
    fun getSize(context: Context, assetPath: String): Long {
        return try {
            context.assets.open(assetPath).use { inputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var totalSize = 0L
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    totalSize += bytesRead
                }
                totalSize
            }
        } catch (e: IOException) {
            Log.w(TAG, "获取Asset文件大小失败: $assetPath", e)
            -1
        }
    }

    /**
     * 获取文件的MIME类型
     *
     * @param context 上下文
     * @param assetPath Asset文件路径
     * @return MIME类型，失败返回null
     */
    @JvmStatic
    fun getMimeType(context: Context, assetPath: String): String? {
        return try {
            val inputStream = context.assets.open(assetPath)
            val mimeType = getMimeTypeFromStream(inputStream)
            inputStream.close()
            mimeType
        } catch (e: Exception) {
            Log.w(TAG, "获取Asset MIME类型失败: $assetPath", e)
            null
        }
    }


    /* ========================= 字体操作 ========================= */

    /**
     * 从Asset加载字体文件
     *
     * @param context 上下文
     * @param assetPath 字体文件路径（支持.ttf, .otf格式）
     * @return Typeface对象，失败返回null
     */
    @JvmStatic
    fun loadFont(context: Context, assetPath: String): Typeface? {
        // 检查缓存
        fontCache[assetPath]?.let { return it }

        return try {
            val typeface = Typeface.createFromAsset(context.assets, assetPath)
            // 存入缓存
            fontCache[assetPath] = typeface
            typeface
        } catch (e: Exception) {
            Log.w(TAG, "加载字体失败: $assetPath", e)
            null
        }
    }

    /**
     * 从Asset加载字体文件并指定样式
     *
     * @param context 上下文
     * @param assetPath 字体文件路径
     * @param style 字体样式（Typeface.NORMAL, Typeface.BOLD, Typeface.ITALIC, Typeface.BOLD_ITALIC）
     * @return Typeface对象，失败返回null
     */
    @JvmStatic
    fun loadFont(context: Context, assetPath: String, style: Int): Typeface? {
        val cacheKey = "${assetPath}_style_$style"
        fontCache[cacheKey]?.let { return it }

        return try {
            val typeface = Typeface.createFromAsset(context.assets, assetPath)
            val styledTypeface = Typeface.create(typeface, style)
            // 存入缓存
            fontCache[cacheKey] = styledTypeface
            styledTypeface
        } catch (e: Exception) {
            Log.w(TAG, "加载字体失败: $assetPath, style: $style", e)
            null
        }
    }

    /**
     * 从Asset目录批量加载所有字体文件
     *
     * @param context 上下文
     * @param fontDir 字体目录路径
     * @return Map<文件名, Typeface>
     */
    @JvmStatic
    fun loadFontsFromDirectory(context: Context, fontDir: String): Map<String, Typeface> {
        val fonts = mutableMapOf<String, Typeface>()
        try {
            val fontFiles = listFiles(context, fontDir)
            for (fontFile in fontFiles) {
                if (isFontFile(fontFile)) {
                    val fullPath = if (fontDir.isEmpty()) fontFile else "$fontDir/$fontFile"
                    val typeface = loadFont(context, fullPath)
                    if (typeface != null) {
                        fonts[fontFile] = typeface
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "批量加载字体失败: $fontDir", e)
        }
        return fonts
    }

    /**
     * 检查文件是否为字体文件
     *
     * @param fileName 文件名
     * @return true表示是字体文件
     */
    private fun isFontFile(fileName: String): Boolean {
        val lowerCaseName = fileName.lowercase()
        return lowerCaseName.endsWith(".ttf") ||
               lowerCaseName.endsWith(".otf") ||
               lowerCaseName.endsWith(".ttc") ||
               lowerCaseName.endsWith(".woff") ||
               lowerCaseName.endsWith(".woff2")
    }

    /**
     * 清除字体缓存
     */
    @JvmStatic
    fun clearFontCache() {
        fontCache.clear()
    }

    /* ========================= 批量操作 ========================= */

    /**
     * 批量复制多个文件
     *
     * @param context 上下文
     * @param assetPaths Asset文件路径列表
     * @param destDir 目标目录路径
     * @return true表示全部成功，false表示有失败
     */
    @JvmStatic
    fun copyMultiple(context: Context, assetPaths: List<String>, destDir: String): Boolean {
        if (assetPaths.isEmpty()) return false

        val destDirFile = File(destDir)
        if (!destDirFile.exists()) {
            destDirFile.mkdirs()
        }

        var successCount = 0
        for (assetPath in assetPaths) {
            val fileName = File(assetPath).name
            val destPath = "$destDir/$fileName"
            if (copyTo(context, assetPath, destPath)) {
                successCount++
            }
        }

        val success = successCount == assetPaths.size
        return success
    }

    /**
     * 批量复制文件（带进度回调）
     *
     * @param context 上下文
     * @param assetPath Asset文件路径
     * @param destPath 目标文件路径
     * @param progressCallback 进度回调函数（0-100）
     * @return true表示成功，false表示失败
     */
    @JvmStatic
    fun copyWithProgress(
        context: Context, assetPath: String,
        destPath: String, progressCallback: (progress: Int) -> Unit
    ): Boolean {
        return try {
            val inputStream = context.assets.open(assetPath)
            val outputStream = FileOutputStream(destPath)

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            var totalBytesRead = 0L
            val fileSize = getSize(context, assetPath)

            if (fileSize <= 0) {
                inputStream.close()
                outputStream.close()
                return false
            }

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                // 计算进度
                val progress = ((totalBytesRead * 100) / fileSize).toInt()
                progressCallback(progress.coerceIn(0, 100))
            }

            inputStream.close()
            outputStream.close()

            progressCallback(100) // 确保最终进度为100%
            true
        } catch (e: Exception) {
            Log.w(TAG, "带进度复制Asset文件失败: $assetPath -> $destPath", e)
            progressCallback(0) // 失败时重置进度
            false
        }
    }


    /* ========================= 私有辅助方法 ========================= */

    /**
     * 复制输入流到输出流
     */
    private fun copyStream(input: InputStream, output: OutputStream): Long {
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        var totalBytes: Long = 0
        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
            totalBytes += bytesRead
        }
        output.flush()
        return totalBytes
    }

    /**
     * 检查路径是否为目录
     */
    private fun isDirectory(context: Context, assetPath: String): Boolean {
        return try {
            val files = context.assets.list(assetPath)
            // 如果list返回非空数组，则认为是目录
            !files.isNullOrEmpty()
        } catch (e: IOException) {
            false
        }
    }

    /**
     * 从输入流获取MIME类型（简单实现）
     */
    private fun getMimeTypeFromStream(inputStream: InputStream): String? {
        return try {
            val bytes = ByteArray(8)
            val bytesRead = inputStream.read(bytes)
            if (bytesRead < 8) return null

            // 简单的文件签名检测
            when {
                bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
                bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "image/png"
                bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() -> "image/gif"
                bytes[0] == 0x25.toByte() && bytes[1] == 0x50.toByte() -> "application/pdf"
                bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() -> "application/zip"
                else -> "application/octet-stream"
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 清除Asset文件列表缓存
     */
    @JvmStatic
    fun clearCache() {
        assetListCache.clear()
    }
}