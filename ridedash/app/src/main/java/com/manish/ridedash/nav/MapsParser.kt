package com.manish.ridedash.nav

import com.manish.ridedash.data.NavState

/**
 * Turns the ongoing Google Maps notification into a [NavState].
 *
 * There is no Maps SDK for turn-by-turn, so the fields below are whatever Maps happens to put in its
 * notification, and that wording changes between Maps versions and languages. Everything here is
 * therefore deliberately tolerant: it looks for shapes ("a number and a unit", "a clock time", the
 * word "Then") rather than for exact strings, and any field it cannot read comes back null instead of
 * breaking the screen.
 *
 * Feed it real samples before trusting it: [com.manish.ridedash.service.MapsNotificationListener]
 * logs every field under the tag RideDash/MapsRaw during a live navigation.
 */
object MapsParser {

    const val MAPS_PACKAGE = "com.google.android.apps.maps"

    /** The notification extras we care about, already pulled out of the Bundle. */
    data class Fields(
        val title: String? = null,
        val text: String? = null,
        val bigText: String? = null,
        val subText: String? = null,
    )

    private val distanceRegex = Regex(
        """(\d+(?:[.,]\d+)?)\s*(km|m|mi|ft|feet|yd)\b""",
        RegexOption.IGNORE_CASE,
    )

    /** "Then turn left onto X", "Dann ...", "Then" is the English one we key on. */
    private val thenRegex = Regex(
        """(?:^|\n)\s*then\b[\s:]*(.+)""",
        setOf(RegexOption.IGNORE_CASE),
    )

    private val clockRegex = Regex(
        """^\s*\d{1,2}[:.]\d{2}(?:\s*(?:am|pm|AM|PM))?\s*$""",
    )

    private val durationRegex = Regex(
        """\d+\s*(?:h|hr|hrs|hour|hours|min|mins|minute|minutes|d|day|days)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(fields: Fields): NavState? {
        val title = fields.title?.trim().orEmpty()
        val text = fields.text?.trim().orEmpty()
        val bigText = fields.bigText?.trim().orEmpty()
        val subText = fields.subText?.trim().orEmpty()

        if (title.isEmpty() && text.isEmpty() && bigText.isEmpty()) return null

        // Distance to the maneuver: normally the whole title ("350 m"), sometimes inside the
        // instruction ("In 350 m, turn right"), occasionally only in the body text.
        val titleDistance = findDistance(title)
        val distance = titleDistance ?: findDistance(text) ?: findDistance(bigText)

        // What the title says to do, with the distance taken out of it.
        val instruction = if (titleDistance != null) {
            instructionAround(title, titleDistance)
        } else {
            tidy(title).takeIf { it.isNotEmpty() }
        }

        val body = if (text.isNotEmpty()) text else bigText
        val thenSource = if (bigText.isNotEmpty()) bigText else text
        val thenStreet = thenRegex.find(thenSource)
            ?.groupValues
            ?.get(1)
            ?.lineSequence()
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        // The street is the body's first line, minus a "Then ..." line that belongs to the next turn.
        val street = body.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("then", ignoreCase = true) }
            .firstOrNull()
            .orEmpty()

        val summary = parseSubText(subText)

        return NavState(
            distanceValue = distance?.value.orEmpty(),
            distanceUnit = distance?.unit.orEmpty(),
            distanceMeters = distance?.meters,
            instruction = instruction,
            street = street,
            thenStreet = thenStreet,
            etaClock = summary.etaClock,
            remainingDistance = summary.remainingDistance,
            remainingTime = summary.remainingTime,
            maneuverKey = maneuverKey(instruction, street, thenStreet),
        )
    }

    /**
     * ETA row: Maps packs it into one line, usually "12 min · 4.2 km · 18:42", but the order and the
     * separator move around, so each chunk is classified by what it looks like.
     */
    fun parseSubText(subText: String): Summary {
        if (subText.isBlank()) return Summary()
        var etaClock: String? = null
        var remainingDistance: String? = null
        var remainingTime: String? = null

        subText.split('·', '•', '|', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { chunk ->
                when {
                    clockRegex.matches(chunk) -> etaClock = etaClock ?: chunk
                    durationRegex.containsMatchIn(chunk) -> remainingTime = remainingTime ?: chunk
                    distanceRegex.containsMatchIn(chunk) ->
                        remainingDistance = remainingDistance ?: chunk
                }
            }

        return Summary(etaClock, remainingDistance, remainingTime)
    }

    /**
     * The words around a distance inside the title. "In 500 m, turn left" is the instruction "turn
     * left"; "Turn right in 200 m" is "Turn right"; a bare "350 m" has no instruction at all. Taking
     * the longer side gets all three right without knowing which shape Maps used.
     */
    private fun instructionAround(title: String, distance: Distance): String? {
        val before = tidy(title.take(distance.start))
        val after = tidy(title.substring(distance.start + distance.raw.length))
        return listOf(before, after).maxByOrNull { it.length }?.takeIf { it.isNotEmpty() }
    }

    /**
     * Trims punctuation, and the connector word the distance leaves hanging: "Turn right in 200 m"
     * gives "Turn right in" once the distance is cut out, and the rider wants "Turn right".
     */
    private fun tidy(fragment: String): String {
        var text = fragment.trim(*TRIM_CHARS)
        while (true) {
            val lastWord = text.substringAfterLast(' ', "").lowercase()
            if (lastWord.isEmpty() || lastWord !in TRAILING_CONNECTORS) return text
            text = text.substringBeforeLast(' ').trim(*TRIM_CHARS)
        }
    }

    fun findDistance(source: String): Distance? {
        val match = distanceRegex.find(source) ?: return null
        val rawValue = match.groupValues[1]
        val unit = match.groupValues[2].lowercase()
        val number = rawValue.replace(',', '.').toDoubleOrNull() ?: return null
        val meters = when (unit) {
            "km" -> number * 1000
            "m" -> number
            "mi" -> number * 1609.34
            "ft", "feet" -> number * 0.3048
            "yd" -> number * 0.9144
            else -> return null
        }
        return Distance(
            value = rawValue,
            unit = match.groupValues[2],
            meters = meters.toInt(),
            raw = match.value,
            start = match.range.first,
        )
    }

    /**
     * Identity of the current maneuver. Maps gives us no id, so the instruction plus the streets is
     * the best available stand-in; [NavProgressTracker] also treats a big jump in distance as a new
     * maneuver, which covers two turns in a row onto the same street.
     */
    fun maneuverKey(instruction: String?, street: String, thenStreet: String?): String =
        listOf(instruction.orEmpty(), street, thenStreet.orEmpty())
            .joinToString("|") { it.lowercase().replace(Regex("""\s+"""), " ").trim() }

    data class Distance(
        val value: String,
        val unit: String,
        val meters: Int,
        /** The matched substring, so it can be cut out of the instruction. */
        val raw: String,
        /** Where the match starts in the source string. */
        val start: Int = 0,
    )

    private val TRAILING_CONNECTORS = setOf("in", "after", "for", "within", "at", "then")

    private val TRIM_CHARS = charArrayOf(' ', ',', '\u00B7', '-', '\u2013', ':', '.')

    data class Summary(
        val etaClock: String? = null,
        val remainingDistance: String? = null,
        val remainingTime: String? = null,
    )
}
