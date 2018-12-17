package kotlinx.atomicfu.test

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.loop
import kotlin.test.Test


class Receiver {
    val a = atomic(0)

    fun update() {
        a.loop { ref ->
            when(ref) {
                0 -> a.compareAndSet(0, 10)
                10 -> a.getAndIncrement()
                11 -> return
            }
        }
    }
}

class Inline {
    @Test
    fun inlineReceiverTest() {
        val r = Receiver()
        r.update()
    }
}
