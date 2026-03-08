package com.aesthetic.funthingwalls

import android.app.Application
import com.google.android.material.color.DynamicColors

class FunthingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // This single line tells the entire app to pull colors from the phone's system!
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}