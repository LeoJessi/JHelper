package top.jessi.jhelper.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.createBitmap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder
import com.caverock.androidsvg.SVG

/**
 * SVG 转换器
 * 将 SVG 对象转换为 BitmapDrawable
 *
 * 优化点：
 * 1. 将 SVG 渲染为 Bitmap，避免 PictureDrawable 每次绘制时重新渲染
 * 2. 限制最大渲染尺寸（256px），减少内存占用
 * 3. 使用 Bitmap 缓存，提升滑动流畅度
 * 4. 对大尺寸 SVG（如 10800x5040）自动缩放
 */
class SvgDrawableTranscoder : ResourceTranscoder<SVG, BitmapDrawable> {

    companion object {
        // 最大渲染尺寸限制，避免渲染过大的 SVG 导致卡顿
        private const val MAX_RENDER_SIZE = 256
    }

    override fun transcode(toTranscode: Resource<SVG>, options: Options): Resource<BitmapDrawable> {
        val svg = toTranscode.get()

        // 渲染 SVG 为 Bitmap
        val bitmap = renderSvgToBitmap(svg)

        val drawable = BitmapDrawable(null, bitmap)
        return SimpleResource(drawable)
    }

    /**
     * 渲染 SVG 为 Bitmap
     * 使用 Bitmap 缓存，避免每次绘制时重新渲染
     */
    private fun renderSvgToBitmap(svg: SVG): Bitmap {
        // 获取 SVG 的文档尺寸
        val docWidth = svg.documentWidth
        val docHeight = svg.documentHeight

        // 计算实际渲染尺寸
        var width = if (docWidth > 0) docWidth.toInt() else MAX_RENDER_SIZE
        var height = if (docHeight > 0) docHeight.toInt() else MAX_RENDER_SIZE

        // 限制最大尺寸，避免渲染过大的 SVG
        if (width > MAX_RENDER_SIZE || height > MAX_RENDER_SIZE) {
            val scale = MAX_RENDER_SIZE.toFloat() / maxOf(width, height)
            width = (width * scale).toInt()
            height = (height * scale).toInt()
        }

        // 创建 Bitmap 并渲染
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        // 设置缩放比例，让 SVG 按比例渲染到目标尺寸
        if (docWidth > 0 && docHeight > 0) {
            val scaleX = width.toFloat() / docWidth
            val scaleY = height.toFloat() / docHeight
            val scale = minOf(scaleX, scaleY)
            canvas.scale(scale, scale)
        }

        svg.renderToCanvas(canvas)

        return bitmap
    }
}
