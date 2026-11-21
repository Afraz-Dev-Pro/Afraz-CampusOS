package com.afraz.campusos.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class BunkCalculatorTest {
    @Test
    fun emptyAttendanceHasNoFailureStatus() {
        val result = BunkCalculator.calculate(attended = 0, total = 0)

        assertEquals(null, result.percentage)
        assertEquals(BunkAdvice.NoClasses, result.advice)
    }

    @Test
    fun targetBoundaryDoesNotAllowAnAbsentClass() {
        val result = BunkCalculator.calculate(attended = 9, total = 12)

        assertEquals(75.0, result.percentage!!, 0.0001)
        assertEquals(BunkAdvice.OnTheEdge, result.advice)
    }

    @Test
    fun safeBunkCountUsesExactPredictiveFormula() {
        val result = BunkCalculator.calculate(attended = 10, total = 12)

        assertEquals(BunkAdvice.SafeToBunk(1), result.advice)
    }

    @Test
    fun recoveryCountReachesTargetAfterRequiredPresentClasses() {
        val result = BunkCalculator.calculate(attended = 6, total = 10)

        assertEquals(BunkAdvice.MustAttend(classes = 6, targetPercent = 75), result.advice)
        assertEquals(75.0, (6.0 + 6.0) / (10.0 + 6.0) * 100.0, 0.0001)
    }
}
