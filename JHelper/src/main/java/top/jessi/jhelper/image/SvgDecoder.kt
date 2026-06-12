package top.jessi.jhelper.image

import android.util.Log
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * SVG 解码器
 * 将 InputStream 解析为 SVG 对象
 *
 * 优化点：
 * 1. 通过检查文件头判断是否是 SVG，快速排除非 SVG 文件
 * 2. 只读取前 512 字节进行判断，减少开销
 * 3. 支持大尺寸 SVG 文件
 */
class SvgDecoder : ResourceDecoder<InputStream, SVG> {

    companion object {
        private const val TAG = "SvgDecoder"
    }

    @Throws(IOException::class)
    override fun handles(source: InputStream, options: Options): Boolean {
        // 快速检查是否是 SVG 文件
        return try {
            // 只读取前 512 字节，减少开销
            source.mark(512)
            val buffer = ByteArray(512)
            val bytesRead = source.read(buffer, 0, 512)
            source.reset()

            if (bytesRead <= 0) return false

            // 转换为字符串并检查文件头
            // SVG 文件通常以 XML 声明或 <svg 开头
            val content = String(buffer, 0, bytesRead, Charsets.UTF_8).trimStart()
            content.startsWith("<?xml") || content.startsWith("<svg")
        } catch (e: Exception) {
            false
        }
    }

    @Throws(IOException::class)
    override fun decode(source: InputStream, width: Int, height: Int, options: Options): Resource<SVG>? {
        return try {
            // 读取所有数据到内存，避免流阻塞
            val bytes = readAllBytes(source)

            // 解析 SVG
            val svg = SVG.getFromInputStream(bytes.inputStream())

            // 设置 SVG 尺寸（如果指定了宽高）
            if (width > 0 && height > 0) {
                svg.documentWidth = width.toFloat()
                svg.documentHeight = height.toFloat()
            }

            SimpleResource(svg)
        } catch (e: SVGParseException) {
            Log.w(TAG, "Cannot load SVG from stream", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error loading SVG", e)
            null
        }
    }

    /**
     * 读取所有字节到内存
     */
    private fun readAllBytes(inputStream: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()
        val data = ByteArray(4096)
        var bytesRead: Int
        while (inputStream.read(data).also { bytesRead = it } != -1) {
            buffer.write(data, 0, bytesRead)
        }
        return buffer.toByteArray()
    }
}
