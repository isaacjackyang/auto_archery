package com.autoarchery.app.vision

import java.util.concurrent.atomic.AtomicInteger

/** One immutable frame per emitted event. A stale callback can only complete its own event. */
internal class PendingShotCommit(frame: FloatArray, val count: Int) {
    val frame = frame.copyOf()
    private val result = AtomicInteger(0)
    val accepted: Boolean? get() = when (result.get()) { 1 -> true; 2 -> false; else -> null }
    fun acknowledge(accepted: Boolean) { result.compareAndSet(0, if (accepted) 1 else 2) }
}

/** A decrease after the acknowledged count was observed is a manual edit, not commit latency. */
internal fun waitForCommittedCount(baselineCount: Int?, observedCount: Int, lastObservedCount: Int): Boolean =
    baselineCount != null && baselineCount > observedCount && observedCount == lastObservedCount
