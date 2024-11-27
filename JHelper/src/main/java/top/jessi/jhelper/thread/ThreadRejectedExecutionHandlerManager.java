package top.jessi.jhelper.thread;

import android.util.Log;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by Jessi on 2022/8/15 17:03
 * Email：17324719944@189.cn
 * Describe：线程池饱和策略
 */
public class ThreadRejectedExecutionHandlerManager implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    }

    /*
     * 饱和策略1：调用者线程执行策略
     * 在该策略下，在调用者中执行被拒绝任务的run方法。除非线程池showdown，否则直接丢弃线程
     * */
    public static class CallerRunsPolicy extends ThreadRejectedExecutionHandlerManager {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            Log.w("Thread queue is full：",
                    "currently running threads：" + executor.getPoolSize()
                            + "-----active threads：" + executor.getActiveCount()
                            + "-----tasks waiting to run：" + executor.getQueue().size());
            /*判断线程池是否在正常运行，如果线程池在正常运行则由调用者线程执行被拒绝的任务。如果线程池停止运行，则直接丢弃该任务*/
            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }

    /*
     * 饱和策略2：终止策略
     * 在该策略下，丢弃被拒绝的任务，并抛出拒绝执行异常
     * */
    public static class AbortPolicy extends ThreadRejectedExecutionHandlerManager {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            Log.w("Thread queue is full：",
                    "currently running threads：" + executor.getPoolSize()
                            + "-----active threads：" + executor.getActiveCount()
                            + "-----tasks waiting to run：" + executor.getQueue().size());
            throw new RejectedExecutionException("请求任务：" + r.toString() + "，线程池负载过高执行饱和终止策略！");
        }
    }

    /*
     * 饱和策略3：丢弃策略
     * 在该策略下，什么都不做直接丢弃被拒绝的任务
     * */
    public static class DiscardPolicy extends ThreadRejectedExecutionHandlerManager {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            Log.w("Thread queue is full：",
                    "currently running threads：" + executor.getPoolSize()
                            + "-----active threads：" + executor.getActiveCount()
                            + "-----tasks waiting to run：" + executor.getQueue().size());
        }
    }

    /*
     * 饱和策略4：弃老策略
     * 在该策略下，丢弃最早放入阻塞队列中的线程，并尝试将拒绝任务加入阻塞队列
     * */
    public static class DiscardOldestPolicy extends ThreadRejectedExecutionHandlerManager {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            Log.w("Thread queue is full：",
                    "currently running threads：" + executor.getPoolSize()
                            + "-----active threads：" + executor.getActiveCount()
                            + "-----tasks waiting to run：" + executor.getQueue().size());
            /*判断线程池是否正常运行，如果线程池正常运行则弹出（或丢弃）最早放入阻塞队列中的任务，并尝试将拒绝任务加入阻塞队列。如果线程池停止运行，则直接丢弃该任务*/
            if (!executor.isShutdown()) {
                executor.getQueue().poll();
                executor.execute(r);
            }
        }
    }
}