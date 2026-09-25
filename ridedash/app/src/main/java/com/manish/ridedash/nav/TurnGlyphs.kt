package com.manish.ridedash.nav

/**
 * Text arrows for the watch notification. The watch mirrors phone notifications as plain text, so an
 * arrow character travels and an icon does not.
 *
 * The maneuver type is not in the notification either, so the arrow is picked from the wording. An
 * unknown instruction falls back to a neutral turn arrow rather than guessing a direction.
 */
object TurnGlyphs {

    const val FALLBACK = "↑" // up arrow

    private val rules = listOf(
        setOf("u-turn", "u turn", "make a u") to "↶",
        setOf("roundabout", "rotary", "circle") to "↻",
        setOf("sharp left") to "↖",
        setOf("sharp right") to "↗",
        setOf("slight left", "keep left", "stay left", "bear left") to "↖",
        setOf("slight right", "keep right", "stay right", "bear right") to "↗",
        setOf("exit left", "ramp left") to "↰",
        setOf("exit right", "ramp right") to "↱",
        setOf("left") to "↰",
        setOf("right") to "↱",
        setOf("straight", "continue", "head", "merge") to "↑",
        setOf("destination", "arrive") to "◎",
    )

    fun forInstruction(instruction: String?): String {
        val text = instruction?.lowercase() ?: return FALLBACK
        rules.forEach { (keywords, glyph) ->
            if (keywords.any { text.contains(it) }) return glyph
        }
        return FALLBACK
    }
}
