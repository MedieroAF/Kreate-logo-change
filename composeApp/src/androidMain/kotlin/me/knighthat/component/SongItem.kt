package me.knighthat.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import coil.compose.AsyncImage
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.EXPLICIT_PREFIX
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.R
import it.fast4x.rimusic.cleanPrefix
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.DownloadedStateMedia
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.service.isLocal
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.tab.toolbar.Clickable
import it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import it.fast4x.rimusic.ui.components.tab.toolbar.Icon
import it.fast4x.rimusic.ui.components.themed.HeaderIconButton
import it.fast4x.rimusic.ui.components.themed.IconButton
import it.fast4x.rimusic.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.favoritesIcon
import it.fast4x.rimusic.ui.styling.favoritesOverlay
import it.fast4x.rimusic.ui.styling.px
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.conditional
import it.fast4x.rimusic.utils.disableScrollingTextKey
import it.fast4x.rimusic.utils.downloadedStateMedia
import it.fast4x.rimusic.utils.getDownloadState
import it.fast4x.rimusic.utils.getLikedIcon
import it.fast4x.rimusic.utils.isNowPlaying
import it.fast4x.rimusic.utils.manageDownload
import it.fast4x.rimusic.utils.medium
import it.fast4x.rimusic.utils.playlistindicatorKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.secondary
import it.fast4x.rimusic.utils.semiBold
import it.fast4x.rimusic.utils.thumbnail
import kotlinx.coroutines.Dispatchers
import me.knighthat.component.tab.ItemSelector

private interface SongIndicator: Icon {
    override val sizeDp: Dp
        get() = 18.dp
    override val color: Color
        @Composable
        get() = colorPalette().accent

    override fun onShortClick() { /* Does nothing */ }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun ToolBarButton() {
        val modifier = this.modifier
                                     .size(sizeDp)
                                     .combinedClickable(
                                         onClick = ::onShortClick,
                                         onLongClick = {
                                             if(this is Clickable)
                                                 this.onLongClick()
                                         }
                                     )

        IconButton(
            icon = iconId,
            color = color,
            enabled = isEnabled,
            onClick = {},
            modifier = modifier
        )

        Spacer( Modifier.padding(horizontal = 3.dp) )
    }
}

@Composable
fun SongText(
    text: String,
    style: TextStyle,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    modifier: Modifier = Modifier
) = BasicText(
    text = text,
    style = style,
    maxLines = 1,
    overflow = overflow,
    modifier = modifier
)

/**
 *  Displays information of a song.
 *
 *  @param song record from database
 *  @param navController optional field to detect whether the
 *  current location is playlist to hide playlist indicator.
 *  @param isRecommended whether this song is selected by algorithm
 *  @param modifier applied to the outermost layer but not its content
 *  @param showThumbnail whether to fetch/show thumbnail of this song.
 *  [thumbnailOverlay] will still being shown regardless the state of this value
 *  @param trailingContent content being placed to the rightmost of the card
 */
