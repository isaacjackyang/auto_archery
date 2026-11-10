package com.autoarchery.app.domain

import org.junit.Assert.*
import org.junit.Test

class RestoreShotPolicyTest {
    @Test fun preservesAvailableIndex() { assertEquals(2, restoredArrowIndex(true, true, false, 6, listOf(1), 2)) }
    @Test fun avoidsIndexReusedByNewDetection() { assertEquals(3, restoredArrowIndex(true, true, false, 6, listOf(1, 2), 2)) }
    @Test fun rejectsFullRound() { assertNull(restoredArrowIndex(true, true, false, 2, listOf(1, 2), 2)) }
    @Test fun rejectsDifferentRound() { assertNull(restoredArrowIndex(true, false, false, 6, emptyList(), 1)) }
    @Test fun rejectsCompletedRound() { assertNull(restoredArrowIndex(true, true, true, 6, emptyList(), 1)) }
    @Test fun rejectsRepeatedRestore() { assertNull(restoredArrowIndex(false, true, false, 6, emptyList(), 1)) }
}
