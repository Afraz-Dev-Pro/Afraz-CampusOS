package com.afraz.campusos.feature_hackhub

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afraz.campusos.data.JsonBackupManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HackHubState(
    val hackathons: List<HackathonItem> = emptyList(),
    val pyqs: List<PyqItem> = emptyList(),
    val usingSeedData: Boolean = true,
    val feedQuery: String = "",
    val teammatePosts: List<TeammatePost> = emptyList(),
    val selectedSkill: String? = null,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val showAuthSheet: Boolean = false,
    val showPostForm: Boolean = false,
    val error: String? = null
)

sealed interface HackHubEvent { data object ExportBackup : HackHubEvent }

@HiltViewModel
class HackHubViewModel @Inject constructor(
    private val feedRepository: CommunityFeedRepository,
    private val teammateRepository: TeammateRepository,
    private val authManager: LazyAuthManager,
    private val backupManager: JsonBackupManager
) : ViewModel() {
    private val mutableState = MutableStateFlow(HackHubState())
    val state: StateFlow<HackHubState> = mutableState.stateIn(viewModelScope, SharingStarted.Eagerly, HackHubState())
    private val eventChannel = Channel<HackHubEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var nextDocument: DocumentSnapshot? = null
    private var hasMore = true
    private var requestedAction: RequestedAction? = null

    init {
        refreshFeeds()
        refreshTeammates()
    }

    fun refreshFeeds() = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, error = null) }
        val feeds = runCatching { feedRepository.load() }.getOrElse { error ->
            mutableState.update { it.copy(loading = false, error = error.message ?: "Community feeds are unavailable") }
            return@launch
        }
        mutableState.update { it.copy(hackathons = feeds.hackathons, pyqs = feeds.pyqs, usingSeedData = feeds.usingSeedData, loading = false) }
    }

    fun selectSkill(skill: String?) { mutableState.update { it.copy(selectedSkill = skill) } }
    fun setFeedQuery(value: String) { mutableState.update { it.copy(feedQuery = value) } }

    fun refreshTeammates() = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, error = null) }
        try {
            authManager.currentUserOrNull()?.let { teammateRepository.cleanupExpiredOwnPosts(it.uid) }
            val cached = runCatching { teammateRepository.firstCachedPage() }.getOrNull()
            if (cached != null) {
                nextDocument = cached.lastDocument
                hasMore = cached.posts.size == TeammateRepository.PAGE_SIZE
                mutableState.update { it.copy(teammatePosts = cached.posts, loading = false) }
            }
            val page = teammateRepository.fetchPage(null, Source.DEFAULT)
            nextDocument = page.lastDocument
            hasMore = page.posts.size == TeammateRepository.PAGE_SIZE
            mutableState.update { it.copy(teammatePosts = page.posts, loading = false) }
        } catch (error: Exception) {
            mutableState.update { it.copy(loading = false, error = error.message ?: "Teammate posts are not available") }
        }
    }

    fun loadMore() = viewModelScope.launch {
        if (!hasMore || mutableState.value.loadingMore) return@launch
        mutableState.update { it.copy(loadingMore = true) }
        try {
            val page = teammateRepository.fetchPage(nextDocument, Source.DEFAULT)
            nextDocument = page.lastDocument
            hasMore = page.posts.size == TeammateRepository.PAGE_SIZE
            mutableState.update { it.copy(teammatePosts = (it.teammatePosts + page.posts).distinctBy(TeammatePost::id), loadingMore = false) }
        } catch (error: Exception) {
            mutableState.update { it.copy(loadingMore = false, error = error.message ?: "Unable to load more teammate posts") }
        }
    }

    fun requestPost() = request(RequestedAction.Post)
    fun requestBackup() = request(RequestedAction.Backup)

    private fun request(action: RequestedAction) {
        requestedAction = action
        if (authManager.currentUserOrNull() != null) handleAuthorizedAction(action)
        else mutableState.update { it.copy(showAuthSheet = true, error = null) }
    }

    fun dismissAuth() { mutableState.update { it.copy(showAuthSheet = false) }; requestedAction = null }

    fun signIn(activity: Activity) = viewModelScope.launch {
        mutableState.update { it.copy(error = null) }
        try {
            authManager.signIn(activity)
            mutableState.update { it.copy(showAuthSheet = false) }
            requestedAction?.let(::handleAuthorizedAction)
        } catch (error: Exception) {
            mutableState.update { it.copy(error = error.message ?: "Sign-in was not completed") }
        }
    }

    private fun handleAuthorizedAction(action: RequestedAction) {
        requestedAction = null
        when (action) {
            RequestedAction.Post -> mutableState.update { it.copy(showPostForm = true) }
            RequestedAction.Backup -> viewModelScope.launch { eventChannel.send(HackHubEvent.ExportBackup) }
        }
    }

    fun dismissPostForm() { mutableState.update { it.copy(showPostForm = false) } }

    fun submitPost(authorName: String, college: String, pitch: String, skills: List<String>, githubUsername: String, contactUrl: String) = viewModelScope.launch {
        val user = authManager.currentUserOrNull() ?: run {
            mutableState.update { it.copy(error = "Sign in before posting") }
            return@launch
        }
        try {
            teammateRepository.post(user.uid, authorName, college, pitch, skills, githubUsername, contactUrl)
            mutableState.update { it.copy(showPostForm = false) }
            refreshTeammates()
        } catch (error: Exception) {
            mutableState.update { it.copy(error = error.message ?: "Unable to publish your request") }
        }
    }

    fun exportBackup(uri: android.net.Uri) = viewModelScope.launch {
        runCatching { backupManager.writeToUri(uri, compressed = false) }
            .onFailure { mutableState.update { state -> state.copy(error = it.message ?: "Backup export failed") } }
    }

    fun restoreBackup(uri: android.net.Uri) = viewModelScope.launch {
        runCatching { backupManager.restoreFromUri(uri) }
            .onFailure { mutableState.update { state -> state.copy(error = it.message ?: "Backup restore failed") } }
    }

    private enum class RequestedAction { Post, Backup }
}
