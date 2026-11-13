package com.afraz.campusos.feature_hackhub

import android.content.Context
import com.afraz.campusos.R
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseBootstrap {
    fun ensureConfigured(context: Context): FirebaseApp {
        FirebaseApp.getApps(context).firstOrNull()?.let { return it }
        val apiKey = context.getString(R.string.firebase_api_key)
        val appId = context.getString(R.string.firebase_app_id)
        val projectId = context.getString(R.string.firebase_project_id)
        val senderId = context.getString(R.string.firebase_sender_id)
        require(!apiKey.startsWith("CONFIGURE_") && !appId.startsWith("CONFIGURE_") && !projectId.startsWith("CONFIGURE_")) {
            "Configure Firebase values in app/src/main/res/values/strings.xml before using cloud features"
        }
        val options = FirebaseOptions.Builder()
            .setApiKey(apiKey)
            .setApplicationId(appId)
            .setProjectId(projectId)
            .also { if (!senderId.startsWith("CONFIGURE_")) it.setGcmSenderId(senderId) }
            .build()
        return FirebaseApp.initializeApp(context, options) ?: FirebaseApp.getInstance()
    }
}
