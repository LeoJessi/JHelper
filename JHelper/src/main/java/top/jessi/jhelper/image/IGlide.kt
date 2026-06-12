package top.jessi.jhelper.image

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import top.jessi.jhelper.thread.ThreadPool

/**
 * Glide图片加载工具类
 *
 * 支持的图片源类型：
 * - String: 网络URL（如 "https://example.com/image.jpg"）
 * - Uri: 本地Uri（如 Uri.parse("content://...")）
 * - File: 本地文件（如 File("/path/to/image.jpg")）
 * - Int: 资源ID（如 R.drawable.image）
 * - Bitmap: Bitmap对象
 *
 * 支持自动识别所有格式（PNG/JPG/GIF/WebP/SVG等）
 * SVG 支持由 MyGlide 模块提供
 *
 * DiskCacheStrategy 缓存策略：
 * - ALL: 缓存原始数据和转换后的数据 -- SVG图片不能使用这个？
 * - NONE: 不缓存
 * - DATA: 只缓存原始数据
 * - RESOURCE: 只缓存转换后的数据
 * - AUTOMATIC: 根据数据源自动选择策略（默认）
 *
 * 使用示例（Java）：
 * IGlide.load(imageView, url);
 * IGlide.load(imageView, url, R.drawable.placeholder, R.drawable.error);
 * IGlide.loadCircle(imageView, url, R.drawable.placeholder, R.drawable.error);
 * IGlide.loadRound(imageView, url, 20, R.drawable.placeholder, R.drawable.error);
 * IGlide.loadGif(imageView, gifUrl);
 * IGlide.loadWithCallback(context, url, callback);
 *
 * 使用示例（Kotlin）：
 * IGlide.load(imageView, url)
 * IGlide.load(imageView, url, R.drawable.placeholder, R.drawable.error)
 * IGlide.loadCircle(imageView, url, R.drawable.placeholder, R.drawable.error)
 * IGlide.loadRound(imageView, url, 20, R.drawable.placeholder, R.drawable.error)
 * IGlide.loadGif(imageView, gifUrl)
 * IGlide.loadWithCallback(context, url, callback = callback)
 */
class IGlide {

