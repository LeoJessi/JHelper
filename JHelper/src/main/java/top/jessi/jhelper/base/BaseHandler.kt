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
class BaseHandler(handler: MessageHandler) : Handler(Looper.getMainLooper()) {

    private val mReference = WeakReference(handler)

    override fun handleMessage(msg: Message) {
        mReference.get()?.onHandleMessage(msg)
    }

    /**
     * 消息处理接口，Activity/Fragment 实现此接口以接收 Handler 消息
     */
    interface MessageHandler {
        fun onHandleMessage(msg: Message)
    }
}
