package kotlinx.atomicfu

import kotlin.js.JsName

/**
 * Creates Trace object for tracing atomic operations.
 *
 * Usage: create a separate field for Trace and pass to atomic factory function:
 *
 * ```
 * val trace = Trace(size)
 * val a = atomic(initialValue, trace)
 * ```
 */
@JsName("atomicfu\$Trace\$")
fun Trace(size: Int = 32, format: (AtomicInt, String) -> String = { index, text -> "$index: $text" }): BaseTrace = TraceImpl(size, format)

val NO_TRACE = BaseTrace()

/**
 * Default no-op BaseTrace implementation that can be overridden
 */
public open class BaseTrace {

    @JsName("atomicfu\$BaseTrace\$append\$")
    @PublishedApi
    internal open fun append(text: String) {}

    inline operator fun invoke(text: () -> String) {
        append(text())
    }
}

class TraceImpl(size: Int, val format: (AtomicInt, String) -> String) : BaseTrace() {
    private val s = { size: Int -> var b = 1; while (b < size) b = b shl 1;b } (size)
    private val mask = s - 1
    private val trace = arrayOfNulls<String>(s)
    private val index = atomic(0)

    override fun append(text: String) {
        val i = index.getAndIncrement()
        trace[i and mask] = format(index, text)
    }
}
