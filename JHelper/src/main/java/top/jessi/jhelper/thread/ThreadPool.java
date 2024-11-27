package top.jessi.jhelper.thread;

import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Jessi on 2022/8/15 17:12
 * Email：17324719944@189.cn
 * Describe：单例线程池
 */
public class ThreadPool {
    // 系统可用计算资源(CPU核心数)
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    // 核心线程数
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    // 最大线程数
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    // 空闲线程存活时间
    private static final int KEEP_ALIVE_SECONDS = 30;
    // 工作队列
    private static final BlockingDeque<Runnable> POOL_WORK_QUEUE = new LinkedBlockingDeque<>(128);
    // 工厂模式
    private static final ThreadCustomFactoryManager MY_THREAD_FACTORY = new ThreadCustomFactoryManager("CUSTOM-POOL");
    // 饱和策略
    private static final ThreadRejectedExecutionHandlerManager THREAD_REJECTED_EXECUTION_HANDLER = new ThreadRejectedExecutionHandlerManager.DiscardOldestPolicy();
    // 线程池对象
    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR;
    // 声明定义线程池工具类对象静态变量，在所有线程中同步
    private static volatile ThreadPool sThreadPool = null;

    /*初始化线程池静态代码块*/
    static {
        THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
                //核心线程数
                CORE_POOL_SIZE,
                //最大线程数
                MAXIMUM_POOL_SIZE,
                //空闲线程执行时间
                KEEP_ALIVE_SECONDS,
                //空闲线程执行时间单位
                TimeUnit.SECONDS,
                //工作队列（或阻塞队列）
                POOL_WORK_QUEUE,
                //工厂模式
                MY_THREAD_FACTORY,
                //饱和策略
                THREAD_REJECTED_EXECUTION_HANDLER
        );
    }

    /**
     * 线程池工具类空参构造方法
     * 私有化构造方法,阻止外部直接实例化对象
     */
    private ThreadPool() {
        /*构造方法里面禁止加入任何业务逻辑，如果有初始化逻辑，放在init方法中*/
    }

    /**
     * 获取线程池工具类实例  在所有线程里同步
     * 整个程序都用这个实例  一处更新处处更新  不要反复创建实例消耗系统资源
     *
     * @return 实例对象
     */
    public static ThreadPool getInstance() {
        if (sThreadPool == null) {
            synchronized (ThreadPool.class) {
                if (sThreadPool == null) {
                    sThreadPool = new ThreadPool();
                }
            }
        }
        return sThreadPool;
    }

    /**
     * 执行线程任务，不返回结果
     *
     * @param runnable Runnable任务
     */
    public void executor(Runnable runnable) {
        THREAD_POOL_EXECUTOR.execute(runnable);
    }

    /**
     * 向线程池提交一个任务，返回线程结果
     * Callable接口支持返回执行结果，此时需要调用FutureTask.get()方法实现
     * 此方法会阻塞主线程直到获取‘Future(将来)’结果；当不调用此方法时，主线程不会阻塞！
     *
     * @param callable Callable任务
     * @param <T>      泛型
     * @return 执行任务
     */
    public <T> Future<T> submit(Callable<T> callable) {
        return THREAD_POOL_EXECUTOR.submit(callable);
    }

    /**
     * 获取当前线程池线程数量
     *
     * @return 当前线程池线程数量
     */
    public int getSize() {
        return THREAD_POOL_EXECUTOR.getPoolSize();
    }

    /**
     * 获取当前活动的线程数量
     *
     * @return 当前活动的线程数量
     */
    public int getActiveCount() {
        return THREAD_POOL_EXECUTOR.getActiveCount();
    }

    /**
     * 获取线程池状态
     *
     * @return 线程池状态
     */
    public boolean isShutDown() {
        return THREAD_POOL_EXECUTOR.isShutdown();
    }

    /**
     * 试图停止所有正在执行的活动任务
     *
     * @return 返回等待执行的任务列表
     */
    public List<Runnable> shutDownNow() {
        return THREAD_POOL_EXECUTOR.shutdownNow();
    }

    /**
     * 从线程队列中移除对象
     *
     * @param runnable 任务
     */
    public void cancel(Runnable runnable) {
        if (sThreadPool != null) {
            THREAD_POOL_EXECUTOR.getQueue().remove(runnable);
        }
    }

    /**
     * 移除对象
     *
     * @param callable 任务
     */
    public void cancel(Callable<?> callable) {
        if (sThreadPool != null) {
            THREAD_POOL_EXECUTOR.getQueue().remove(callable);
        }
    }

    /**
     * 关闭线程池
     */
    public void showDown() {
        THREAD_POOL_EXECUTOR.shutdown();
    }

    /**
     * 关闭线程池后判断所有任务是否都已完成
     *
     * @return 是否完成
     */
    public boolean isTerminated() {
        return THREAD_POOL_EXECUTOR.isTerminated();
    }

}
