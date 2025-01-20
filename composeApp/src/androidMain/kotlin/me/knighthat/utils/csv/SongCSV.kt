package me.knighthat.utils.csv

import com.github.doyaaaaaken.kotlincsv.client.ICsvFileWriter
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.utils.durationTextToMillis

data class SongCSV(
    val songId: String,
    val playlistId: Long,
    val playlistName: String,
    val title: String,
    val artists: String,
    val duration: String,
    val thumbnailUrl: String
) {
    constructor(
        song: Song,
        playlistId: Long = 0,
        playlistName: String = ""
    ): this(
        playlistId = playlistId,
        playlistName = playlistName,
        songId = song.id,
        title = song.title,
        artists = song.artistsText ?: "",
        duration = durationTextToMillis( song.durationText ?: "" ).div( 1000 ).toString(),
        thumbnailUrl = song.thumbnailUrl ?: ""
    )

    fun write( writer: ICsvFileWriter ) {
        writer.writeRow( playlistId, playlistName, songId, title, artists, duration, thumbnailUrl )
        writer.flush()      // Always flush after write to prevent overlapping
    }
}