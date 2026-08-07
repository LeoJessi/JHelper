package top.jessi.jhelper.base

import android.content.Context
import android.os.Bundle
import android.os.Message
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by Jessi on 2026/8/5 22:59
 * Email：17324719944@189.cn
 * Describe：Activity基类
 */
open class IActivity : AppCompatActivity(), IHandler.MessageHandler {

    protected val mIHandler = IHandler(this)
    protected val me: Context = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onHandleMessage(msg: Message) {

    }

    override fun onDestroy() {
        super.onDestroy()
        mIHandler.removeCallbacksAndMessages(null)
    }
}
