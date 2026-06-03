package top.jessi.jhelper.file

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.channels.FileChannel
import java.util.Locale

/**
 * Created by Jessi on 2023/4/1 15:08
 * Email：17324719944@189.cn
 * Describe：操作文件工具类
 */
object Files {

    private const val TAG = "Files"

    /* M3U 查询相关常量 */
    private val M3U_URI = MediaStore.Files.getContentUri("external")
    private val M3U_PROJECTION = arrayOf(MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.DISPLAY_NAME)
    private const val M3U_SELECTION = ("(${MediaStore.Files.FileColumns.MIME_TYPE} IN (?, ?, ?)"
            + " OR LOWER(${MediaStore.Files.FileColumns.DATA}) LIKE ?"
            + " OR LOWER(${MediaStore.Files.FileColumns.DATA}) LIKE ?)")
    private val M3U_SELECTION_ARGS =
        arrayOf("audio/x-mpegurl", "application/x-mpegurl", "audio/mpegurl", "%.m3u", "%.m3u8")

    /**
     * 判断文件是否存在
     *
     * @param path 文件路径
     * @return true表示文件存在，false表示文件不存在
     */
    @JvmStatic
    fun isExists(path: String): Boolean {
        return !TextUtils.isEmpty(path) && File(path).exists()
    }

    /**
     * 获取文件类型
     *
     * @param file 文件
     * @return 文件类型
     */
    @JvmStatic
    fun getFileMimeType(file: File): String {
        if (!file.exists() || file.isDirectory) {
            return "vnd.android.document/directory"
        }
        var extension = MimeTypeMap.getFileExtensionFromUrl(file.absolutePath)
        if (TextUtils.isEmpty(extension)) {
            val fileName = file.name
            val lastDotIndex = fileName.lastIndexOf(".")
            if (lastDotIndex != -1 && lastDotIndex < fileName.length - 1) {
                extension = fileName.substring(lastDotIndex + 1).lowercase(Locale.getDefault())
            }
        }
        if (!TextUtils.isEmpty(extension)) {
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (!mimeType.isNullOrEmpty()) {
                return mimeType
            }
        }
        return "application/octet-stream"
    }

    /**
     * 获取文件大小
     *
     * @param path 文件路径
     * @return 文件大小（单位：字节）
     */
    @JvmStatic
    fun getSize(path: String): Long {
        if (TextUtils.isEmpty(path)) return 0
        val file = File(path)
        return if (file.exists()) file.length() else 0
    }

    /**
     * 创建目录
     *
     * @param dirPath 目录路径
     * @return true：创建成功 false：创建失败
     */
    @JvmStatic
    fun createDir(dirPath: String): Boolean {
        var isSuccess = false
        val file = File(dirPath)
        if (!file.exists()) isSuccess = file.mkdirs()
        return isSuccess
    }

    /**
     * 创建文件
     *
     * @param filePath 文件路径
     * @return true：创建成功 false：创建失败
     */
    @JvmStatic
    fun create(filePath: String): Boolean {
        val file = File(filePath)
        if (file.exists()) return false
        try {
            val parentFile = file.parentFile
            if (parentFile != null && !parentFile.exists()) {
                if (!parentFile.mkdirs()) {
                    Log.w("Files", "create parent dir failed: ${parentFile.absolutePath}")
                    return false
                }
            }
            return file.createNewFile()
        } catch (e: IOException) {
            Log.w("Files", "create file failed: $filePath", e)
            return false
        }
    }

    /**
     * 关闭 Closeable 对象
     *
     * @param closeable Closeable 对象
     */
    @JvmStatic
    fun closeQuietly(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (e: IOException) {
            Log.w(TAG, "operation failed", e)
        }
    }

