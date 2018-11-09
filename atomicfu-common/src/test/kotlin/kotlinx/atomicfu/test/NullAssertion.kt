package kotlinx.atomicfu.test

import kotlinx.atomicfu.atomic

class Example {
    private val a = atomic<Any?>(null)
    private val something: Any? = "here it is"

    fun f() {
        a.compareAndSet(something!!, null)
    }
}
