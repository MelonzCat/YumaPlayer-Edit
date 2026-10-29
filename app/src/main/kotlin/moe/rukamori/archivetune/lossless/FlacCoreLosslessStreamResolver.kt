package moe.rukamori.archivetune.lossless

import moe.rukamori.archivetune.constants.FlacQuality
import moe.rukamori.archivetune.db.entities.Song
import moe.rukamori.archivetune.flaccore.streaming.FlacStreamRegistry
import moe.rukamori.archivetune.flaccore.model.TrackQuery
import moe.rukamori.archivetune.playback.resolvers.LosslessStreamResolver
import moe.rukamori.archivetune.playback.resolvers.StreamUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlacCoreLosslessStreamResolver @Inject constructor(
    private val registry: FlacStreamRegistry
) : LosslessStreamResolver {
    override suspend fun resolve(song: Song, quality: FlacQuality): StreamUrl? {
        val artistName = song.artists.mapNotNull { it.name.takeIf(String::isNotBlank) }.joinToString(", ")
        val query = TrackQuery(
            artist = artistName,
            title = song.title,
            album = song.album?.title,
            isrc = song.song.isrc?.takeIf { it.isNotBlank() },
            durationMs = song.song.duration * 1000L,
            explicit = song.song.explicit
        )
        
        val flacUrl = registry.resolve(query, quality.streamQuality) ?: return null
        
        return StreamUrl(
            url = flacUrl.url,
            expiresAtMs = flacUrl.expiresAtMs,
            codec = flacUrl.codec,
            bitsPerSample = flacUrl.bitsPerSample,
            sampleRateHz = flacUrl.sampleRateHz,
            bitrateKbps = flacUrl.bitrateKbps,
            coverArtUrl = flacUrl.coverArtUrl,
            origin = flacUrl.origin
        )
    }
}
