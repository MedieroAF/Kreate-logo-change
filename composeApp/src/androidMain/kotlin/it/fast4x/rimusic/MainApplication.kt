package it.fast4x.rimusic

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import it.fast4x.rimusic.utils.CaptureCrash
import it.fast4x.rimusic.utils.FileLoggingTree
import it.fast4x.rimusic.utils.logDebugEnabledKey
import it.fast4x.rimusic.utils.preferences
import me.knighthat.coil.ImageCacheFactory
import timber.log.Timber
import java.io.File

class MainApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        //DatabaseInitializer()
        Dependencies.init(this)

        /**** LOG *********/
        val logEnabled = preferences.getBoolean(logDebugEnabledKey, false)
        if (logEnabled) {
            val dir = filesDir.resolve("logs").also {
                if (it.exists()) return@also
                it.mkdir()
            }

            Thread.setDefaultUncaughtExceptionHandler(CaptureCrash(dir.absolutePath))

            Timber.plant(FileLoggingTree(File(dir, "RiMusic_log.txt")))
            Timber.d("Log enabled at ${dir.absolutePath}")
        } else {
            Timber.uprootAll()
            Timber.plant(Timber.DebugTree())
        }
        /**** LOG *********/
    }

    override fun newImageLoader(): ImageLoader = ImageCacheFactory.LOADER

}

object Dependencies {
    lateinit var application: MainApplication
        private set

    internal fun init(application: MainApplication) {
        this.application = application
        DatabaseInitializer()
    }
}