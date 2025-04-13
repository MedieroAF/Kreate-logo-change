package me.knighthat.component.tab

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import app.kreate.android.R
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.appContext
import it.fast4x.rimusic.models.Playlist
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import it.fast4x.rimusic.utils.formatAsDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.knighthat.component.ImportFromFile
import me.knighthat.utils.DurationUtils
import me.knighthat.utils.Toaster
import me.knighthat.utils.csv.SongCSV
import java.io.InputStream

/**
 * Import songs from custom CSV file.
 *
 * If song is assigned to a playlist,
 * this will handle adding that song
 * to corresponding playlist upon process.
 *
 * This is the template of CSV file:
 *
 * | PlaylistBrowseId | PlaylistName | MediaId | Title | Artists | Duration | ThumbnailUrl |
 * | --- | --- | --- | --- | --- | --- | --- |
 * | 1 | Awesome Playlist | dQw4w9WgXcQ | A song | Anonymous | 212 | https://... |
 * | 2 | Summer | fNFzfwLM72c | Lovely | Anonymous | 250 | https://... |
 * | 3 | New Songs | kPa7bsKwL-c | DWAS | Anonymous | 253 | https://... |
 */
class ImportSongsFromCSV(
    launcher: ManagedActivityResultLauncher<Array<String>, Uri?>
) : ImportFromFile(launcher), MenuIcon, Descriptive {

    companion object {
        private fun parseFromCsvFile( inputStream: InputStream ): List<SongCSV> =
            csvReader().readAllWithHeader(inputStream)
                       .fastMap { row ->      // Experimental, revert back to [map] if needed

                           // Previous version of Kreate uses "PlaylistBrowseId" as playlistId,
                           // this is wrong and must be correct to empty string before inserting
                           // to the database
                           var browseId = row["PlaylistBrowseId"].orEmpty()
                           if( browseId.toLongOrNull() != null )
                               browseId = ""

                           // For backward compatibility, RiMusic exports duration
                           // in human-readable format "00:00" while Kreate exports
                           // in seconds.
                           val rawDuration = row["Duration"].orEmpty()
                           val convertedDuration =
                               if( !DurationUtils.isHumanReadable( rawDuration ) )
                                   formatAsDuration( rawDuration.toLong().times( 1000 ) )
                               else
                                   rawDuration

                           SongCSV(
                               playlistBrowseId = browseId,
                               playlistName = row["PlaylistName"].orEmpty(),
                               songId = row["MediaId"].orEmpty(),
                               title = row["Title"].orEmpty(),
                               artists = row["Artists"].orEmpty(),
                               thumbnailUrl = row["ThumbnailUrl"].orEmpty(),
                               duration = convertedDuration
                           )
                       }
                       .toList()        // Make it immutable

        private fun processSongs( songs: List<SongCSV> ): Map<Pair<String, String>, List<Song>> =
            songs.fastFilter { it.songId.isNotBlank() }
                 .groupBy { it.playlistName to it.playlistBrowseId }
                 .mapValues { (_, songs) ->
                     songs.fastMap {
                         Song(
                             id = it.songId,
                             title = it.title,
                             artistsText = it.artists,
                             thumbnailUrl = it.thumbnailUrl,
                             durationText = it.duration,
                             totalPlayTimeMs = 1L       // Bypass hidden song checker
                         )
                     }
                 }

        @Composable
        operator fun invoke() = ImportSongsFromCSV(
            rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                // [uri] must be non-null (meaning path exists) in order to work
                uri ?: return@rememberLauncherForActivityResult

                // Run in background to prevent UI thread
                // from freezing due to large file.
                CoroutineScope( Dispatchers.IO ).launch {
                    // Songs with no playlist
                    val straySongs = mutableListOf<Song>()
                    val combos = mutableMapOf<Playlist, List<Song>>()

                    appContext().contentResolver
                                .openInputStream( uri )
                                ?.use( ::parseFromCsvFile )       // Use [use] because it closes stream on exit
                                ?.let( ::processSongs )
                                ?.forEach { (playlist, songs) ->
                                    if( playlist.first.isNotBlank() ) {
                                        val realPlaylist = Playlist(name = playlist.first, browseId = playlist.second)
                                        combos[realPlaylist] = songs
                                    } else
                                        straySongs.addAll( songs )
                                }

                    Database.asyncTransaction {
                        songTable.upsert( straySongs )

                        combos.forEach { (playlist, songs) ->
                            songTable.upsert( songs )       // Upsert first to override existing songs
                            mapIgnore( playlist, *songs.toTypedArray() )
                        }

                        // Show message when it's done
                        Toaster.done()
                    }

                }
            }
        )
    }

    override val supportedMimes: Array<String> = arrayOf("text/csv", "text/comma-separated-values")
    override val iconId: Int = R.drawable.resource_import
    override val messageId: Int = R.string.import_playlist
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )
}