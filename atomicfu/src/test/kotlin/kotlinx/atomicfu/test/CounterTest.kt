package kotlinx.atomicfu.test

import kotlinx.atomicfu.atomic
import org.junit.Test

class Counter {
    val threads: Int = 20
    val iterations: Int = 100
    var counter: Int = 0
    val busy = atomic(false)

    fun mppIncrement(): Int {
        val workers = Array(threads){IncrementerThread()}
        for (i in 0..threads - 1)
            workers[i] = IncrementerThread()
        for (w in workers)
            w.start()

        for (w in workers) {
            try {
               w.join()
            } catch (e: InterruptedException) { }
        }
        return counter
    }

    inner class IncrementerThread : Thread() {
        override fun run() {
            for (i in 1..iterations) {
                // ensure only one thread in CS
                while (!busy.compareAndSet(false, true)) {}
                val old = counter
                Thread.sleep(1)
                counter = old + 1
                busy.lazySet(false)
            }
        }
    }
}

class CounterTest {
    @Test
    fun testCounter() {
        val c = Counter()
        check(c.mppIncrement() == c.threads * c.iterations)
    }
}

