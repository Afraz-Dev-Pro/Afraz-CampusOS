package com.afraz.campusos.feature_hackhub

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.afraz.campusos.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class LazyAuthManager @Inject constructor(@ApplicationContext private val context: Context) {
    fun currentUserOrNull(): FirebaseUser? = if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseAuth.getInstance().currentUser

    suspend fun signIn(activity: Activity): FirebaseUser {
        FirebaseBootstrap.ensureConfigured(activity)
        val serverClientId = activity.getString(R.string.google_web_client_id)
        require(serverClientId.endsWith(".apps.googleusercontent.com")) { "Configure the Google web client ID in app/src/main/res/values/strings.xml" }
        val googleOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build()
        val response = CredentialManager.create(activity).getCredential(activity, request)
        val credential = response.credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Google sign-in did not return an ID token"
        }
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(token, null)
        return withContext(Dispatchers.IO) {
            FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).awaitResult().user
                ?: error("Firebase sign-in did not return a user")
        }
    }
}
