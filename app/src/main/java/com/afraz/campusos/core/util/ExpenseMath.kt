package com.afraz.campusos.core.util

import java.math.BigDecimal
import java.math.RoundingMode

object ExpenseMath {
    fun rupeesToPaise(rupees: String): Long = BigDecimal(rupees.trim())
        .setScale(2, RoundingMode.HALF_EVEN)
        .movePointRight(2)
        .longValueExact()
        .also { require(it >= 0) }

    fun splitPaise(totalPaise: Long, people: Int): List<Long> {
        require(totalPaise >= 0 && people > 0)
        val amount = BigDecimal.valueOf(totalPaise)
        val roundedShare = amount.divide(BigDecimal.valueOf(people.toLong()), 0, RoundingMode.HALF_EVEN).longValueExact()
        val base = List(people) { roundedShare }
        val delta = amount.subtract(BigDecimal.valueOf(roundedShare).multiply(BigDecimal.valueOf(people.toLong()))).longValueExact()
        if (delta == 0L) return base
        val shares = base.toMutableList()
        val direction = if (delta > 0) 1L else -1L
        repeat(kotlin.math.abs(delta).toInt()) { index -> shares[index % people] += direction }
        check(shares.sum() == totalPaise)
        return shares
    }
}