    /**
     * 复制文件
     *
     * @param sourceFilePath 资源文件路径
     * @param targetFilePath 目标文件路径
     * @return 是否复制成功
     */
    @JvmStatic
    fun copyFile(sourceFilePath: String, targetFilePath: String): Boolean {
        var sourceChannel: FileChannel? = null
        var targetChannel: FileChannel? = null
        var isSuccess = false
        try {
            val sourceFile = File(sourceFilePath)
            val targetFile = File(targetFilePath)
            if (!sourceFile.exists()) return false
            if (!isExists(targetFilePath)) if (!create(targetFilePath)) return false
            sourceChannel = FileInputStream(sourceFile).channel
            targetChannel = FileOutputStream(targetFile).channel
            sourceChannel.transferTo(0, sourceChannel.size(), targetChannel)
            isSuccess = true
        } catch (e: Exception) {
            Log.w(TAG, "operation failed", e)
        } finally {
            closeQuietly(sourceChannel)
            closeQuietly(targetChannel)
        }
        return isSuccess
    }

    /**
     * 移动文件（支持跨分区移动）
     *
     * @param sourceFilePath 资源文件路径
     * @param targetFilePath 目标文件路径
     * @return 是否移动成功
     */
    @JvmStatic
    fun moveFile(sourceFilePath: String, targetFilePath: String): Boolean {
        try {
            val sourceFile = File(sourceFilePath)
            if (!sourceFile.exists()) return false
            if (!isExists(targetFilePath)) if (!create(targetFilePath)) return false
            val targetFile = File(targetFilePath)
            // 优先使用 renameTo（同分区高效移动）
            if (sourceFile.renameTo(targetFile)) return true
            // renameTo 失败时（跨分区），回退到 copy + delete
            if (copyFile(sourceFilePath, targetFilePath)) {
                return delete(sourceFilePath)
            }
            return false
        } catch (e: Exception) {
            Log.w("Files", "move file failed: $sourceFilePath -> $targetFilePath", e)
            return false
        }
    }

