package fr.lumiere.tv.ui.components

import fr.lumiere.tv.data.api.ReleaseInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseBadgesTest {
    @Test
    fun ordreEtSansDoublon() = assertEquals(
        listOf("2160p", "HDR10", "HEVC", "DD+ 5.1", "MULTi", "VFF"),
        releaseBadges(
            ReleaseInfo(
                resolution = "2160p",
                hdr = "HDR10",
                videoCodec = "HEVC",
                audioCodec = "DD+ 5.1",
                languages = listOf("MULTi", "VFF", "MULTi"),
            ),
        ),
    )

    @Test
    fun valeursAbsentesIgnorees() = assertEquals(listOf("1080p"), releaseBadges(ReleaseInfo(resolution = "1080p")))
}
