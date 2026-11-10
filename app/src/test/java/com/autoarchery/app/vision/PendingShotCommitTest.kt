package com.autoarchery.app.vision

import org.junit.Assert.*
import org.junit.Test

class PendingShotCommitTest {
    @Test fun waitsForUiToObserveCommittedCount() { assertTrue(waitForCommittedCount(3, 2, 2)) }
    @Test fun manualDeletionDoesNotWaitForever() { assertFalse(waitForCommittedCount(3, 2, 3)) }
    @Test fun catchesUpWhenCommittedCountArrives() { assertFalse(waitForCommittedCount(3, 3, 2)) }
    @Test fun waitsForExplicitAcknowledgementAndCopiesFrame() {
        val source = floatArrayOf(10f, 20f)
        val pending = PendingShotCommit(source, 2)
        source[0] = 255f
        assertNull(pending.accepted)
        assertEquals(10f, pending.frame[0], 0f)
        pending.acknowledge(true)
        assertEquals(true, pending.accepted)
    }
    @Test fun rejectionCannotBeOverwrittenByLateSuccess() {
        val pending = PendingShotCommit(floatArrayOf(10f), 0)
        pending.acknowledge(false)
        pending.acknowledge(true)
        assertEquals(false, pending.accepted)
    }
    @Test fun oldSessionCallbackCannotCompleteNewEvent() {
        val old = PendingShotCommit(floatArrayOf(10f), 0)
        val current = PendingShotCommit(floatArrayOf(20f), 0)
        old.acknowledge(true)
        assertNull(current.accepted)
    }
}
