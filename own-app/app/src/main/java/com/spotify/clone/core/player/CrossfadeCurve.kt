package com.spotify.clone.core.player

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Equal-power gain law for Automix & Crossfade transitions
 * (ported from LastWave-Native CrossfadeCurve).
 *
 * Ensures incomingGain^2 + outgoingGain^2 == 1.0 across the full blend window
 * so perceived loudness stays constant without a midpoint dip, plus headroom
 * limiting (`1 / sqrt(in + out)`) to prevent digital clipping when two loud
 * masters overlap.
 */
object CrossfadeCurve {

    const val MIN_OVERLAP_MS = 250L
    private val HALF_PI = (PI / 2.0).toFloat()

    /** Outgoing fader gain at [progress] (0f = full, 1f = silent). */
    fun outgoingGain(progress: Float): Float =
        cos(progress.coerceIn(0f, 1f) * HALF_PI)

    /** Incoming fader gain at [progress] (0f = silent, 1f = full). */
    fun incomingGain(progress: Float): Float =
        sin(progress.coerceIn(0f, 1f) * HALF_PI)

    /**
     * Master headroom trim preventing peak clipping when two full-scale signals sum.
     */
    fun headroom(incomingGain: Float, outgoingGain: Float): Float {
        val sum = incomingGain.coerceIn(0f, 1f) + outgoingGain.coerceIn(0f, 1f)
        return if (sum <= 1f) 1f else 1f / sqrt(sum)
    }
}
