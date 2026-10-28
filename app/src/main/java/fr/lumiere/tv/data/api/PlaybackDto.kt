package fr.lumiere.tv.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Copie des contrats de Lumiere/packages/shared/src/playback.ts.

/** Référence vers un média lisible ; le discriminant JSON est `type`. */
@Serializable
sealed interface MediaRef {
    @Serializable
    @SerialName("remote")
    data class Remote(val path: String) : MediaRef

    @Serializable
    @SerialName("library")
    data class Library(val id: Int) : MediaRef

    /** Épisode dont le fichier est choisi par le serveur au moment de la lecture. */
    @Serializable
    @SerialName("episode")
    data class Episode(val titleId: Int, val season: Int, val episode: Int) : MediaRef
}

@Serializable
data class ClientCapabilities(
    val videoCodecs: List<String>,
    val audioCodecs: List<String>,
    val containers: List<String>,
)

@Serializable
data class PlaybackRequest(
    val media: MediaRef,
    val capabilities: ClientCapabilities,
    val audioTrack: Int? = null,
    val mode: String = "auto",
)

@Serializable
data class AudioTrackInfo(
    val index: Int,
    val label: String,
    val language: String? = null,
    val codec: String,
    val channels: Int? = null,
    val isDefault: Boolean = false,
)

@Serializable
data class SubtitleTrackInfo(
    val id: String,
    val label: String,
    val language: String? = null,
    val codec: String,
    val source: String,
    val supported: Boolean = true,
    val isDefault: Boolean = false,
    val isForced: Boolean = false,
    val provider: String? = null,
    val hashMatch: Boolean? = null,
    val hearingImpaired: Boolean? = null,
    val release: String? = null,
    val downloads: Int? = null,
)

@Serializable
data class SubtitleSync(
    val offset: Double,
    val rate: Double,
    val source: String,
    val note: String? = null,
)

@Serializable
data class SubtitleSyncState(val sync: SubtitleSync? = null, val autoFailed: Boolean = false)

@Serializable
data class SubtitleSyncResult(val sync: SubtitleSync? = null, val message: String)

@Serializable
data class SavedSubtitleSync(val sync: SubtitleSync)

@Serializable
data class OnlineSubtitlesResult(
    val tracks: List<SubtitleTrackInfo> = emptyList(),
    val providers: List<String> = emptyList(),
    val notices: List<String> = emptyList(),
)

@Serializable
data class VideoInfo(
    val codec: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val hdr: Boolean = false,
)

@Serializable
data class NextMedia(val media: MediaRef, val label: String)

@Serializable
data class OnlineSubtitlesInfo(val available: Boolean = false, val autoLanguage: String? = null)

@Serializable
data class PlaybackSession(
    val sessionId: String,
    val mode: String,
    val streamUrl: String,
    val mimeType: String,
    val seek: String,
    val duration: Double? = null,
    val resumePosition: Double = 0.0,
    val mediaKey: String,
    val title: String,
    val subtitle: String? = null,
    val titleId: Int? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val audioTracks: List<AudioTrackInfo> = emptyList(),
    val selectedAudioTrack: Int? = null,
    val subtitleTracks: List<SubtitleTrackInfo> = emptyList(),
    val defaultSubtitleTrack: String? = null,
    val video: VideoInfo? = null,
    val reasons: List<String> = emptyList(),
    val next: NextMedia? = null,
    val onlineSubtitles: OnlineSubtitlesInfo = OnlineSubtitlesInfo(),
)

@Serializable
data class ProgressUpdate(val position: Double, val duration: Double?)

@Serializable
data class ContinueWatchingItem(
    val mediaKey: String,
    val media: MediaRef,
    val title: String,
    val subtitle: String? = null,
    val titleId: Int? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val position: Double,
    val duration: Double? = null,
    val updatedAt: String? = null,
)

@Serializable
data class StreamOrigin(val start: Double)
