package com.manish.ridedash

import android.app.Application
import com.manish.ridedash.util.Notifications

class RideDashApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
    }
}
