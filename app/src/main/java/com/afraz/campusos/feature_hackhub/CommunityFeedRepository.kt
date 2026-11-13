package com.afraz.campusos.feature_hackhub

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class CommunityFeeds(val hackathons: List<HackathonItem>, val pyqs: List<PyqItem>, val usingSeedData: Boolean)

@Singleton
class CommunityFeedRepository @Inject constructor(
    private val api: CommunityFeedApi,
    private val json: Json,
    @ApplicationContext private val context: Context
) {
    suspend fun load(): CommunityFeeds {
        val seeded = seed()
        val remote = runCatching {
            val events = api.getHackathons().use { json.decodeFromString<List<HackathonItem>>(it.string()) }
            val papers = api.getPyqs().use { json.decodeFromString<List<PyqItem>>(it.string()) }
            if (events.isEmpty() || papers.isEmpty()) null else CommunityFeeds(events, papers, false)
        }.getOrNull()
        return remote ?: seeded
    }

    private fun seed(): CommunityFeeds {
        val events = context.assets.open("seed_hackathons.json").bufferedReader().use { json.decodeFromString<List<HackathonItem>>(it.readText()) }
        val papers = context.assets.open("seed_pyqs.json").bufferedReader().use { json.decodeFromString<List<PyqItem>>(it.readText()) }
        return CommunityFeeds(events, papers, true)
    }
}
