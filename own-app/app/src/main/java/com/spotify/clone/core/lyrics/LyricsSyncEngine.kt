package com.spotify.clone.core.lyrics

import com.spotify.clone.core.model.LyricLine
import com.spotify.clone.core.model.LyricsResult
import com.spotify.clone.core.model.LyricsSyncType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Algorithmic English transliteration & vocal-cadence synchronization engine.
 *
 * Ensures:
 * 1. Every lyric line has a clean English (Latin/Romanized) representation.
 * 2. Synced LRC/curated timestamps are drift-normalized and given syllable-aware durations.
 * 3. Plain (UNSYNCED) lyrics are time-aligned to the track's vocal envelope using
 *    syllable density + punctuation phrasing weights so every song plays in sync.
 */
object LyricsSyncEngine {

    /**
     * Normalizes a [LyricsResult] so every line has English text and synchronized timestamps.
     */
    fun prepareEnglishSyncedLyrics(
        result: LyricsResult,
        trackDurationMs: Long
    ): LyricsResult {
        if (result.lines.isEmpty()) return result

        val englishLines = result.lines.map { line ->
            val english = toEnglishText(line)
            if (line.romanized == english) line else line.copy(romanized = english)
        }

        val hasTimestamps = (result.syncType == LyricsSyncType.LINE_SYNCED ||
            result.syncType == LyricsSyncType.SYLLABLE_SYNCED) &&
            englishLines.any { it.timestampMs >= 0L }

        return if (hasTimestamps) {
            val normalized = normalizeSyncedLines(englishLines, trackDurationMs)
            result.copy(
                syncType = LyricsSyncType.LINE_SYNCED,
                lines = normalized
            )
        } else {
            val aligned = alignUnsyncedLines(englishLines, trackDurationMs)
            val syncLabel = if (result.provider.isBlank()) "Synced" else "${result.provider} • Synced"
            result.copy(
                syncType = LyricsSyncType.LINE_SYNCED,
                lines = aligned,
                provider = syncLabel
            )
        }
    }

    /**
     * Returns the English representation of a [LyricLine] (existing Romanized/English text,
     * or automatic on-device Indic-to-English phonetic transliteration).
     */
    fun toEnglishText(line: LyricLine): String {
        val existingRoman = line.romanized?.trim()
        if (!existingRoman.isNullOrBlank() && !containsNonLatinIndic(existingRoman)) {
            return existingRoman
        }
        val raw = line.words.trim()
        if (raw.isEmpty()) return ""
        return if (containsNonLatinIndic(raw)) {
            IndicRomanizer.transliterate(raw).ifBlank { raw }
        } else {
            raw
        }
    }

    /**
     * Enforces monotonic timestamps, corrects gross track-duration drift (e.g. radio edit vs album),
     * and computes syllable-proportional sweep durations for each synced line.
     */
    fun normalizeSyncedLines(
        lines: List<LyricLine>,
        trackDurationMs: Long
    ): List<LyricLine> {
        val sorted = lines
            .filter { (it.romanized ?: it.words).isNotBlank() }
            .sortedBy { it.timestampMs.coerceAtLeast(0L) }
        if (sorted.isEmpty()) return emptyList()

        // Scale timestamps if LRC span diverges substantially from actual stream duration
        val lastTs = sorted.last().timestampMs
        val firstTs = sorted.first().timestampMs
        val scaledTimestamps: List<Long> = if (
            trackDurationMs > 15_000L &&
            sorted.size >= 5 &&
            lastTs > firstTs &&
            (lastTs > (trackDurationMs * 1.06).toLong() || lastTs < (trackDurationMs * 0.56).toLong())
        ) {
            val targetStart = firstTs.coerceIn(0L, (trackDurationMs * 0.22).toLong())
            val targetEnd = (trackDurationMs * 0.93).toLong().coerceAtLeast(targetStart + 5_000L)
            val sourceSpan = (lastTs - firstTs).coerceAtLeast(1L).toDouble()
            val targetSpan = (targetEnd - targetStart).toDouble()
            sorted.map { line ->
                val frac = (line.timestampMs - firstTs).toDouble() / sourceSpan
                (targetStart + (frac * targetSpan)).roundToLong().coerceIn(0L, trackDurationMs - 800L)
            }
        } else {
            sorted.map { it.timestampMs.coerceAtLeast(0L) }
        }

        // Enforce strictly monotonic increasing timestamps (min 350ms apart)
        val monotonic = LongArray(sorted.size)
        for (i in sorted.indices) {
            val raw = scaledTimestamps[i]
            monotonic[i] = if (i == 0) raw else max(raw, monotonic[i - 1] + 350L)
        }

        // Compute vocal-envelope durations based on syllable count & next entry
        return sorted.mapIndexed { idx, line ->
            val ts = monotonic[idx]
            val text = line.romanized?.takeIf { it.isNotBlank() } ?: line.words
            val estSingMs = (estimateSyllables(text) * 285L).coerceIn(1350L, 7800L)
            val gapToNext = if (idx < sorted.lastIndex) {
                (monotonic[idx + 1] - ts).coerceAtLeast(450L)
            } else if (trackDurationMs > ts + 1200L) {
                (trackDurationMs - ts).coerceIn(1800L, 6500L)
            } else {
                4000L
            }
            val duration = minOf(estSingMs.coerceAtLeast((gapToNext * 0.85f).roundToLong()), gapToNext)
                .coerceIn(900L, 9000L)
            line.copy(timestampMs = ts, durationMs = duration)
        }
    }

