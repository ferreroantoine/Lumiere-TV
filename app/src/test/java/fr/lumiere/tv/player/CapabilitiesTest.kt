package fr.lumiere.tv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class CapabilitiesTest {
    @Test
    fun decodeursMateriels() {
        val caps = buildCapabilities(setOf("video/avc", "video/hevc", "audio/mp4a-latm"), emptySet())
        assertEquals(listOf("h264", "hevc"), caps.videoCodecs)
        assertEquals(listOf("aac"), caps.audioCodecs)
    }

    @Test
    fun passthroughAjouteLAudioDolby() {
        val caps = buildCapabilities(setOf("video/avc", "audio/mp4a-latm"), setOf("audio/eac3"))
        assertEquals(listOf("aac", "eac3"), caps.audioCodecs)
    }

    @Test
    fun typeInconnuIgnore() {
        val caps = buildCapabilities(setOf("video/mp4v-es", "video/avc"), emptySet())
        assertEquals(listOf("h264"), caps.videoCodecs)
    }

    @Test
    fun conteneursToujoursDeclares() {
        assertEquals(listOf("mp4", "mkv", "webm"), buildCapabilities(emptySet(), emptySet()).containers)
    }

    @Test
    fun ordreStable() {
        val caps = buildCapabilities(
            setOf("audio/eac3", "audio/ac3", "audio/flac", "audio/opus", "audio/mpeg", "audio/mp4a-latm", "video/x-vnd.on2.vp9", "video/av01", "video/hevc", "video/avc"),
            emptySet(),
        )
        assertEquals(listOf("h264", "hevc", "av1", "vp9"), caps.videoCodecs)
        assertEquals(listOf("aac", "mp3", "opus", "flac", "ac3", "eac3"), caps.audioCodecs)
    }
}
