@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.entao.app.task

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import java.util.concurrent.*


fun mainTask(block: () -> Unit) {
    if (Task.isMainThread) {
        block()
    } else {
        Task.fore(block)
    }
}

fun foreTask(block: () -> Unit) {
    Task.fore(block)
}

fun foreTask(delay: Long, block: () -> Unit) {
    Task.foreDelay(delay, block)
}

fun backTask(block: () -> Unit) {
    Task.back(block)
}

fun backTask(delay: Long, block: () -> Unit) {
    Task.backDelay(delay, block)
}

private val mergeMap: HashMap<String, Runnable> = HashMap()
fun mergeTask(key: String, delay: Long, block: () -> Unit) {
    val r = Runnable(block)
    mergeMap[key] = r
    foreTask(delay) {
        val rr = mergeMap.remove(key)
        if (rr === r) {
            r.run()
        }
    }
}


object Task {
    val foreHandler: Handler by lazy { Handler(Looper.getMainLooper()) }
    val queueHandler: Handler by lazy { Handler(queueThread.looper) }
    private val pool: ScheduledExecutorService by lazy {
        Executors.newScheduledThreadPool(1) { r ->
            PoolThread(r)
        }
    }
    private val queueThread: HandlerThread = HandlerThread("backQueue").apply { start() }

    val isMainThread: Boolean get() = Thread.currentThread().id == Looper.getMainLooper().thread.id

    fun fore(block: () -> Unit) {
        this.foreHandler.post(block)
    }

    fun foreDelay(millSeconds: Long, block: () -> Unit) {
        this.foreHandler.postDelayed(block, millSeconds)
    }

    fun queue(block: () -> Unit) {
        this.queueHandler.post(block)
    }

    fun queueDelay(millSeconds: Long, block: () -> Unit) {
        this.queueHandler.postDelayed(block, millSeconds)
    }

    fun back(block: () -> Unit): Future<*> {
        return pool.submit(block)
    }

    fun backDelay(millSeconds: Long, block: () -> Unit): Future<*> {
        return pool.schedule(block, millSeconds, TimeUnit.MILLISECONDS)
    }

    fun backRepeat(delay: Long, block: () -> Unit): ScheduledFuture<*> {
        return pool.scheduleWithFixedDelay(block, delay, delay, TimeUnit.MILLISECONDS)
    }
}

@Suppress("UNUSED_PARAMETER")
private fun threadUncaughtHandler(thread: Thread, ex: Throwable) {
    ex.printStackTrace()
}

private class PoolThread(r: Runnable) : Thread(r) {
    init {
        isDaemon = true
        priority = Thread.NORM_PRIORITY - 1
        setUncaughtExceptionHandler(::threadUncaughtHandler)
    }
}

