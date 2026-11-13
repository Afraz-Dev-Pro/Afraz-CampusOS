package com.afraz.campusos.di

import android.content.Context
import com.afraz.campusos.feature_hackhub.CommunityFeedApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Provides @Singleton
    fun httpClient(@ApplicationContext context: Context): OkHttpClient {
        val cache = Cache(context.cacheDir.resolve("hackhub-http"), 10L * 1024L * 1024L)
        return OkHttpClient.Builder()
            .cache(cache)
            .addNetworkInterceptor { chain ->
                chain.proceed(chain.request()).newBuilder().header("Cache-Control", "public, max-age=21600").build()
            }
            .build()
    }

    @Provides @Singleton
    fun feedApi(client: OkHttpClient): CommunityFeedApi = Retrofit.Builder()
        .baseUrl(CommunityFeedApi.BASE_URL)
        .client(client)
        .build()
        .create(CommunityFeedApi::class.java)
}
