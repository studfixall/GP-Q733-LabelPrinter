package com.gp.q733

import android.app.Application
import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Q733App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
    }
}