    /**
     * 删除文件或目录（迭代方式，避免深层嵌套导致栈溢出）
     *
     * @param path 文件或目录的路径
     * @return true：删除成功 false：删除失败
     */
    @JvmStatic
    fun delete(path: String): Boolean {
        if (!isExists(path)) return false
        val root = File(path)
        if (root.isFile) return root.delete()
        // 使用栈迭代删除，避免递归栈溢出
        val fileStack: ArrayDeque<File> = ArrayDeque()
        fileStack.addLast(root)
        val dirsToDelete = mutableListOf<File>()
        while (fileStack.isNotEmpty()) {
            val current = fileStack.removeLast()
            val children = current.listFiles()
            if (children.isNullOrEmpty()) {
                if (!current.delete()) return false
            } else {
                dirsToDelete.add(current)
                for (child in children) {
                    if (child.isDirectory) {
                        fileStack.addLast(child)
                    } else {
                        if (!child.delete()) return false
                    }
                }
            }
        }
        // 从子到父依次删除目录
        for (dir in dirsToDelete.reversed()) {
            if (!dir.delete()) return false
        }
        return true
    }

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    @JvmStatic
    fun read(filePath: String): String {
        if (!isExists(filePath) || File(filePath).length() <= 0) return ""
        try {
            val file = File(filePath)
            FileInputStream(file).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    return reader.lineSequence().joinToString("\n")
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "read file failed: $filePath", e)
            return ""
        }
    }

    /**
     * 将字符串写入指定文件（使用 UTF-8 编码）
     *
     * @param filePath 文件路径
     * @param content  内容
     * @param append   是否追加
     */
    @JvmStatic
    fun write(filePath: String, content: String, append: Boolean): Boolean {
        var outputStream: FileOutputStream? = null
        var writer: java.io.OutputStreamWriter? = null
        try {
            outputStream = FileOutputStream(filePath, append)
            writer = java.io.OutputStreamWriter(outputStream, Charsets.UTF_8)
            writer.write(content)
            writer.flush()
            return true
        } catch (e: IOException) {
            Log.w("Files", "write file failed: $filePath", e)
            return false
        } finally {
            closeQuietly(writer)
            closeQuietly(outputStream)
        }
    }

    /**
     * 获取设备图片总数
     */
    @JvmStatic
    fun getImageTotal(context: Context): Int {
        return queryMediaCount(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    }

    /**
     * 获取设备视频总数
     */
    @JvmStatic
    fun getVideoTotal(context: Context): Int {
        return queryMediaCount(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
    }

    /**
     * 获取设备音频总数
     */
    @JvmStatic
    fun getAudioTotal(context: Context): Int {
        return queryMediaCount(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
    }

    /**
     * 通用查询媒体数量，确保 Cursor 被正确关闭
     */
    private fun queryMediaCount(context: Context, uri: android.net.Uri): Int {
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return 0
        return cursor.use { it.count }
    }

    /**
     * 获取设备M3U列表总数
     */
    @JvmStatic
    fun getM3UTotal(context: Context): Int {
        val cursor = context.contentResolver.query(
            M3U_URI, M3U_PROJECTION, M3U_SELECTION, M3U_SELECTION_ARGS, null
        ) ?: return 0
        return cursor.use { it.count }
    }

    /**
     * 获取设备图片列表
     *
     * @param pageSize 每页查询大小
     * @param curPage 当前查询页（从0开始）
     */
    @JvmStatic
    fun getImageList(context: Context, pageSize: Int, curPage: Int): MutableList<File> {
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC LIMIT $pageSize OFFSET ${pageSize * curPage}"
        return queryMediaFiles(
            context,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            sortOrder,
            MediaStore.Images.Media.DATA
        )
    }

    /**
     * 获取设备视频列表
     *
     * @param pageSize 每页查询大小
     * @param curPage 当前查询页（从0开始）
     */
    @JvmStatic
    fun getVideoList(context: Context, pageSize: Int, curPage: Int): MutableList<File> {
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC LIMIT $pageSize OFFSET ${pageSize * curPage}"
        return queryMediaFiles(
            context,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            sortOrder,
            MediaStore.Video.Media.DATA
        )
    }

    /**
     * 获取设备音频列表
     *
     * @param pageSize 每页查询大小
     * @param curPage 当前查询页（从0开始）
     */
    @JvmStatic
    fun getAudioList(context: Context, pageSize: Int, curPage: Int): MutableList<File> {
        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC LIMIT $pageSize OFFSET ${pageSize * curPage}"
        return queryMediaFiles(
            context,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            sortOrder,
            MediaStore.Audio.Media.DATA
        )
    }

    /**
     * 获取设备M3U列表
     *
     * @param pageSize 每页查询大小
     * @param curPage 当前查询页（从0开始）
     */
    @JvmStatic
    fun getM3UList(context: Context, pageSize: Int, curPage: Int): MutableList<File> {
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT $pageSize OFFSET ${pageSize * curPage}"
        return queryMediaFiles(
            context, M3U_URI, M3U_PROJECTION, M3U_SELECTION, M3U_SELECTION_ARGS,
            sortOrder, MediaStore.Files.FileColumns.DATA
        )
    }

    // ==================== 通用私有方法 ====================

    /**
     * 通用查询媒体文件列表（完整参数版，使用 LIMIT/OFFSET 真分页）
     */
    @SuppressLint("Range")
    private fun queryMediaFiles(
        context: Context, uri: android.net.Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?, dataColumn: String
    ): MutableList<File> {
        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            ?: return ArrayList()
        return cursor.use {
            val fileList = ArrayList<File>()
            while (it.moveToNext()) {
                val filePath = it.getString(it.getColumnIndexOrThrow(dataColumn))
                fileList.add(File(filePath))
            }
            fileList
        }
    }

    /**
     * 借助第三方软件打开文件
     * 需先在AndroidManifest.xml 注册 provider
     *
     * <provider
     *     android:name="androidx.core.content.FileProvider"
     *     android:authorities="${applicationId}.fileprovider"
     *     android:exported="false"
     *     android:grantUriPermissions="true">
     *     <meta-data
     *         android:name="android.support.FILE_PROVIDER_PATHS"
     *         android:resource="@xml/file_provider_paths" />
     * </provider>
     */
    @JvmStatic
    fun openToExternal(context: Context, file: File): Boolean {
        if (!file.exists()) return false
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        intent.setDataAndType(uri, getFileMimeType(file))
        // 用于检查是否存在一个Activity能够响应特定的Intent。如果返回值为null，表示没有找到匹配的Activity
        if (intent.resolveActivity(context.packageManager) == null) return false
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "open to external failure: ${file.absolutePath}", e)
            return false
        }
        return true
    }


}