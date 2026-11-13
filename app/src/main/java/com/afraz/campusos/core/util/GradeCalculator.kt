package com.afraz.campusos.core.util

data class CourseGrade(val credits: Int, val grade: String)

object GradeCalculator {
    val defaultScale = mapOf("O" to 10.0, "E" to 9.0, "A" to 8.0, "B" to 7.0, "C" to 6.0, "D" to 5.0, "F" to 0.0)

    fun parseScale(custom: String): Map<String, Double>? {
        if (custom.isBlank()) return defaultScale
        val values = runCatching {
            custom.split(',').associate { item ->
                val parts = item.trim().split('=')
                require(parts.size == 2)
                val grade = parts[0].trim().uppercase()
                val point = parts[1].trim().toDouble()
                require(grade.isNotBlank() && point in 0.0..10.0)
                grade to point
            }
        }.getOrNull() ?: return null
        return values.takeIf { it.isNotEmpty() }
    }

    fun sgpa(courses: List<CourseGrade>, scale: Map<String, Double> = defaultScale): Double? {
        val valid = courses.filter { it.credits > 0 && it.grade.isNotBlank() }
        if (valid.isEmpty()) return null
        val points = valid.map { course ->
            val point = scale[course.grade.trim().uppercase()] ?: return null
            course.credits * point
        }
        val totalCredits = valid.sumOf { it.credits }
        return points.sum().toDouble() / totalCredits
    }
}
