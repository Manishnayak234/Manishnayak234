package com.manish.ridedash.data

/** Which way the bike was laid over when it earned a cheer. */
enum class LeanSide { LEFT, RIGHT }

/**
 * The bit of the dashboard that is purely for fun: a word of encouragement when the bike gets
 * properly laid over.
 *
 * It **latches**. Nobody reads a line of text mid-corner at fifteen degrees of lean — the eyes are
 * a long way up the road where they belong. So the cheer is held for [holdMs] after the lean that
 * earned it, and you get it on the way out of the corner, which is the only moment it could ever
 * be read.
 */
class LeanCheerGate(
    private val triggerDeg: Float = TRIGGER_DEG,
    private val holdMs: Long = HOLD_MS,
) {
    private var side: LeanSide? = null
    private var untilMs = 0L

    fun update(leanDeg: Float?, nowMs: Long): LeanSide? {
        if (leanDeg != null && !leanDeg.isNaN()) {
            when {
                leanDeg <= -triggerDeg -> {
                    side = LeanSide.LEFT
                    untilMs = nowMs + holdMs
                }
                leanDeg >= triggerDeg -> {
                    side = LeanSide.RIGHT
                    untilMs = nowMs + holdMs
                }
            }
        }
        if (nowMs >= untilMs) side = null
        return side
    }

    fun reset() {
        side = null
        untilMs = 0L
    }

    companion object {
        /** Lean enough to be deliberate, not so much that every roundabout sets it off. */
        const val TRIGGER_DEG = 15f

        /** Long enough to read once the bike is upright again. */
        const val HOLD_MS = 3_000L
    }
}
