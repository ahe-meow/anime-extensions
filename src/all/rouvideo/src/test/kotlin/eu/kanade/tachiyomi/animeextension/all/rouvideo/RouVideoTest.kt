package eu.kanade.tachiyomi.animeextension.all.rouvideo

import java.util.Base64 as JavaBase64
import aniyomi.lib.m3u8server.M3u8Integration
import eu.kanade.tachiyomi.animesource.model.Video
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouVideoTest {
    @Test
    fun `decodes offset base64 video data`() {
        val original = """{"videoUrl":"https://example.com/video.m3u8"}"""
        val k = 13
        val encoded = JavaBase64.getEncoder().encodeToString(
            original.toByteArray(Charsets.UTF_8)
                .map { byte -> (byte.toInt() + k).toByte() }
                .toByteArray(),
        )

        assertEquals(original, RouVideoDto.decodeVideoData(encoded, k))
    }

    @Test
    fun `normalizes relative hls endpoint against rouvideo origin`() {
        assertEquals(
            "https://rou.video/api/hls/example#.m3u8",
            RouVideo.normalizePlaylistUrl("/api/hls/example"),
        )
    }

    @Test
    fun `preserves https while converting index jpg playlist urls`() {
        assertEquals(
            "https://cdn.example/hls/example/index.m3u8?token=abc",
            RouVideo.normalizePlaylistUrl("https://cdn.example/hls/example/index.jpg?token=abc"),
        )
    }

    @Test
    fun `routes api hls videos through local m3u8 server`() {
        val playlistUrl = RouVideo.normalizePlaylistUrl("/api/hls/example")
        val integration = M3u8Integration(OkHttpClient())
        try {
            val video =
                Video(
                    url = playlistUrl,
                    quality = "Video",
                    videoUrl = playlistUrl,
                )
            val processedVideo = integration.processVideoList(listOf(video)).single()
            val processedUrl = requireNotNull(processedVideo.videoUrl)

            assertTrue(processedUrl.startsWith("http://localhost:"))
            assertTrue(processedUrl.contains("/m3u8?url="))
        } finally {
            runCatching { integration.stopServer() }
        }
    }
}
