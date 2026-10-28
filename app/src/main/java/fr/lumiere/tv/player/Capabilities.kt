package fr.lumiere.tv.player

import android.content.Context
import android.media.MediaCodecList
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioCapabilities
import fr.lumiere.tv.data.api.ClientCapabilities

// Types MIME Android vers noms de codecs de Lumière (apps/web/src/lib/capabilities.ts), dans l'ordre déclaré.
private val VIDEO = listOf(
    "video/avc" to "h264",
    "video/hevc" to "hevc",
    "video/av01" to "av1",
    "video/x-vnd.on2.vp9" to "vp9",
)
private val AUDIO = listOf(
    "audio/mp4a-latm" to "aac",
    "audio/mpeg" to "mp3",
    "audio/opus" to "opus",
    "audio/flac" to "flac",
    "audio/ac3" to "ac3",
    "audio/eac3" to "eac3",
)

/** ExoPlayer lit ces conteneurs lui-même, quel que soit l'appareil. */
private val CONTAINERS = listOf("mp4", "mkv", "webm")

/**
 * Capacités déclarées au serveur : décodeurs présents, plus l'audio Dolby que l'appareil
 * sait envoyer tel quel à l'ampli ou à la TV (passthrough).
 */
fun buildCapabilities(decoderMimeTypes: Set<String>, passthroughAudio: Set<String>): ClientCapabilities =
    ClientCapabilities(
        videoCodecs = VIDEO.filter { it.first in decoderMimeTypes }.map { it.second },
        audioCodecs = AUDIO.filter { it.first in decoderMimeTypes || it.first in passthroughAudio }.map { it.second },
        containers = CONTAINERS,
    )

private var cached: ClientCapabilities? = null

/** Capacités réelles de l'appareil, calculées une fois. */
@OptIn(UnstableApi::class)
fun detectCapabilities(context: Context): ClientCapabilities {
    cached?.let { return it }
    val decoders = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        .filter { !it.isEncoder }
        .flatMap { it.supportedTypes.map(String::lowercase) }
        .toSet()
    val audio = AudioCapabilities.getCapabilities(context, AudioAttributes.DEFAULT, null)
    val passthrough = buildSet {
        if (audio.supportsEncoding(C.ENCODING_AC3)) add("audio/ac3")
        if (audio.supportsEncoding(C.ENCODING_E_AC3)) add("audio/eac3")
    }
    return buildCapabilities(decoders, passthrough).also {
        cached = it
        Log.i("Lumiere", "Capacités : vidéo ${it.videoCodecs}, audio ${it.audioCodecs}")
    }
}
