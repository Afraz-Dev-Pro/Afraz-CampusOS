package com.afraz.campusos.core.util

import kotlin.math.ceil
import kotlin.math.floor

sealed interface BunkAdvice {
    data object NoClasses : BunkAdvice
    data class SafeToBunk(val classes: Int) : BunkAdvice
    data object OnTheEdge : BunkAdvice
    data class MustAttend(val classes: Int, val targetPercent: Int) : BunkAdvice
}

data class BunkSummary(val attended: Int, val total: Int, val percentage: Double?, val advice: BunkAdvice)

object BunkCalculator {
    fun calculate(attended: Int, total: Int, target: Double = 0.75): BunkSummary {
        require(total >= 0 && attended in 0..total) { "Attendance counts are inconsistent" }
        require(target > 0.0 && target <= 1.0) { "Target must be greater than 0 and at most 100%" }
        if (total == 0) return BunkSummary(attended, total, null, BunkAdvice.NoClasses)

        val fraction = attended.toDouble() / total
        val advice = if (fraction >= target) {
            val safeBunks = floor((attended - target * total) / target).toInt().coerceAtLeast(0)
            if (safeBunks > 0) BunkAdvice.SafeToBunk(safeBunks) else BunkAdvice.OnTheEdge
        } else {
            val required = if (target == 1.0) Int.MAX_VALUE else ceil((target * total - attended) / (1.0 - target)).toInt()
            BunkAdvice.MustAttend(required.coerceAtLeast(0), (target * 100).toInt())
        }
        return BunkSummary(attended, total, fraction * 100.0, advice)
    }
}
