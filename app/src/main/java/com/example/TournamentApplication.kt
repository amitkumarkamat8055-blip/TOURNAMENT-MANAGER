package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class TournamentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:348190154491:android:4e89bf9095a85d73d05a5e")
                    .setApiKey("AIzaSyB-3-OkMG4nIeOSpvXWMqvsvVErJkojAYo")
                    .setProjectId("tournament-app-e3160")
                    .setStorageBucket("tournament-app-e3160.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("TournamentApplication", "FirebaseApp initialized explicitly")
            }
        } catch (e: Exception) {
            Log.e("TournamentApplication", "Firebase initialization error: ${e.message}", e)
        }
    }
}