@UnstableApi
@ExperimentalFoundationApi
@Composable
fun SongItem(
    song: Song,
    itemSelector: ItemSelector<Song>? = null,
    navController: NavController? = null,
    isRecommended: Boolean = false,
    modifier: Modifier = Modifier,
    showThumbnail: Boolean = true,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
    thumbnailOverlay: @Composable BoxScope.() -> Unit = {},
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    // Essentials
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val disableScrollingText by rememberPreference( disableScrollingTextKey, false )
    val hapticFeedback = LocalHapticFeedback.current

    val colorPalette = colorPalette()
    val isPlaying = binder?.player?.isNowPlaying( song.id ) ?: false

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy( 12.dp ),
        modifier = modifier.clip( RoundedCornerShape(10.dp) )
                           .fillMaxWidth()
                           .conditional( isPlaying ) {
                               background( colorPalette.favoritesOverlay )
                           }
                           // This component must be placed before padding to prevent
                           // ripple effect to only highlight padded area
                           .combinedClickable(
                               onClick = onClick,
                               onLongClick = {
                                   hapticFeedback.performHapticFeedback( HapticFeedbackType.LongPress )
                                   onLongClick()
                               }
                           )
                           .padding(
                               vertical = Dimensions.itemsVerticalPadding,
                               horizontal = 16.dp
                           )
    ) {
        // Song's thumbnail
        Box(
            Modifier.size( Dimensions.thumbnails.song )
        ) {
            /*
                Thumbnail size
                It always fetches a fixed size to prevent cache
                from storing multiple images.
                Let the OS do the resizing for more efficient outcome.
                TODO: Make a simple system to detect network speed and/or
                TODO: data saver that automatically lower the quality to
                TODO: reduce loading time and to preserve data usage.
             */
            val thumbnailSizePx = Dimensions.thumbnails.song.px

            // Actual thumbnail (from cache or fetch from url)
            if( showThumbnail )
                AsyncImage(
                    model = song.thumbnailUrl
                                ?.thumbnail( thumbnailSizePx ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                                       .clip( thumbnailShape() )
                )

            /*
                To avoid the app from rendering components
                on top of each other without showing the
                previous, is condition is here to force it
                to display either overlay.
             */
            if( isPlaying )
                NowPlayingSongIndicator(
                    mediaId = song.id,
                    player = binder?.player
                )
            else
                thumbnailOverlay()

            if( song.likedAt != null )
                HeaderIconButton(
                    onClick = {},
                    icon = getLikedIcon(),
                    color = colorPalette().favoritesIcon,
                    iconSize = 12.dp,
                    modifier = Modifier.align( Alignment.BottomStart )
                                       .absoluteOffset( x = (-8).dp )
                )
        }

        // Song's information
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy( 4.dp ),
            modifier = Modifier.weight( 1f )
        ) {
            Row( verticalAlignment = Alignment.CenterVertically ) {
                // Show icon if song is recommended by the algorithm
                if( isRecommended )
                    object: SongIndicator {
                        override val iconId: Int = R.drawable.smart_shuffle
                    }.ToolBarButton()

                val showInPlaylistIndicator by rememberPreference( playlistindicatorKey,false )
                val isInPlaylistScreen = navController != null && NavRoutes.localPlaylist.isHere( navController )
                // Show icon if song belongs to a playlist,
                // except when in playlist.
                if( showInPlaylistIndicator && !isInPlaylistScreen ) {

                    val isExistedInAPlaylist by remember( showInPlaylistIndicator ) {
                        Database.isSongMappedToPlaylist( song.id )
                    }.collectAsState( initial = false, context = Dispatchers.IO )

                    if( isExistedInAPlaylist )
                        object: SongIndicator, Descriptive {
                            override val iconId: Int = R.drawable.add_in_playlist
                            override val messageId: Int = R.string.playlistindicatorinfo2
                            override val sizeDp: Dp = 10.dp
                            override val modifier: Modifier =
                                Modifier.background(colorPalette().accent, CircleShape)
                                        .padding(all = 3.dp)
                            override val color: Color
                                @Composable
                                get() = colorPalette.text

                            override fun onShortClick() = super.onShortClick()
                        }.ToolBarButton()
                }

                if( song.title.startsWith(EXPLICIT_PREFIX) )
                    object: SongIndicator {
                        override val iconId: Int = R.drawable.explicit
                    }.ToolBarButton()

                // Song's name
                SongText(
                    text = cleanPrefix( song.title ),
                    style = typography().xs.semiBold,
                    modifier = Modifier.weight( 1f )
                                       .conditional( !disableScrollingText ) {
                                           basicMarquee( iterations = Int.MAX_VALUE )
                                       }
                )
            }

            Row( verticalAlignment = Alignment.CenterVertically ) {
                // Song's author
                SongText(
                    text = song.artistsText.toString(),
                    style = typography().xs.semiBold.secondary,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight( 1f )
                                       .conditional( !disableScrollingText ) {
                                           basicMarquee( iterations = Int.MAX_VALUE )
                                       }
                )

                /*
                    Song's duration
                    If it's "null", show --:-- instead of leaving it empty
                 */
                SongText(
                    text = song.durationText ?: "--:--",
                    style = typography().xxs.secondary.medium,
                    modifier = Modifier.padding( top = 4.dp, start = 5.dp  )
                )

                Spacer( Modifier.padding(horizontal = 4.dp) )

                // Show download icon when song is NOT local
                if( !song.isLocal ) {
                    // Temporal cache, acquire by listening to the song
                    var cacheState by remember {
                        mutableStateOf(DownloadedStateMedia.NOT_CACHED_OR_DOWNLOADED)
                    }
                    cacheState = downloadedStateMedia( song.id )
                    // "Permanent" cache, requires explicit interaction to start downloading
                    var downloadState by remember {
                        mutableIntStateOf( Download.STATE_STOPPED )
                    }
                    downloadState = getDownloadState( song.id )

                    val icon =when( downloadState ) {
                        Download.STATE_DOWNLOADING,
                        Download.STATE_REMOVING     -> R.drawable.download_progress

                        else                        -> cacheState.iconId
                    }
                    val color = when( cacheState ) {
                        DownloadedStateMedia.NOT_CACHED_OR_DOWNLOADED -> colorPalette().textDisabled

                        else                                          -> colorPalette().text
                    }
                    val isDownloaded = when( cacheState ) {
                        DownloadedStateMedia.CACHED_AND_DOWNLOADED,
                        DownloadedStateMedia.DOWNLOADED             -> true

                        else                                        -> false
                    }

                    IconButton(
                        icon = icon,
                        color = color,
                        modifier = Modifier.size( 20.dp ),
                        onClick = {
                            val mediaItem = song.asMediaItem

                            // TODO: Confirmation dialog upon delete
                            binder?.cache?.removeResource( mediaItem.mediaId )
                            Database.asyncTransaction {
                                deleteFormat( mediaItem.mediaId )
                            }

                            manageDownload(
                                context = context,
                                mediaItem = mediaItem,
                                downloadState = isDownloaded
                            )
                        }
                    )
                }
            }
        }

        if( itemSelector != null ) {
            // It must watch for [selectedItems.size] for changes
            // Otherwise, state will stay the same
            val checkedState = remember( itemSelector.size ) {
                mutableStateOf( song in itemSelector )
            }

            if( itemSelector.isActive )
                Checkbox(
                    checked = checkedState.value,
                    onCheckedChange = {
                        checkedState.value = it
                        if ( it )
                            itemSelector.add( song )
                        else
                            itemSelector.remove( song )
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = colorPalette().accent,
                        uncheckedColor = colorPalette().text
                    ),
                    modifier = Modifier.scale( 0.7f )
                )
        }

        trailingContent?.invoke( this )
    }
}