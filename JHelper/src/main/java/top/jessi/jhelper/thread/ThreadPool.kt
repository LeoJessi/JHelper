package top.jessi.jhelper.thread

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import top.jessi.jhelper.thread.ThreadPool.execute
import top.jessi.jhelper.thread.ThreadPool.submit
import top.jessi.jhelper.thread.ThreadPool.submitAll
import top.jessi.jhelper.thread.ThreadPool.submitWithTimeout
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * 基于 Kotlin 协程的线程池工具
 *
 * 替代方案：
 * - 简单异步 → [execute]
 * - 需要返回值 → [submit]
 * - 批量执行 → [submitAll]
 * - 带超时 → [submitWithTimeout]
 *
 * 线程模型：
 * - IO 密集型任务 → [Dispatchers.IO]（默认）
 * - CPU 密集型任务 → [Dispatchers.Default]
 * - UI 操作 → [Dispatchers.Main]
 *
 * 使用示例：
 * ```
 * // 执行异步任务
 * val job = ThreadPool.execute { doWork() }
 *
 * // 获取返回值
 * val deferred = ThreadPool.submit { compute() }
 * val result = deferred.await()
 *
 * // 批量执行
 * val results = ThreadPool.submitAll(listOf({ task1() }, { task2() }))
 *
 * // 指定调度器（CPU 密集型）
 * val result = ThreadPool.submit(Dispatchers.Default) { heavyCompute() }
 *
 * // 取消任务
 * ThreadPool.cancel(job)
 *
 * // 关闭线程池
 * ThreadPool.shutdown()
 * ```
 */
object ThreadPool {

    /**
     * 全局协程作用域，使用 SupervisorJob 避免子协程异常传播
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 是否已关闭
     */
    private val isShutdown = AtomicBoolean(false)

    /* ================= 任务提交 ================= */

    /**
     * 提交一个无返回值的异步任务
     *
     * @param dispatcher 调度器，默认 [Dispatchers.IO]
     * @param block 要执行的任务
     * @return [Job]，可用于取消任务或等待完成
     * @throws IllegalStateException 如果线程池已关闭
     */
    @JvmStatic
    fun execute(dispatcher: CoroutineContext = Dispatchers.IO, block: suspend () -> Unit): Job {
        checkNotShutdown()
        return scope.launch(dispatcher) { block() }
    }

    /**
     * 提交一个有返回值的异步任务
     *
     * @param dispatcher 调度器，默认 [Dispatchers.IO]
     * @param block 要执行的任务
     * @return [Deferred]，可调用 [Deferred.await] 获取结果
     * @throws IllegalStateException 如果线程池已关闭
     */
    @JvmStatic
    fun <T> submit(dispatcher: CoroutineContext = Dispatchers.IO, block: suspend () -> T): Deferred<T> {
        checkNotShutdown()
        return scope.async(dispatcher) { block() }
    }

    /**
     * 提交一个 Runnable 任务（兼容旧 API）
     *
     * @param runnable 要执行的任务
     * @return [Job]
     * @throws IllegalStateException 如果线程池已关闭
     */
    @JvmStatic
    fun execute(runnable: Runnable): Job {
        return execute { runnable.run() }
    }

    /**
     * 提交一个 Callable 任务（兼容旧 API）
     *
     * @param callable 要执行的任务
     * @return [Deferred]
     * @throws IllegalStateException 如果线程池已关闭
     */
    @JvmStatic
    fun <T> submit(callable: Callable<T>): Deferred<T> {
        return submit { callable.call() }
    }

    /* ================= 批量操作 ================= */

    /**
     * 批量提交任务并等待全部完成
     *
     * @param dispatcher 调度器，默认 [Dispatchers.IO]
     * @param blocks 任务列表
     * @return 所有任务的结果列表
     * @throws IllegalStateException 如果线程池已关闭
     */
    @JvmStatic
    suspend fun <T> submitAll(dispatcher: CoroutineContext = Dispatchers.IO, blocks: List<suspend () -> T>): List<T> {
        checkNotShutdown()
        return blocks.map { block ->
            scope.async(dispatcher) { block() }
        }.awaitAll()
    }

    /**
     * 批量提交任务（Java 友好版本）
     *
     * @param callables Callable 列表
     * @param dispatcher 调度器，默认 [Dispatchers.IO]
     * @return [Deferred]，可调用 [Deferred.await] 获取结果列表
     * @throws IllegalStateException 如果线程池已关闭
     */
    @JvmStatic
    fun <T> submitAll(callables: List<Callable<T>>, dispatcher: CoroutineContext = Dispatchers.IO): Deferred<List<T>> {
        checkNotShutdown()
        return scope.async {
            callables.map { callable ->
                async(dispatcher) { callable.call() }
            }.awaitAll()
        }
    }

    /* ================= 超时控制 ================= */

    /**
     * 提交一个带超时的任务
     *
     * @param timeoutMillis 超时时间（毫秒）
     * @param dispatcher 调度器，默认 [Dispatchers.IO]
     * @param block 要执行的任务
     * @return 任务结果
     * @throws kotlinx.coroutines.TimeoutCancellationException 超时抛出
     * @throws IllegalStateException 如果线程池已关闭
     */
    @JvmStatic
    suspend fun <T> submitWithTimeout(
        timeoutMillis: Long, dispatcher: CoroutineContext = Dispatchers.IO, block: suspend () -> T
    ): T {
        checkNotShutdown()
        return withTimeout(timeoutMillis) {
            withContext(dispatcher) { block() }
        }
    }

    /* ================= 任务管理 ================= */

    /**
     * 取消指定的任务
     *
     * @param job 要取消的 Job
     * @return true 如果成功取消，false 如果任务已完成或已被取消
     */
    @JvmStatic
    fun cancel(job: Job): Boolean {
        if (job.isActive) {
            job.cancel()
            return true
        }
        return false
    }

    /**
     * 取消所有正在执行的任务并标记线程池为已关闭
     * 关闭后不可重新打开
     *
     * @return true 如果成功取消，false 如果线程池已关闭
     */
    @JvmStatic
    fun cancelAll(): Boolean {
        if (isShutdown.getAndSet(true)) return false
        scope.cancel()
        return true
    }

    /* ================= 状态查询 ================= */

    /**
     * 是否已关闭
     */
    @JvmStatic
    fun isShutdown(): Boolean = isShutdown.get()

    /* ================= 生命周期 ================= */

    /**
     * 关闭线程池，取消所有正在执行的任务
     * 关闭后不可重新打开
     */
    @JvmStatic
    fun shutdown() {
        isShutdown.set(true)
        scope.cancel()
    }

    /**
     * 关闭后检查所有任务是否都已完成
     */
    @JvmStatic
    fun isTerminated(): Boolean {
        return isShutdown.get() && scope.coroutineContext[Job]?.isCompleted == true
    }

    /* ================= 内部方法 ================= */

    private fun checkNotShutdown() {
        check(!isShutdown.get()) { "ThreadPool has been shut down" }
    }
}
