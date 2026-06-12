package top.jessi.jhelper.image

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import java.io.IOException
import java.io.InputStream

/**
 * SVG 解码器
 * 将 InputStream 解析为 SVG 对象
 */
class SvgDecoder : ResourceDecoder<InputStream, SVG> {

    @Throws(IOException::class)
    override fun handles(source: InputStream, options: Options): Boolean {
        // 总是尝试解码，让 Glide 决定是否使用
        return true
    }

    @Throws(IOException::class)
    override fun decode(source: InputStream, width: Int, height: Int, options: Options): Resource<SVG>? {
        return try {
            val svg = SVG.getFromInputStream(source)
            SimpleResource(svg)
        } catch (e: SVGParseException) {
            throw IOException("Cannot load SVG from stream", e)
        }
    }
}
