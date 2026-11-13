package com.afraz.campusos.feature_hackhub

import retrofit2.http.GET
import okhttp3.ResponseBody

interface CommunityFeedApi {
    @GET("hackathons.json") suspend fun getHackathons(): ResponseBody
    @GET("pyqs.json") suspend fun getPyqs(): ResponseBody

    companion object { const val BASE_URL = "https://raw.githubusercontent.com/AfrazCampusOS/community-data/main/" }
}
