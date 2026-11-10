package com.autoarchery.app.domain

/** Return a collision-free index only while the original round still accepts arrows. */
internal fun restoredArrowIndex(
    isDeleted: Boolean,
    sameRound: Boolean,
    roundCompleted: Boolean,
    expectedCount: Int,
    existingIndices: List<Int>,
    originalIndex: Int,
): Int? {
    if (!isDeleted || !sameRound || roundCompleted || existingIndices.size >= expectedCount) return null
    return if (originalIndex !in existingIndices) originalIndex else (existingIndices.maxOrNull() ?: 0) + 1
}
