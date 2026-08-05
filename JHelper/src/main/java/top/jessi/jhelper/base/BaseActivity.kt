package top.jessi.jhelper.base

import android.os.Bundle
import android.os.Message
import androidx.fragment.app.FragmentActivity

/**
 * Created by Jessi on 2026/8/5 22:59
 * Email：17324719944@189.cn
 * Describe：Activity基类
 */
open class BaseActivity : FragmentActivity(), BaseHandler.MessageHandler {

    protected val mBaseHandler = BaseHandler(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onHandleMessage(msg: Message) {

    }

    override fun onDestroy() {
        super.onDestroy()
        mBaseHandler.removeCallbacksAndMessages(null)
    }
}
