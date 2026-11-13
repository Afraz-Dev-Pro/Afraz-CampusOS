package com.afraz.campusos.feature_hackhub

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class TeammatePage(val posts: List<TeammatePost>, val lastDocument: DocumentSnapshot?)

class FirebaseNotConfiguredException : IllegalStateException("Cloud community needs the app owner's Firebase configuration")

@Singleton
class TeammateRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val db: FirebaseFirestore by lazy {
        FirebaseBootstrap.ensureConfigured(context)
        FirebaseFirestore.getInstance().also { firestore ->
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
        }
    }

    suspend fun firstCachedPage(): TeammatePage = fetchPage(null, Source.CACHE)
    suspend fun fetchPage(after: DocumentSnapshot?, source: Source = Source.DEFAULT): TeammatePage {
        val now = System.currentTimeMillis()
        var query: Query = db.collection(COLLECTION)
            .whereGreaterThan("expiresAt", now)
            .orderBy("expiresAt", Query.Direction.ASCENDING)
            .limit(PAGE_SIZE.toLong())
        if (after != null) query = query.startAfter(after)
        val snapshot = query.get(source).awaitResult()
        return TeammatePage(snapshot.documents.mapNotNull { document ->
            document.toObject(TeammatePost::class.java)?.copy(id = document.id)
        }, snapshot.documents.lastOrNull())
    }

    suspend fun cleanupExpiredOwnPosts(uid: String) {
        while (true) {
            val expired = db.collection(COLLECTION)
                .whereEqualTo("uid", uid)
                .whereLessThan("expiresAt", System.currentTimeMillis())
                .limit(400)
                .get()
                .awaitResult()
            if (expired.isEmpty) return
            val batch: WriteBatch = db.batch()
            expired.documents.forEach { batch.delete(it.reference) }
            batch.commit().awaitResult()
            if (expired.size() < 400) return
        }
    }

    suspend fun post(uid: String, authorName: String, college: String, pitch: String, skills: List<String>, githubUsername: String, contactUrl: String) {
        require(authorName.length <= 40 && college.length <= 60 && pitch.length <= 150 && skills.size <= 8)
        cleanupExpiredOwnPosts(uid)
        val data = mapOf(
            "uid" to uid,
            "authorName" to authorName,
            "college" to college,
            "pitchBio" to pitch,
            "skills" to skills,
            "githubUsername" to githubUsername,
            "contactUrl" to contactUrl,
            "expiresAt" to System.currentTimeMillis() + POST_LIFETIME_MILLIS
        )
        db.collection(COLLECTION).add(data).awaitResult()
    }

    companion object {
        const val PAGE_SIZE = 10
        const val COLLECTION = "teammate_posts"
        private const val POST_LIFETIME_MILLIS = 14L * 24 * 60 * 60 * 1_000
    }
}

internal suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            continuation.resume(task.result as T)
        } else {
            continuation.resumeWithException(task.exception ?: IllegalStateException("Cloud request failed"))
        }
    }
}
