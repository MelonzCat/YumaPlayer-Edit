package moe.rukamori.archivetune.download

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.playback.resolvers.LosslessStreamResolver

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FlacDownloaderEntryPoint {
    fun losslessStreamResolver(): LosslessStreamResolver

    fun database(): MusicDatabase
}
