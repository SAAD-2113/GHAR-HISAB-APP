package com.sindantechnologies.gharhisab

import android.app.Application
import com.sindantechnologies.gharhisab.di.AppContainer
import com.sindantechnologies.gharhisab.di.DefaultAppContainer

/**
 * Application class providing manual dependency injection.
 *
 * The [container] is created once and shared across all ViewModels.
 */
class GharHisabApplication : Application() {

    /** AppContainer instance used by the rest of classes to obtain dependencies */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
