package top.jessi.jhelper.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Jessi on 2022/8/15 16:59
 * Email：17324719944@189.cn
 * Describe：创建线程池的工厂对象
 */
public class ThreadCustomFactoryManager implements ThreadFactory {
    private final String poolName;
    private final AtomicInteger count = new AtomicInteger(0);

    public ThreadCustomFactoryManager(String poolName) {
        this.poolName = poolName;
    }

    /**
     * 该方法用来创建线程，给线程命名
     *
     * @param r 进程
     * @return 线程
     */
    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        String nowThreadName = poolName + count.addAndGet(1);
        thread.setName(nowThreadName);
        return thread;
    }
}