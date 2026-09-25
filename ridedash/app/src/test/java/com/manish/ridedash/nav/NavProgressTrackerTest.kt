package com.manish.ridedash.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavProgressTrackerTest {

    @Test
    fun `first reading of a maneuver is zero progress`() {
        val tracker = NavProgressTracker()
        assertEquals(0f, tracker.progressFor("turn right|MG Road|", 400), 0.001f)
        assertTrue(tracker.lastWasNewManeuver)
    }

    @Test
    fun `progress fills as the turn gets closer`() {
        val tracker = NavProgressTracker()
        tracker.progressFor("turn right|MG Road|", 400)
        assertEquals(0.5f, tracker.progressFor("turn right|MG Road|", 200), 0.001f)
        assertEquals(1f, tracker.progressFor("turn right|MG Road|", 0), 0.001f)
        assertFalse(tracker.lastWasNewManeuver)
    }

    @Test
    fun `a new maneuver restarts the bar`() {
        val tracker = NavProgressTracker()
        tracker.progressFor("turn right|MG Road|", 400)
        tracker.progressFor("turn right|MG Road|", 100)
        assertEquals(0f, tracker.progressFor("turn left|Airport Road|", 900), 0.001f)
        assertTrue(tracker.lastWasNewManeuver)
    }

    @Test
    fun `two turns onto the same street are told apart by the distance jumping`() {
        val tracker = NavProgressTracker()
        tracker.progressFor("continue|Ring Road|", 300)
        tracker.progressFor("continue|Ring Road|", 80)
        // Same key, but the turn is suddenly a kilometre away again: that is the next maneuver.
        assertEquals(0f, tracker.progressFor("continue|Ring Road|", 1_000), 0.001f)
        assertTrue(tracker.lastWasNewManeuver)
    }

    @Test
    fun `a slightly larger reading does not push the bar backwards`() {
        val tracker = NavProgressTracker()
        tracker.progressFor("turn right|MG Road|", 300)
        val progress = tracker.progressFor("turn right|MG Road|", 320)
        assertEquals(0f, progress, 0.001f)
        assertFalse(tracker.lastWasNewManeuver)
    }
}
