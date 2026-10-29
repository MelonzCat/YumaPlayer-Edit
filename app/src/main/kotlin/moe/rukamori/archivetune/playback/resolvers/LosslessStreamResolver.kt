package moe.rukamori.archivetune.playback.resolvers

import moe.rukamori.archivetune.constants.FlacQuality
import moe.rukamori.archivetune.db.entities.Song

data class StreamUrl(
    val url: String,
    val expiresAtMs: Long,
    val codec: String? = null,
    val bitsPerSample: Int? = null,
    val sampleRateHz: Int? = null,
    val bitrateKbps: Int? = null,
    val coverArtUrl: String? = null,
    val origin: String
)

interface LosslessStreamResolver {
    suspend fun resolve(song: Song, quality: FlacQuality): StreamUrl?
}
