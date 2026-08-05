package top.jessi.jhelper.base

import android.os.Message
import androidx.fragment.app.Fragment

/**
 * Created by Jessi on 2026/8/5 22:59
 * Email：17324719944@189.cn
 * Describe：Fragment基类
 */
open class BaseFragment : Fragment(), BaseHandler.MessageHandler {

    protected val mBaseHandler = BaseHandler(this)

    override fun onHandleMessage(msg: Message) {

    }

    override fun onDestroy() {
        super.onDestroy()
        mBaseHandler.removeCallbacksAndMessages(null)
    }
}