    companion object {

        // ==================== 基础加载方法 ====================

        /**
         * 加载图片（通用方法，自动识别格式）
         *
         * 支持自动识别 PNG/JPG/GIF/WebP/SVG 等格式
         *
         * @param imageView 目标ImageView
         * @param source 图片源（支持String/Uri/File/Int/Bitmap等）
         * @param placeholder 占位图资源ID，0表示不设置
         * @param error 错误图资源ID，0表示不设置
         * @param diskCacheStrategy 磁盘缓存策略，默认为AUTOMATIC
         */
        @JvmOverloads
        @JvmStatic
        fun load(
            imageView: ImageView, source: Any?, placeholder: Int = 0, error: Int = 0,
            diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC
        ) {
            Glide.with(imageView.context)
                .load(source)
                .applyOptions(placeholder, error)
                .diskCacheStrategy(diskCacheStrategy)
                .into(imageView)
        }

        // ==================== 圆形图片 ====================

        /**
         * 加载圆形图片（常用于头像）
         *
         * @param imageView 目标ImageView
         * @param source 图片源（支持多种类型）
         * @param placeholder 占位图资源ID，0表示不设置
         * @param error 错误图资源ID，0表示不设置
         * @param diskCacheStrategy 磁盘缓存策略，默认为AUTOMATIC
         */
        @JvmOverloads
        @JvmStatic
        fun loadCircle(
            imageView: ImageView, source: Any?, placeholder: Int = 0, error: Int = 0,
            diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC
        ) {
            Glide.with(imageView.context)
                .load(source)
                .applyOptions(placeholder, error)
                .circleCrop()
                .diskCacheStrategy(diskCacheStrategy)
                .into(imageView)
        }

        // ==================== 圆角图片 ====================

        /**
         * 加载圆角图片（统一圆角半径）
         *
         * @param imageView 目标ImageView
         * @param source 图片源（支持多种类型）
         * @param radius 圆角半径（单位：px）
         * @param placeholder 占位图资源ID，0表示不设置
         * @param error 错误图资源ID，0表示不设置
         * @param diskCacheStrategy 磁盘缓存策略，默认为AUTOMATIC
         */
        @JvmOverloads
        @JvmStatic
        fun loadRound(
            imageView: ImageView, source: Any?, radius: Int, placeholder: Int = 0, error: Int = 0,
            diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC
        ) {
            Glide.with(imageView.context)
                .load(source)
                .applyOptions(placeholder, error)
                .transform(CenterCrop(), RoundedCorners(radius))
                .diskCacheStrategy(diskCacheStrategy)
                .into(imageView)
        }

        // ==================== 图片变换 ====================

        /**
         * 居中裁剪加载
         *
         * @param imageView 目标ImageView
         * @param source 图片源（支持多种类型）
         * @param placeholder 占位图资源ID，0表示不设置
         * @param error 错误图资源ID，0表示不设置
         * @param diskCacheStrategy 磁盘缓存策略，默认为AUTOMATIC
         */
        @JvmOverloads
        @JvmStatic
        fun loadCenterCrop(
            imageView: ImageView, source: Any?, placeholder: Int = 0, error: Int = 0,
            diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC
        ) {
            Glide.with(imageView.context)
                .load(source)
                .applyOptions(placeholder, error)
                .centerCrop()
                .diskCacheStrategy(diskCacheStrategy)
                .into(imageView)
        }

        /**
         * 适应居中加载（完整显示图片）
         *
         * @param imageView 目标ImageView
         * @param source 图片源（支持多种类型）
         * @param placeholder 占位图资源ID，0表示不设置
         * @param error 错误图资源ID，0表示不设置
         * @param diskCacheStrategy 磁盘缓存策略，默认为AUTOMATIC
         */
        @JvmOverloads
        @JvmStatic
        fun loadFitCenter(
            imageView: ImageView, source: Any?, placeholder: Int = 0, error: Int = 0,
            diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC
        ) {
            Glide.with(imageView.context)
                .load(source)
                .applyOptions(placeholder, error)
                .fitCenter()
                .diskCacheStrategy(diskCacheStrategy)
                .into(imageView)
        }

        // ==================== GIF 加载 ====================

        /**
         * 加载 GIF 图片
         *
         * 强制以 GIF 格式解码，确保动画播放
         *
         * @param imageView 目标ImageView
         * @param source 图片源（支持String/Uri/File/Int等）
         * @param placeholder 占位图资源ID，0表示不设置
         * @param error 错误图资源ID，0表示不设置
         * @param diskCacheStrategy 磁盘缓存策略，默认为AUTOMATIC
         */
        @JvmOverloads
        @JvmStatic
        fun loadGif(
            imageView: ImageView, source: Any?, placeholder: Int = 0, error: Int = 0,
            diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC
        ) {
            Glide.with(imageView.context)
                .asGif()
                .load(source)
                .applyOptions(placeholder, error)
                .diskCacheStrategy(diskCacheStrategy)
                .into(imageView)
        }

        /**
         * 加载圆形 GIF 图片
         *
         * @param imageView 目标ImageView
         * @param source 图片源（支持多种类型）
         * @param placeholder 占位图资源ID，0表示不设置
         * @param error 错误图资源ID，0表示不设置
         * @param diskCacheStrategy 磁盘缓存策略，默认为AUTOMATIC
         */
        @JvmOverloads
        @JvmStatic
        fun loadGifCircle(
            imageView: ImageView, source: Any?, placeholder: Int = 0, error: Int = 0,
            diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC
        ) {
            Glide.with(imageView.context)
                .asGif()
                .load(source)
                .applyOptions(placeholder, error)
                .circleCrop()
                .diskCacheStrategy(diskCacheStrategy)
                .into(imageView)
        }

        /**
         * 加载圆角 GIF 图片
         *
         * @param imageView 目标ImageView
         * @param source 图片源（支持多种类型）
         * @param radius 圆角半径（单位：px）
         * @param placeholder 占位图资源ID，0表示不设置
         * @param error 错误图资源ID，0表示不设置
         * @param diskCacheStrategy 磁盘缓存策略，默认为AUTOMATIC
         */
        @JvmOverloads
        @JvmStatic
        fun loadGifRound(
            imageView: ImageView, source: Any?, radius: Int, placeholder: Int = 0, error: Int = 0,
            diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC
        ) {
            Glide.with(imageView.context)
                .asGif()
                .load(source)
                .applyOptions(placeholder, error)
                .transform(CenterCrop(), RoundedCorners(radius))
                .diskCacheStrategy(diskCacheStrategy)
                .into(imageView)
        }

        // ==================== 尺寸控制 ====================

        /**
         * 指定宽高加载
         *
         * @param imageView 目标ImageView
         * @param source 图片源（支持多种类型）
         * @param width 宽度（px）
         * @param height 高度（px）
         * @param placeholder 占位图资源ID，0表示不设置
         * @param error 错误图资源ID，0表示不设置
         * @param diskCacheStrategy 磁盘缓存策略，默认为AUTOMATIC
         */
        @JvmOverloads
        @JvmStatic
        fun loadWithSize(
            imageView: ImageView, source: Any?, width: Int, height: Int, placeholder: Int = 0, error: Int = 0,
            diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC
        ) {
            Glide.with(imageView.context)
                .load(source)
                .applyOptions(placeholder, error)
                .override(width, height)
                .diskCacheStrategy(diskCacheStrategy)
                .into(imageView)
        }

        /**
         * 加载缩略图（按比例缩小）
         *
         * @param imageView 目标ImageView
         * @param source 图片源（支持多种类型）
         * @param sizeMultiplier 缩放比例（0.0f-1.0f，如0.2f表示原图的20%）
         * @param placeholder 占位图资源ID，0表示不设置
         * @param error 错误图资源ID，0表示不设置
         * @param diskCacheStrategy 磁盘缓存策略，默认为AUTOMATIC
         */
        @JvmOverloads
        @JvmStatic
        fun loadThumbnail(
            imageView: ImageView, source: Any?, sizeMultiplier: Float, placeholder: Int = 0, error: Int = 0,
            diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC
        ) {
            Glide.with(imageView.context)
                .load(source)
                .applyOptions(placeholder, error)
                .thumbnail(sizeMultiplier)
                .diskCacheStrategy(diskCacheStrategy)
                .into(imageView)
        }

        // ==================== 缓存控制 ====================

        /**
         * 跳过缓存加载（每次从网络加载）
         *
         * @param imageView 目标ImageView
         * @param source 图片源（支持多种类型）
         */
        @JvmStatic
        fun loadSkipCache(imageView: ImageView, source: Any?) {
            Glide.with(imageView.context)
                .load(source)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(imageView)
        }

        /**
         * 仅从缓存加载（不请求网络）
         *
         * @param imageView 目标ImageView
         * @param source 图片源（支持多种类型）
         */
        @JvmStatic
        fun loadOnlyCache(imageView: ImageView, source: Any?) {
            Glide.with(imageView.context)
                .load(source)
                .onlyRetrieveFromCache(true)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)
        }

