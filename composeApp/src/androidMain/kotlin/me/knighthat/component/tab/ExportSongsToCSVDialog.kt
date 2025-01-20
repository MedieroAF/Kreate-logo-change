package me.knighthat.component.tab

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import it.fast4x.rimusic.R
import it.fast4x.rimusic.appContext
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import me.knighthat.component.ExportToFileDialog
import me.knighthat.utils.csv.SongCSV

/**
 * Create a custom CSV file with replicable information of
 * provided songs. Can be used to share between users.
 *
 * This is the template of CSV file:
 *
 * | PlaylistBrowseId | PlaylistName | MediaId | Title | Artists | Duration | ThumbnailUrl |
 * | --- | --- | --- | --- | --- | --- | --- |
 * | 1 | Awesome Playlist | dQw4w9WgXcQ | A song | Anonymous | 212 | https://... |
 * | 2 | Summer | fNFzfwLM72c | Lovely | Anonymous | 250 | https://... |
 * | 3 | New Songs | kPa7bsKwL-c | DWAS | Anonymous | 253 | https://... |
 */
class ExportSongsToCSVDialog private constructor(
    valueState: MutableState<String>,
    activeState: MutableState<Boolean>,
    launcher: ManagedActivityResultLauncher<String, Uri?>
): ExportToFileDialog(valueState, activeState, launcher), MenuIcon, Descriptive {

    companion object {
        @Composable
        operator fun invoke(
            playlistId: Long,
            playlistName: String,
            songs: () -> List<Song>
        ) = ExportSongsToCSVDialog(
            // [playlistName] is an mutable object. Therefore,
            // if it was changed externally, this "remembered"
            // state must be updated as well.
            remember( playlistName ) { mutableStateOf(playlistName) },
            rememberSaveable { mutableStateOf(false) },
            rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument( "text/csv" )
            ) { uri ->
                // [uri] must be non-null (meaning path exists) in order to work
                uri ?: return@rememberLauncherForActivityResult

                appContext().contentResolver
                            .openOutputStream( uri )
                            ?.use { outStream ->         // Use [use] because it closes stream on exit
                                csvWriter().open( outStream ) {
                                    writeRow(       // Write down needed sections
                                        "PlaylistBrowseId",
                                        "PlaylistName",
                                        "MediaId",
                                        "Title",
                                        "Artists",
                                        "Duration",
                                        "ThumbnailUrl"
                                    )
                                    flush()

                                    songs().map {
                                                SongCSV(
                                                    playlistId = playlistId,
                                                    playlistName = playlistName,
                                                    song = it
                                                )
                                           }
                                           .forEach { it.write( this ) }

                                    close()
                                }
                            }
            }
        )
    }

    override val extension: String = ".csv"
    override val iconId: Int = R.drawable.export
    override val messageId: Int = R.string.export_playlist
    override val dialogTitle: String
        @Composable
        get() = stringResource( R.string.enter_the_playlist_name )
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    // Both [IDialog] and [Descriptive] require this function,
    // so it must be explicitly stated here to not confuse the compiler
    override fun onShortClick() = super.onShortClick()
}