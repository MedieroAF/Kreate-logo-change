package me.knighthat.component.song

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAny
import androidx.media3.common.util.UnstableApi
import it.fast4x.innertube.Innertube
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.R
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.service.modern.PlayerServiceModern
import it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.knighthat.component.dialog.CheckboxDialog
import me.knighthat.utils.Toaster
import org.jetbrains.annotations.Contract
import timber.log.Timber
import java.util.Optional

@UnstableApi
class ResetSongDialog private constructor(
    activeState: MutableState<Boolean>,
    private val binder: PlayerServiceModern.Binder?,
    var song: Optional<Song>
) : CheckboxDialog(activeState), MenuIcon, Descriptive {

    companion object {
        private const val TITLE_CHECKBOX_ID = "title"
        private const val AUTHORS_CHECKBOX_ID = "authors"
        private const val THUMBNAIL_CHECKBOX_ID = "thumbnail"
        private const val PLAYTIME_CHECKBOX_ID = "playtime"
        private const val CACHE_CHECKBOX_ID = "cache"

        @Composable
        operator fun invoke( song: Song ): ResetSongDialog =
            ResetSongDialog(
                remember { mutableStateOf(false) },
                LocalPlayerServiceBinder.current,
                Optional.of( song )
            )
    }

    /*
     * To save memory, these buttons are not init at runtime.
     * They are created upon used, and discard after used.
     */
    init {
        val resetTitle = object : Item() {
            override val id: String = TITLE_CHECKBOX_ID
            override val menuIconTitle: String
                @Composable
                get() = stringResource( R.string.title_reset_title )
        }
        items.add( resetTitle )

        val resetAuthors = object : Item() {
            override val id: String = AUTHORS_CHECKBOX_ID
            override val menuIconTitle: String
                @Composable
                get() = stringResource( R.string.title_reset_authors )
        }
        items.add( resetAuthors )

        val resetThumbnail = object: Item() {
            override val id: String = THUMBNAIL_CHECKBOX_ID
            override val menuIconTitle: String
                @Composable
                get() = stringResource( R.string.reset_thumbnail )
        }
        items.add( resetThumbnail )

        val resetPlaytime = object : Item() {
            override val id: String = PLAYTIME_CHECKBOX_ID
            override val menuIconTitle: String
                @Composable
                get() = stringResource( R.string.title_reset_total_play_time )
        }
        items.add( resetPlaytime )

        val resetCache = object : Item() {
            override val id: String = CACHE_CHECKBOX_ID
            override val menuIconTitle: String
                @Composable
                get() = stringResource( R.string.title_reset_cache )
        }
        items.add( resetCache )

        items.add( Item.SELECT_ALL )
    }

    override val iconId: Int = R.drawable.refresh_circle
    override val messageId: Int = R.string.info_open_reset_dialog
    override val menuIconTitle: String
        @Composable
        get() = stringResource( R.string.settings_reset )
    override val dialogTitle: String
        @Composable
        get() = menuIconTitle

    override fun onShortClick() = showDialog()

    override fun onConfirm() {
        CoroutineScope( Dispatchers.IO ).launch {
            if( song.isEmpty ) return@launch
            var song = this@ResetSongDialog.song.get()

            val fetchIds = arrayOf(TITLE_CHECKBOX_ID, AUTHORS_CHECKBOX_ID, THUMBNAIL_CHECKBOX_ID)
            // [Array.fastAny] is in experimental stage, revert to [Array.any] if needed
            if( items.filter { it.id in fetchIds }.fastAny { it.selected } )
                /*
                 * Reset "title", "authors", and "thumbnail".
                 *
                 * Topology: If any of the related checkboxes is selected,
                 * then fetch the internet for up-to-date information.
                 * If checkbox is ticked, then replace that property with
                 * newly fetched information.
                 */
                Innertube.player( videoId = song.id )
                         .onSuccess { response ->
                             val videoDetails = response.second?.videoDetails ?: return@launch

                             @Contract("_,null->null")
                             fun <T> getProperty( itemId: String, result: T? ): T? =
                                 if (items.first { it.id == itemId }.selected) result else null

                             val title = getProperty( TITLE_CHECKBOX_ID, videoDetails.title )
                             val authors = getProperty( AUTHORS_CHECKBOX_ID, videoDetails.author )
                             val thumbnailUrl = getProperty( THUMBNAIL_CHECKBOX_ID, videoDetails.thumbnail?.thumbnails?.last()?.url )

                             song = song.copy(
                                 title = title ?: song.title,
                                 artistsText = authors ?: song.artistsText,
                                 thumbnailUrl = thumbnailUrl ?: song.thumbnailUrl,
                             )

                             Timber.tag("reset_dialog").d( "Song: $song" )
                         }
                         .onFailure {
                             Timber.tag("reset_dialog").d( it )
                         }

            if( items.first { it.id == PLAYTIME_CHECKBOX_ID }.selected )
                song = song.copy( totalPlayTimeMs = 0L )

            Database.asyncTransaction {
                if( items.first { it.id == CACHE_CHECKBOX_ID }.selected ) {
                    binder?.cache?.removeResource( song.id )
                    binder?.downloadCache?.removeResource( song.id )
                    deleteFormat( song.id )
                    resetContentLength( song.id )
                }

                update( song )

                Toaster.done()
            }
        }

        hideDialog()
    }
}