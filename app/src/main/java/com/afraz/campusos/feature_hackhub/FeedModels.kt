package com.afraz.campusos.feature_hackhub

import kotlinx.serialization.Serializable

@Serializable
data class HackathonItem(
    val id: String,
    val eventName: String,
    val startsAt: String,
    val endsAt: String,
    val registrationDeadline: String,
    val sponsorTracks: List<String> = emptyList(),
    val prizePool: String = "",
    val applyUrl: String
)

@Serializable
data class PyqItem(
    val id: String,
    val subjectName: String,
    val courseCode: String,
    val semester: Int,
    val year: Int,
    val pdfUrl: String
)

@Serializable
data class TeammatePost(
    val id: String = "",
    val uid: String = "",
    val authorName: String = "",
    val college: String = "",
    val pitchBio: String = "",
    val skills: List<String> = emptyList(),
    val githubUsername: String = "",
    val contactUrl: String = "",
    val expiresAt: Long = 0
)
