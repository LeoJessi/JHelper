package top.jessi.jhelper.file

import android.content.Context
import android.content.Intent
import android.database.Cursor
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
import java.io.FileWriter
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
        var mimeType = ""
        var extension = MimeTypeMap.getFileExtensionFromUrl(file.absolutePath)
        if (TextUtils.isEmpty(extension)) {
            val fileName = file.name
            if (fileName == "" || fileName.endsWith(".")) {
                return "vnd.android.document/hidden"
            }
            val lastDotIndex = fileName.lastIndexOf(".")
            if (lastDotIndex != -1 && lastDotIndex < fileName.length - 1) {
                extension = fileName.substring(lastDotIndex + 1).lowercase(Locale.getDefault())
            }
        }
        if (!TextUtils.isEmpty(extension)) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).toString()
        }
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "application/octet-stream"
        }
        return mimeType
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
        var isSuccess = false
        val file = File(filePath)
        if (!file.exists()) {
            try {
                /*根据文件路径截取出文件所在文件夹，如文件夹不存在则先创建文件夹*/
                val dirPath = filePath.substring(0, filePath.lastIndexOf("/"))
                if (!isExists(dirPath)) createDir(dirPath)
                isSuccess = file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return isSuccess
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
            e.printStackTrace()
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
            e.printStackTrace()
        } finally {
            closeQuietly(sourceChannel)
            closeQuietly(targetChannel)
        }
        return isSuccess
    }

    /**
     * 移动文件
     *
     * @param sourceFilePath 资源文件路径
     * @param targetFilePath 目标文件路径
     * @return 是否移动成功
     */
    @JvmStatic
    fun moveFile(sourceFilePath: String, targetFilePath: String): Boolean {
        try {
            val sourceFile = File(sourceFilePath)
            val targetFile = File(targetFilePath)
            if (!sourceFile.exists()) return false
            if (!isExists(targetFilePath)) if (!create(targetFilePath)) return false
            return sourceFile.renameTo(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 删除文件或目录
     *
     * @param path 文件或目录的路径
     * @return true：删除成功 false：删除失败
     */
    @JvmStatic
    fun delete(path: String): Boolean {
        if (!isExists(path)) return false
        val file = File(path)
        if (!file.isFile) {
            val files = file.listFiles()
            if (files != null) {
                for (f in files) {
                    delete(f.absolutePath)
                }
            }
        }
        return file.delete()
    }

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    @JvmStatic
    fun read(filePath: String): String {
        if (!isExists(filePath)) return ""
        var inputStream: FileInputStream? = null
        var bufferedReader: BufferedReader? = null
        val stringBuilder = StringBuilder()
        try {
            val file = File(filePath)
            inputStream = FileInputStream(file)
            bufferedReader = BufferedReader(InputStreamReader(inputStream), inputStream.available())
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            closeQuietly(bufferedReader)
            closeQuietly(inputStream)
        }
        return stringBuilder.toString()
    }

    /**
     * 将字符串写入指定文件
     *
     * @param filePath 文件路径
     * @param content  内容
     * @param append   是否追加
     */
    @JvmStatic
    fun write(filePath: String, content: String, append: Boolean): Boolean {
        var isSuccess = false
        var fileWriter: FileWriter? = null
        try {
            fileWriter = FileWriter(filePath, append)
            fileWriter.write(content)
            fileWriter.flush()
            isSuccess = true
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            closeQuietly(fileWriter)
        }
        return isSuccess
    }

    /**
     * 获取设备图片总数
     */
    @JvmStatic
    fun getImageTotal(context: Context): Int {
        val cursor: Cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null
        ) ?: return 0
        val total = cursor.count
        cursor.close()
        return total
    }

    /**
     * 获取设备视频总数
     */
    @JvmStatic
    fun getVideoTotal(context: Context): Int {
        val cursor: Cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, null
        ) ?: return 0
        val total = cursor.count
        cursor.close()
        return total
    }

    /**
     * 获取设备音频总数
     */
    @JvmStatic
    fun getAudioTotal(context: Context): Int {
        val cursor: Cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null
        ) ?: return 0
        val total = cursor.count
        cursor.close()
        return total
    }

    /**
     * 获取设备M3U列表总数
     */
    @JvmStatic
    fun getM3UTotal(context: Context): Int {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.DISPLAY_NAME)
        val selection = ("(${MediaStore.Files.FileColumns.MIME_TYPE} IN (?, ?, ?)"
                + " OR LOWER(${MediaStore.Files.FileColumns.DATA}) LIKE ?"
                + " OR LOWER(${MediaStore.Files.FileColumns.DATA}) LIKE ?)")
        val selectionArgs = arrayOf("audio/x-mpegurl", "application/x-mpegurl", "audio/mpegurl", "%.m3u", "%.m3u8")
        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder) ?: return 0
        val total = cursor.count
        cursor.close()
        return total
    }

    /**
     * 获取设备图片列表
     *
     * @param pageSize 每页查询大小
     * @param curPage 当前查询页
     */
    @JvmStatic
    fun getImageList(context: Context, pageSize: Int, curPage: Int): MutableList<File> {
        // 默认按照最后修改时间倒序排序
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        val cursor: Cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, sortOrder
        ) ?: return ArrayList()
        val fileList: MutableList<File> = ArrayList()
        // 查询下标
        var curIndex = -1
        // 偏移量 -- 获取起始下标
        val offset = pageSize * curPage
        while (cursor.moveToNext()) {
            curIndex++
            // 查询下标未到获取起始下标 -- 跳过此条数据
            if (curIndex < offset) continue
            // 查询下标已到需获取列表的最后一个下标 -- 跳出此次查询
            if (curIndex >= offset + pageSize) break
            /* 获取文件路径 */
            val filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
            val file = File(filePath)
            fileList.add(file)
        }
        cursor.close()
        return fileList
    }

    /**
     * 获取设备视频列表
     *
     * @param pageSize 每页查询大小
     * @param curPage 当前查询页
     */
    @JvmStatic
    fun getVideoList(context: Context, pageSize: Int, curPage: Int): MutableList<File> {
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        val cursor: Cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, sortOrder
        ) ?: return ArrayList()
        val fileList: MutableList<File> = ArrayList()
        var curIndex = -1
        val offset = pageSize * curPage
        while (cursor.moveToNext()) {
            curIndex++
            if (curIndex < offset) continue
            if (curIndex >= offset + pageSize) break
            val filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
            val file = File(filePath)
            fileList.add(file)
        }
        cursor.close()
        return fileList
    }

    /**
     * 获取设备音频列表
     *
     * @param pageSize 每页查询大小
     * @param curPage 当前查询页
     */
    @JvmStatic
    fun getAudioList(context: Context, pageSize: Int, curPage: Int): MutableList<File> {
        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
        val cursor: Cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, sortOrder
        ) ?: return ArrayList()
        val fileList: MutableList<File> = ArrayList()
        var curIndex = -1
        val offset = pageSize * curPage
        while (cursor.moveToNext()) {
            curIndex++
            if (curIndex < offset) continue
            if (curIndex >= offset + pageSize) break
            val filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
            val file = File(filePath)
            fileList.add(file)
        }
        cursor.close()
        return fileList
    }

    /**
     * 获取设备M3U列表
     *
     * @param pageSize 每页查询大小
     * @param curPage 当前查询页
     */
    @JvmStatic
    fun getM3UList(context: Context, pageSize: Int, curPage: Int): MutableList<File> {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.DISPLAY_NAME)
        val selection = ("(${MediaStore.Files.FileColumns.MIME_TYPE} IN (?, ?, ?)"
                + " OR LOWER(${MediaStore.Files.FileColumns.DATA}) LIKE ?"
                + " OR LOWER(${MediaStore.Files.FileColumns.DATA}) LIKE ?)")
        val selectionArgs = arrayOf("audio/x-mpegurl", "application/x-mpegurl", "audio/mpegurl", "%.m3u", "%.m3u8")
        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
        val cursor =
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder) ?: return ArrayList()
        val fileList: MutableList<File> = ArrayList()
        var curIndex = -1
        val offset = pageSize * curPage
        while (cursor.moveToNext()) {
            curIndex++
            if (curIndex < offset) continue
            if (curIndex >= offset + pageSize) break
            val filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
            val file = File(filePath)
            fileList.add(file)
        }
        cursor.close()
        return fileList
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
            Log.w("Files", "open to external failure")
            return false
        }
        return true
    }


}