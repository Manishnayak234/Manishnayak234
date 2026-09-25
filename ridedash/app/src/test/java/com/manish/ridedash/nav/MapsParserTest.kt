package com.manish.ridedash.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Samples in the shapes Google Maps has been seen to use. When a real phone throws up a shape that is
 * not here, add it from the RideDash/MapsRaw log before touching the parser.
 */
class MapsParserTest {

    @Test
    fun `distance only title`() {
        val nav = MapsParser.parse(
            MapsParser.Fields(
                title = "350 m",
                text = "MG Road",
                subText = "12 min · 4.2 km · 18:42",
            )
        )!!

        assertEquals("350", nav.distanceValue)
        assertEquals("m", nav.distanceUnit)
        assertEquals(350, nav.distanceMeters)
        assertEquals("MG Road", nav.street)
        assertNull(nav.instruction)
        assertEquals("18:42", nav.etaClock)
        assertEquals("4.2 km", nav.remainingDistance)
        assertEquals("12 min", nav.remainingTime)
    }

    @Test
    fun `kilometres are converted to metres`() {
        val nav = MapsParser.parse(MapsParser.Fields(title = "1,2 km", text = "Hosur Road"))!!
        assertEquals(1200, nav.distanceMeters)
        assertEquals("1,2", nav.distanceValue)
    }

    @Test
    fun `instruction in the title with the distance inside it`() {
        val nav = MapsParser.parse(
            MapsParser.Fields(title = "In 500 m, turn left", text = "Outer Ring Road")
        )!!

        assertEquals(500, nav.distanceMeters)
        assertEquals("turn left", nav.instruction)
        assertEquals("Outer Ring Road", nav.street)
    }

    @Test
    fun `instruction before the distance`() {
        val nav = MapsParser.parse(MapsParser.Fields(title = "Turn right in 200 m"))!!
        assertEquals(200, nav.distanceMeters)
        assertEquals("Turn right", nav.instruction)
    }

    @Test
    fun `worded instruction with no distance`() {
        val nav = MapsParser.parse(
            MapsParser.Fields(
                title = "Turn right onto MG Road",
                text = "Then Airport Road",
                subText = "24 min · 12 km · 19:02",
            )
        )!!

        assertNull(nav.distanceMeters)
        assertEquals("", nav.distanceValue)
        assertEquals("Turn right onto MG Road", nav.instruction)
        assertEquals("Airport Road", nav.thenStreet)
        assertEquals("", nav.street)
    }

    @Test
    fun `then line comes out of big text and stays out of the street`() {
        val nav = MapsParser.parse(
            MapsParser.Fields(
                title = "1.2 km",
                text = "Hosur Road",
                bigText = "Hosur Road\nThen turn left onto Sarjapur Road",
            )
        )!!

        assertEquals("Hosur Road", nav.street)
        assertEquals("turn left onto Sarjapur Road", nav.thenStreet)
    }

    @Test
    fun `sub text order does not matter`() {
        val summary = MapsParser.parseSubText("19:02 · 12 km · 24 min")
        assertEquals("19:02", summary.etaClock)
        assertEquals("12 km", summary.remainingDistance)
        assertEquals("24 min", summary.remainingTime)
    }

    @Test
    fun `twelve hour clock and hours in the duration`() {
        val summary = MapsParser.parseSubText("1 hr 5 min · 2.1 mi · 6:42 PM")
        assertEquals("6:42 PM", summary.etaClock)
        assertEquals("2.1 mi", summary.remainingDistance)
        assertEquals("1 hr 5 min", summary.remainingTime)
    }

    @Test
    fun `imperial distances are converted`() {
        val nav = MapsParser.parse(MapsParser.Fields(title = "0.2 mi", text = "Main St"))!!
        assertEquals(321, nav.distanceMeters)
        assertEquals("mi", nav.distanceUnit)
    }

    @Test
    fun `empty notification is not navigation`() {
        assertNull(MapsParser.parse(MapsParser.Fields()))
        assertNull(MapsParser.parse(MapsParser.Fields(subText = "12 min · 4 km")))
    }

    @Test
    fun `maneuver key changes with the street`() {
        val first = MapsParser.parse(MapsParser.Fields(title = "350 m", text = "MG Road"))!!
        val closer = MapsParser.parse(MapsParser.Fields(title = "120 m", text = "MG Road"))!!
        val next = MapsParser.parse(MapsParser.Fields(title = "900 m", text = "Airport Road"))!!

        assertEquals(first.maneuverKey, closer.maneuverKey)
        assertEquals(false, first.maneuverKey == next.maneuverKey)
    }
    // --- Samples captured verbatim from Google Maps on the iQOO 9 SE, Android 14, via
    // --- `adb logcat -s RideDash/MapsRaw`. This build leaves the title empty or bare, puts the
    // --- command in the text, and writes the ETA as "3:58 am ETA".

    @Test
    fun `real sample - route just started, title empty and command in the text`() {
        val nav = MapsParser.parse(
            MapsParser.Fields(
                title = "",
                text = "Head southwest",
                bigText = null,
                subText = "6 min \u00b7 3.1 km \u00b7 3:58 am ETA",
            )
        )

        assertNotNull(nav)
        assertEquals("Head southwest", nav!!.instruction)
        assertEquals("6 min", nav.remainingTime)
        assertEquals("3.1 km", nav.remainingDistance)
        assertEquals("3:58 am", nav.etaClock)
    }

    @Test
    fun `real sample - title carries the bare distance`() {
        val nav = MapsParser.parse(
            MapsParser.Fields(
                title = "0 m",
                text = "Head southwest",
                bigText = null,
                subText = "6 min \u00b7 3.1 km \u00b7 4:00 am ETA",
            )
        )

        assertNotNull(nav)
        assertEquals("0", nav!!.distanceValue)
        assertEquals("m", nav.distanceUnit)
        assertEquals("Head southwest", nav.instruction)
        assertEquals("4:00 am", nav.etaClock)
    }

    @Test
    fun `command and street split apart so the rider sees both`() {
        val nav = MapsParser.parse(
            MapsParser.Fields(title = "200 m", text = "Turn left onto MG Road", subText = null)
        )

        assertNotNull(nav)
        assertEquals("200", nav!!.distanceValue)
        assertEquals("Turn left", nav.instruction)
        assertEquals("MG Road", nav.street)
    }

    @Test
    fun `a trailing ETA label does not swallow the clock`() {
        val summary = MapsParser.parseSubText("6 min \u00b7 3.1 km \u00b7 3:58 am ETA")

        assertEquals("3:58 am", summary.etaClock)
        assertEquals("3.1 km", summary.remainingDistance)
        assertEquals("6 min", summary.remainingTime)
    }

    @Test
    fun `a decimal distance is never mistaken for a clock`() {
        val summary = MapsParser.parseSubText("12 min \u00b7 3.10 km \u00b7 18:42")

        assertEquals("3.10 km", summary.remainingDistance)
        assertEquals("18:42", summary.etaClock)
    }

}
