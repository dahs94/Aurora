package com.example.aurora

import android.app.Application
import timber.log.Timber

/**the 'Application' class is a class that contains the global application state of the app, and it's
used by the OS to interact with your app

Here we're creating a subclass of Application, and overriding the onCreate function to add a little
bit of our own code (setting up logging) . We're then using super to just call the original function.

Example log: Timber.i("onStart Called")
**/

class AuroraApplication : Application() {

    override fun onCreate() {
        Timber.plant(Timber.DebugTree())
        super.onCreate()
    }
}
