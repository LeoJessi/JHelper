package top.jessi.jhelper.base

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.lang.ref.WeakReference

/**
 * Created by Jessi on 2024/6/3 14:09
 * Email：17324719944@189.cn
 * Describe：Handler基类
 */
class IHandler(handler: MessageHandler? = null) : Handler(Looper.getMainLooper()) {

    private val mReference = WeakReference(handler)

    override fun handleMessage(msg: Message) {
        mReference.get()?.onHandleMessage(msg)
    }

    /**
     * 发送只携带 what 的空消息
     * @param what 消息类型标识
     */
    fun send(what: Int): Boolean {
        val msg = obtainMessage(what)
        return sendMessage(msg)
    }

    /**
     * 发送携带 what 和 obj 的消息
     * @param what 消息类型标识
     * @param obj  消息携带的对象
     */
    fun send(what: Int, obj: Any?): Boolean {
        val msg = obtainMessage(what, obj)
        return sendMessage(msg)
    }

    /**
     * 发送携带 what、arg1、arg2 的消息
     * @param what 消息类型标识
     * @param arg1 第一个整型参数（通常用于传递简单数值）
     * @param arg2 第二个整型参数
     */
    fun send(what: Int, arg1: Int, arg2: Int): Boolean {
        val msg = obtainMessage(what, arg1, arg2)
        return sendMessage(msg)
    }

    /**
     * 延迟发送只携带 what 的空消息
     * @param what         消息类型标识
     * @param delayMillis  延迟时间（毫秒）
     */
    fun sendDelayed(what: Int, delayMillis: Long): Boolean {
        val msg = obtainMessage(what)
        return sendMessageDelayed(msg, delayMillis)
    }

    /**
     * 延迟发送携带 what 和 obj 的消息
     * @param what         消息类型标识
     * @param obj          消息携带的对象
     * @param delayMillis  延迟时间（毫秒）
     */
    fun sendDelayed(what: Int, obj: Any?, delayMillis: Long): Boolean {
        val msg = obtainMessage(what, obj)
        return sendMessageDelayed(msg, delayMillis)
    }

    /**
     * 延迟发送携带 what、arg1、arg2 的消息
     * @param what         消息类型标识
     * @param arg1         第一个整型参数
     * @param arg2         第二个整型参数
     * @param delayMillis  延迟时间（毫秒）
     */
    fun sendDelayed(what: Int, arg1: Int, arg2: Int, delayMillis: Long): Boolean {
        val msg = obtainMessage(what, arg1, arg2)
        return sendMessageDelayed(msg, delayMillis)
    }

    /**
     * 消息处理接口，Activity/Fragment 实现此接口以接收 Handler 消息
     */
    interface MessageHandler {
        fun onHandleMessage(msg: Message)
    }
}
