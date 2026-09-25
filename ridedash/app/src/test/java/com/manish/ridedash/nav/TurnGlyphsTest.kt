package com.manish.ridedash.nav

import org.junit.Assert.assertEquals
import org.junit.Test

class TurnGlyphsTest {

    @Test
    fun `left and right get their own arrows`() {
        assertEquals("↰", TurnGlyphs.forInstruction("Turn left onto MG Road"))
        assertEquals("↱", TurnGlyphs.forInstruction("Turn right onto MG Road"))
    }

    @Test
    fun `slight turns use the diagonals`() {
        assertEquals("↖", TurnGlyphs.forInstruction("Keep left at the fork"))
        assertEquals("↗", TurnGlyphs.forInstruction("Slight right"))
    }

    @Test
    fun `u turns and roundabouts are not read as a plain turn`() {
        assertEquals("↶", TurnGlyphs.forInstruction("Make a U-turn"))
        assertEquals("↻", TurnGlyphs.forInstruction("At the roundabout, take the 2nd exit"))
    }

    @Test
    fun `unknown wording falls back instead of guessing a direction`() {
        assertEquals(TurnGlyphs.FALLBACK, TurnGlyphs.forInstruction(null))
        assertEquals(TurnGlyphs.FALLBACK, TurnGlyphs.forInstruction("Weiter geradeaus"))
    }
}