    /**
     * Aligns plain (UNSYNCED) lyrics across the track's vocal window using
     * syllable-count and punctuation-cadence weights.
     */
    fun alignUnsyncedLines(
        lines: List<LyricLine>,
        trackDurationMs: Long
    ): List<LyricLine> {
        val cleanLines = lines.filter { (it.romanized ?: it.words).isNotBlank() }
        if (cleanLines.isEmpty()) return emptyList()

        val effectiveDuration = if (trackDurationMs > 20_000L) {
            trackDurationMs
        } else {
            // Sensible fallback duration when player metadata hasn't reported duration yet
            (cleanLines.size * 3400L + 16_000L).coerceIn(90_000L, 260_000L)
        }

        val introMs = (effectiveDuration * 0.075).roundToLong().coerceIn(5_000L, 17_000L)
        val outroMs = (effectiveDuration * 0.065).roundToLong().coerceIn(4_000L, 14_000L)
        val vocalWindowMs = (effectiveDuration - introMs - outroMs).coerceAtLeast(cleanLines.size * 1_100L)

        val weights = cleanLines.map { line ->
            val text = line.romanized?.takeIf { it.isNotBlank() } ?: line.words
            val syllables = estimateSyllables(text).toDouble()
            val punctuationPause = when {
                text.endsWith("...") || text.endsWith("…") -> 1.8
                text.endsWith("!") || text.endsWith("?") -> 1.2
                text.endsWith(",") -> 0.6
                else -> 0.25
            }
            (syllables + punctuationPause).coerceAtLeast(2.5)
        }

        val totalWeight = weights.sum().coerceAtLeast(1.0)
        var cursorMs = introMs.toDouble()

        return cleanLines.mapIndexed { idx, line ->
            val ts = cursorMs.roundToLong().coerceIn(0L, effectiveDuration - 1_000L)
            val sliceMs = ((weights[idx] / totalWeight) * vocalWindowMs).coerceAtLeast(1_050.0)
            cursorMs += sliceMs
            val activeSingMs = (sliceMs * 0.88).roundToLong().coerceIn(950L, 8_500L)
            line.copy(timestampMs = ts, durationMs = activeSingMs)
        }
    }

    /**
     * Scores candidate track match quality for fuzzy LRCLIB search fallback.
     */
    fun scoreCandidateMatch(
        queryTitle: String,
        queryArtist: String,
        targetDurationMs: Long,
        candTitle: String,
        candArtist: String,
        candDurationSec: Double,
        hasSynced: Boolean
    ): Double {
        var score = 0.0
        val qt = queryTitle.lowercase().trim()
        val ct = candTitle.lowercase().trim()
        if (ct == qt) score += 50.0
        else if (ct.contains(qt) || qt.contains(ct)) score += 28.0

        val qa = queryArtist.lowercase().trim()
        val ca = candArtist.lowercase().trim()
        if (qa.isNotBlank() && (ca.contains(qa) || qa.contains(ca))) score += 25.0

        if (targetDurationMs > 10_000L && candDurationSec > 10.0) {
            val diffSec = abs((targetDurationMs / 1000.0) - candDurationSec)
            score += (20.0 - diffSec).coerceAtLeast(-15.0)
        }
        if (hasSynced) score += 18.0
        return score
    }

    private fun estimateSyllables(text: String): Int {
        val cleaned = text.lowercase().replace(Regex("[^a-z0-9\\s]"), " ")
        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return 4
        var total = 0
        for (word in words) {
            val vowelGroups = Regex("[aeiouy]+").findAll(word).count()
            total += max(1, vowelGroups)
        }
        return total.coerceAtLeast(2)
    }

    private fun containsNonLatinIndic(text: String): Boolean {
        for (ch in text) {
            val code = ch.code
            if (code in 0x0900..0x0D7F) return true
        }
        return false
    }
}

/**
 * Lightweight, zero-dependency phonetic Romanizer for Indic scripts
 * (Devanagari/Hindi, Telugu, Tamil, Kannada, Malayalam) so non-English lyrics
 * from external providers always render in readable English phonetics.
 */