        /**
         * 禁用内存缓存加载
         *
         * @param imageView 目标ImageView
         * @param source 图片源（支持多种类型）
         */
        @JvmStatic
        fun loadNoMemoryCache(imageView: ImageView, source: Any?) {
            Glide.with(imageView.context)
                .load(source)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)
        }

        // ==================== 生命周期与回调 ====================

        /**
         * 取消图片加载请求
         *
         * @param imageView 目标ImageView
         */
        @JvmStatic
        fun cancel(imageView: ImageView) {
            Glide.with(imageView.context).clear(imageView)
        }

        /**
         * 预加载图片到缓存（不显示）
         *
         * @param context Context
         * @param source 图片源（支持多种类型）
         * @param width 宽度，0表示预加载原始尺寸
         * @param height 高度，0表示预加载原始尺寸
         * @param diskCacheStrategy 磁盘缓存策略，默认为AUTOMATIC
         */
        @JvmOverloads
        @JvmStatic
        fun preload(
            context: Context, source: Any?, width: Int = 0, height: Int = 0,
            diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC
        ) {
            val glideRequest = Glide.with(context)
                .load(source)
                .diskCacheStrategy(diskCacheStrategy)
            if (width > 0 && height > 0) {
                glideRequest.preload(width, height)
            } else {
                glideRequest.preload()
            }
        }

        // ==================== 缓存管理 ====================

        /**
         * 清理内存缓存（需在主线程调用）
         *
         * @param context Context
         */
        @JvmStatic
        fun clearMemory(context: Context) {
            ThreadPool.execute(Dispatchers.Main) { Glide.get(context).clearMemory() }
        }

        /**
         * 清理磁盘缓存（需在子线程调用）
         *
         * @param context Context
         */
        @JvmStatic
        fun clearDisk(context: Context) {
            ThreadPool.execute { Glide.get(context).clearDiskCache() }
        }

        /**
         * 清理所有缓存
         *
         * @param context Context
         */
        @JvmStatic
        fun clearAllCache(context: Context) {
            clearMemory(context)
            clearDisk(context)
        }

        // ==================== 高级功能 ====================

        /**
         * 带回调的图片加载（用于二次处理）
         *
         * 使用 CustomTarget 获取 Drawable，适合需要对图片进行二次处理的场景
         * 注意：回调在主线程执行
         *
         * @param context Context
         * @param source 图片源（支持多种类型）
         * @param placeholder 占位图资源ID，0表示不设置
         * @param error 错误图资源ID，0表示不设置
         * @param diskCacheStrategy 磁盘缓存策略，默认为AUTOMATIC
         * @param callback 回调接口
         */
        @JvmOverloads
        @JvmStatic
        fun loadWithCallback(
            context: Context, source: Any?, placeholder: Int = 0, error: Int = 0,
            diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC,
            callback: GlideCallback?
        ) {
            Glide.with(context)
                .load(source)
                .applyOptions(placeholder, error)
                .diskCacheStrategy(diskCacheStrategy)
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        callback?.onSuccess(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // 资源被清除时的回调，通常不需要处理
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        callback?.onFailure(null)
                    }
                })
        }

        /**
         * 应用占位图和错误图配置
         *
         * @param placeholder 占位图资源ID，0表示不设置
         * @param error 错误图资源ID，0表示不设置
         * @return 配置后的RequestBuilder
         */
        private fun <T> RequestBuilder<T>.applyOptions(placeholder: Int, error: Int): RequestBuilder<T> {
            return this
                .apply { if (placeholder != 0) placeholder(placeholder) }
                .apply { if (error != 0) error(error) }
        }
    }

    /**
     * Glide加载回调接口
     */
    interface GlideCallback {
        /** 加载成功 */
        fun onSuccess(drawable: Drawable)

        /** 加载失败 */
        fun onFailure(exception: GlideException?)
    }
}
