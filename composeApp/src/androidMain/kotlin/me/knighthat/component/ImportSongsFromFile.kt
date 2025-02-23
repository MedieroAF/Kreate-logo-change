package me.knighthat.component

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import it.fast4x.rimusic.R
import me.knighthat.utils.Toaster

abstract class ImportSongsFromFile(
    private val launcher: ManagedActivityResultLauncher<Array<String>, Uri?>
) {
    abstract val supportedMimes: Array<String>

    fun onShortClick() {
        try {
            launcher.launch( supportedMimes )
        } catch ( _: ActivityNotFoundException ) {
            Toaster.e( R.string.info_not_find_app_open_doc )
        }
    }
}