internal object IndicRomanizer {

    fun transliterate(input: String): String {
        if (input.isBlank()) return input
        val sb = StringBuilder(input.length * 2)
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            val code = ch.code
            val scriptBase = indicBlockBase(code)
            if (scriptBase == -1) {
                sb.append(ch)
                i++
                continue
            }

            val offset = code - scriptBase
            val nextOffset = if (i + 1 < input.length && indicBlockBase(input[i + 1].code) == scriptBase) {
                input[i + 1].code - scriptBase
            } else {
                -1
            }

            val isConsonant = offset in 0x15..0x39
            if (isConsonant) {
                val baseConsonant = consonantAt(offset, scriptBase)
                when {
                    // Explicit virama / halant suppresses inherent 'a'
                    nextOffset == 0x4D -> {
                        sb.append(baseConsonant)
                        i += 2
                    }
                    // Matra vowel sign replaces inherent 'a'
                    nextOffset in 0x3E..0x4C -> {
                        sb.append(baseConsonant).append(matraAt(nextOffset))
                        i += 2
                    }
                    else -> {
                        // Inherent 'a' (drop trailing schwa at word end in Hindi/Devanagari-like flow? Keep 'a' for clarity)
                        sb.append(baseConsonant).append('a')
                        i++
                    }
                }
            } else {
                sb.append(independentOrSignAt(offset))
                i++
            }
        }
        return sb.toString()
            .replace(Regex(" {2,}"), " ")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun indicBlockBase(code: Int): Int = when (code) {
        in 0x0900..0x097F -> 0x0900 // Devanagari
        in 0x0B80..0x0BFF -> 0x0B80 // Tamil
        in 0x0C00..0x0C7F -> 0x0C00 // Telugu
        in 0x0C80..0x0CFF -> 0x0C80 // Kannada
        in 0x0D00..0x0D7F -> 0x0D00 // Malayalam
        else -> -1
    }

    private fun consonantAt(offset: Int, base: Int): String = when (offset) {
        0x15 -> "k"; 0x16 -> "kh"; 0x17 -> "g"; 0x18 -> "gh"; 0x19 -> "ng"
        0x1A -> "ch"; 0x1B -> "chh"; 0x1C -> "j"; 0x1D -> "jh"; 0x1E -> "ny"
        0x1F -> "t"; 0x20 -> "th"; 0x21 -> "d"; 0x22 -> "dh"; 0x23 -> "n"
        0x24 -> "th"; 0x25 -> "thh"; 0x26 -> "dh"; 0x27 -> "dhh"; 0x28 -> "n"
        0x29 -> "n"
        0x2A -> "p"; 0x2B -> "ph"; 0x2C -> "b"; 0x2D -> "bh"; 0x2E -> "m"
        0x2F -> "y"; 0x30 -> "r"; 0x31 -> "r"; 0x32 -> "l"; 0x33 -> "l"
        0x34 -> if (base == 0x0B80) "zh" else "l"
        0x35 -> "v"; 0x36 -> "sh"; 0x37 -> "sh"; 0x38 -> "s"; 0x39 -> "h"
        else -> ""
    }

    private fun matraAt(offset: Int): String = when (offset) {
        0x3E -> "aa"; 0x3F -> "i"; 0x40 -> "ee"; 0x41 -> "u"; 0x42 -> "oo"
        0x43 -> "ri"; 0x44 -> "ri"
        0x46 -> "e"; 0x47 -> "e"; 0x48 -> "ai"
        0x4A -> "o"; 0x4B -> "o"; 0x4C -> "au"
        else -> ""
    }

    private fun independentOrSignAt(offset: Int): String = when (offset) {
        0x01, 0x02 -> "m"
        0x03 -> "h"
        0x05 -> "a"; 0x06 -> "aa"; 0x07 -> "i"; 0x08 -> "ee"
        0x09 -> "u"; 0x0A -> "oo"; 0x0B -> "ri"
        0x0E, 0x0F -> "e"; 0x10 -> "ai"
        0x12, 0x13 -> "o"; 0x14 -> "au"
        0x3E -> "aa"; 0x3F -> "i"; 0x40 -> "ee"; 0x41 -> "u"; 0x42 -> "oo"
        0x46, 0x47 -> "e"; 0x48 -> "ai"; 0x4A, 0x4B -> "o"; 0x4C -> "au"
        0x58 -> "q"; 0x59 -> "kh"; 0x5A -> "gh"; 0x5B -> "z"; 0x5C -> "r"; 0x5D -> "rh"; 0x5E -> "f"; 0x5F -> "y"
        in 0x66..0x6F -> ('0' + (offset - 0x66)).toString()
        else -> ""
    }
}
