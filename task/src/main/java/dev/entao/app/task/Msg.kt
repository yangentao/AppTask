@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package dev.entao.app.task

import androidx.annotation.Keep
import java.lang.ref.WeakReference
import kotlin.reflect.KClass


fun String.fire(block: Msg.() -> Unit) {
    val m = Msg(this)
    m.block()
    MsgCenter.fire(m)
}


private val KClass<*>.msgID: String get() = this.qualifiedName!!

class Msg(val msg: String) {
    var stoped: Boolean = false
    var result = ArrayList<Any>()
    var backThread: Boolean = false

    var valueInt: Int = 0
    var valueInt2: Int = 0
    var valueLong: Long = 0
    var valueLong2: Long = 0
    var valueDouble: Double = 0.0
    var valueDouble2: Double = 0.0
    var valueString: String = ""
    var valueString2: String = ""
    var valueBool: Boolean = false
    var valueBool2: Boolean = false
    var valueAny: Any? = null

    var onFinishCallback: (Msg) -> Unit = {}

    constructor(cls: KClass<*>) : this(cls.msgID)

    inline fun <reified T : Any> anyValue(): T? {
        return valueAny as? T
    }


    override fun hashCode(): Int {
        return msg.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return this === other
    }


    fun isMsg(vararg msgs: String): Boolean {
        return this.msg in msgs
    }


    fun isMsg(vararg classes: KClass<*>): Boolean {
        return classes.find { this.msg == it.msgID } != null
    }

    operator fun contains(msg: String): Boolean {
        return this.msg == msg
    }


    operator fun contains(msg: KClass<*>): Boolean {
        return this.msg == msg.msgID
    }

    fun fire() {
        MsgCenter.fire(this)
    }
}


fun interface MsgListener {
    fun onMsg(msg: Msg)
}

@Keep
object MsgCenter {
    private val allList = ArrayList<WeakReference<MsgListener>>()

    @Synchronized
    fun add(listener: MsgListener) {
        for (wl in allList) {
            if (wl.get() === listener) {
                return
            }
        }
        allList.add(WeakReference(listener))
    }


    @Synchronized
    fun remove(listener: MsgListener) {
        allList.removeAll { it.get() == null || it.get() === listener }
    }

    fun fire(msg: Msg) {
        if (msg.backThread) {
            Task.back {
                fireCurrent(msg)
            }
        } else {
            Task.fore {
                fireCurrent(msg)
            }
        }

    }

    private fun fireCurrent(msg: Msg) {
        val ls2 = ArrayList<MsgListener>()
        synchronized(this) {
            allList.mapNotNullTo(ls2) { it.get() }
            allList.retainAll { it.get() != null }
        }

        for (l in ls2) {
            if (msg.stoped) {
                break
            } else {
                l.onMsg(msg)
            }
        }
        msg.onFinishCallback(msg)
        msg.onFinishCallback = {}
    }


}