package top.jessi.jhelper.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import top.jessi.jhelper.file.Files
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by Jessi on 2024/11/26 13:51
 * Email：17324719944@189.cn
 * Describe：图片工具类
 */
object Pictures {

    /**
     * 将 Bitmap 转换成字节数组
     *
     * @param bitmap Bitmap 对象
     * @return 字节数组
     */
    @JvmStatic
    fun bitmapToBytes(bitmap: Bitmap): ByteArray? {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    /**
     * 将字节数组转换成 Bitmap
     *
     * @param bytes 字节数组
     * @return Bitmap 对象
     */
    @JvmStatic
    fun bytesToBitmap(bytes: ByteArray): Bitmap? {
        return if (bytes.isNotEmpty()) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else {
            null
        }
    }

    /**
     * 将 Drawable 对象转换成 Bitmap 对象
     *
     * @param drawable Drawable 对象
     * @return Bitmap 对象
     */
    @JvmStatic
    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        // 创建一个与Drawable相同大小的Bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        /*将Drawable绘制到Bitmap上*/
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * 将 Bitmap 对象转换成 Drawable 对象
     *
     * @param context 上下文
     * @param bitmap  Bitmap对象
     * @return Drawable对象
     */
    @JvmStatic
    fun bitmapToDrawable(context: Context, bitmap: Bitmap?): Drawable {
        return BitmapDrawable(context.resources, bitmap)
    }

    /**
     * 将 Bitmap 按指定角度旋转
     *
     * @param bitmap  Bitmap 对象
     * @param degrees 旋转的角度
     * @return 旋转后的 Bitmap 对象
     */
    @JvmStatic
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 缩放 Bitmap
     *
     * @param bitmap    Bitmap 对象
     * @param newWidth  新的宽度
     * @param newHeight 新的高度
     * @return 缩放后的 Bitmap 对象
     */
    @JvmStatic
    fun scaleBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    /**
     * 水平翻转图片
     *
     * @param bitmap 原始图片
     * @return 翻转后的图片
     */
    @JvmStatic
    fun flipHorizontal(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null
        val matrix = Matrix()
        matrix.setScale(-1f, 1f)
        val flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (!bitmap.isRecycled) bitmap.recycle()
        return flippedBitmap
    }

    /**
     * 裁剪图片
     *
     * @param bitmap 原始图片
     * @param x      起点横坐标
     * @param y      起点纵坐标
     * @param width  裁剪宽度
     * @param height 裁剪高度
     * @return 裁剪后的图片
     */
    @JvmStatic
    fun cropBitmap(bitmap: Bitmap?, x: Int, y: Int, width: Int, height: Int): Bitmap? {
        if (bitmap == null) return null
        val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)
        if (!bitmap.isRecycled) bitmap.recycle()
        return croppedBitmap
    }

    /**
     * 裁剪中间图片
     *
     * @param bitmap 原始图片
     * @param width  裁剪宽度
     * @param height 裁剪高度
     * @return 裁剪后的图片
     */
    @JvmStatic
    fun centerCropBitmap(bitmap: Bitmap?, width: Int, height: Int): Bitmap? {
        if (bitmap == null) return null
        val x = (bitmap.width - width) / 2
        val y = (bitmap.height - height) / 2
        val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)
        if (!bitmap.isRecycled) bitmap.recycle()
        return croppedBitmap
    }

    /**
     * 保存 Bitmap 到指定路径
     *
     * @param bitmap   要保存的 Bitmap 对象
     * @param filePath 文件路径，例如 "/sdcard/Pictures/test.jpg"
     * @param format   图片格式，例如 Bitmap.CompressFormat.JPEG、Bitmap.CompressFormat.PNG
     * @param quality  图片质量，取值范围为 0~100，100 表示不压缩，0 表示压缩至最小尺寸
     * @return 是否保存成功
     */
    @JvmStatic
    fun saveImage(bitmap: Bitmap?, filePath: String?, quality: Int, format: ImageFormat?): Boolean {
        if (bitmap == null || filePath.isNullOrBlank()) return false
        var outputStream: OutputStream? = null
        var isSuccess = false
        try {
            val file = File(filePath)
            val parent = file.parentFile ?: return false
            if (!parent.exists()) {
                val mkdirs = parent.mkdirs()
                if (!mkdirs) return false
            }
            outputStream = FileOutputStream(file)
            val compressFormat = when (format) {
                ImageFormat.WEBP -> CompressFormat.WEBP
                ImageFormat.JPEG -> CompressFormat.JPEG
                else -> CompressFormat.PNG
            }
            isSuccess = bitmap.compress(compressFormat, quality, outputStream)
            outputStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            Files.closeQuietly(outputStream)
        }
        return isSuccess
    }

    /**
     * 获取图片尺寸
     *
     * @param filePath 文件路径
     * @return int[]，int[0]为图片宽度，int[1]为图片高度
     */
    @JvmStatic
    fun getBitmapSize(filePath: String?): IntArray {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)
        return intArrayOf(options.outWidth, options.outHeight)
    }

    /**
     * 获取网络资源图片转为bitmap
     *
     * @param imageUrl 图片路径
     * @return bitmap对象
     */
    @JvmStatic
    fun getBitmapFromUrl(imageUrl: String?): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            bitmap
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 将文件图片转换成bitmap图
     *
     * @param filePath 文件路径
     * @return bitmap图
     */
    @JvmStatic
    fun picFileToBitmap(filePath: String?): Bitmap? {
        // 使用 BitmapFactory.decodeFile() 方法将图片转换成 Bitmap 对象
        return BitmapFactory.decodeFile(filePath)
    }

    enum class ImageFormat {
        PNG, WEBP, JPEG
    }

}