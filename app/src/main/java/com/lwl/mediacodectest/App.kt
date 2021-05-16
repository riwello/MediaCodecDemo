package com.lwl.mediacodectest

import android.app.Application
import android.content.Context

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        INSTANCE= applicationContext
    }

    companion object{
        lateinit var INSTANCE:Context
    }
}