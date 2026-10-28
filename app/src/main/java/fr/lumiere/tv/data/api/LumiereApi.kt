package fr.lumiere.tv.data.api

import fr.lumiere.tv.data.server.ServerAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/** Client de l'API Lumière. Toutes les requêtes passent par le même `OkHttpClient`. */
class LumiereApi(
    private val client: OkHttpClient,
    private val baseUrl: () -> ServerAddress,
) {
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun home(): HomeFeed = getJson(url("api/home"))

    suspend fun titles(
        kind: TitleKind?,
        q: String?,
        sort: String?,
        genre: String?,
        page: Int,
        pageSize: Int = 40,
    ): Page<TitleSummary> = getJson(
        url("api/titles") {
            kind?.let { addQueryParameter("kind", it.apiValue) }
            q?.let { addQueryParameter("q", it) }
            sort?.let { addQueryParameter("sort", it) }
            genre?.let { addQueryParameter("genre", it) }
            addQueryParameter("page", page.toString())
            addQueryParameter("pageSize", pageSize.toString())
        },
    )

    suspend fun genres(kind: TitleKind?): List<GenreCount> =
        getJson(url("api/genres") { kind?.let { addQueryParameter("kind", it.apiValue) } })

    suspend fun title(id: Int): TitleDetails = getJson(url("api/titles/$id"))

    suspend fun episodeFiles(id: Int, season: Int, episode: Int): EpisodeFiles =
        getJson(url("api/titles/$id/episodes/$season/$episode"))

    suspend fun library(): List<LibraryItem> = getJson(url("api/library"))

    suspend fun continueWatching(): List<ContinueWatchingItem> = getJson(url("api/playback/continue"))

    suspend fun createSession(request: PlaybackRequest): PlaybackSession =
        sendJson("POST", url("api/playback/sessions"), LumiereJson.encodeToString(request))

    suspend fun streamOrigin(sessionId: String, streamId: String): Double =
        getJson<StreamOrigin>(url("api/playback/sessions/$sessionId/streams/$streamId/origin")).start

    suspend fun subtitles(sessionId: String, trackId: String): String =
        call(Request.Builder().url(url("api/playback/sessions/$sessionId/subtitles/$trackId")).build())

    suspend fun onlineSubtitles(sessionId: String, force: Boolean = false): OnlineSubtitlesResult =
        getJson(url("api/playback/sessions/$sessionId/subtitles/online") { if (force) addQueryParameter("force", "true") })

    suspend fun subtitleSync(sessionId: String, trackId: String): SubtitleSyncState =
        getJson(url("api/playback/sessions/$sessionId/subtitles/$trackId/sync"))

    suspend fun saveSubtitleSync(sessionId: String, trackId: String, offset: Double, rate: Double): SubtitleSync {
        val body = buildJsonObject {
            put("offset", offset)
            put("rate", rate)
        }.toString()
        return sendJson<SavedSubtitleSync>("PUT", url("api/playback/sessions/$sessionId/subtitles/$trackId/sync"), body).sync
    }

    suspend fun resetSubtitleSync(sessionId: String, trackId: String) {
        call(Request.Builder().url(url("api/playback/sessions/$sessionId/subtitles/$trackId/sync")).delete().build())
    }

    suspend fun autoSubtitleSync(sessionId: String, trackId: String): SubtitleSyncResult =
        sendJson("POST", url("api/playback/sessions/$sessionId/subtitles/$trackId/sync/auto"), null)

    /** `duration` est toujours envoyée, `null` comprise : le serveur l'exige. */
    suspend fun saveProgress(sessionId: String, update: ProgressUpdate) {
        val body = buildJsonObject {
            put("position", update.position)
            if (update.duration == null) put("duration", JsonNull) else put("duration", update.duration)
        }.toString()
        call(Request.Builder().url(url("api/playback/sessions/$sessionId/progress")).post(body.toRequestBody(jsonType)).build())
    }

    suspend fun stopSession(sessionId: String) {
        call(Request.Builder().url(url("api/playback/sessions/$sessionId")).delete().build())
    }

    suspend fun setWatched(media: MediaRef, watched: Boolean) {
        val body = buildJsonObject {
            put("media", LumiereJson.encodeToJsonElement(MediaRef.serializer(), media))
            put("watched", watched)
        }.toString()
        call(Request.Builder().url(url("api/playback/watched")).put(body.toRequestBody(jsonType)).build())
    }

    /** URL absolue d'un chemin renvoyé par le serveur (images, flux) ; les URL complètes restent telles quelles. */
    fun absoluteUrl(path: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) path
        else baseUrl().baseUrl + "/" + path.trimStart('/')

    private fun url(path: String, query: HttpUrl.Builder.() -> Unit = {}): HttpUrl =
        "${baseUrl().baseUrl}/$path".toHttpUrl().newBuilder().apply(query).build()

    private suspend inline fun <reified T> getJson(url: HttpUrl): T =
        LumiereJson.decodeFromString(call(Request.Builder().url(url).build()))

    private suspend inline fun <reified T> sendJson(method: String, url: HttpUrl, json: String?): T {
        // Sans corps, pas de type JSON : Fastify refuse un corps JSON vide.
        val body: RequestBody = json?.toRequestBody(jsonType) ?: ByteArray(0).toRequestBody(null)
        return LumiereJson.decodeFromString(call(Request.Builder().url(url).method(method, body).build()))
    }

    private suspend fun call(request: Request): String = withContext(Dispatchers.IO) {
        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw ServerUnreachableException(e)
        }
        response.use {
            val text = try {
                it.body.string()
            } catch (e: IOException) {
                throw ServerUnreachableException(e)
            }
            if (!it.isSuccessful) {
                val message = runCatching { LumiereJson.decodeFromString<ApiErrorBody>(text).message }
                    .getOrNull()?.takeIf { m -> m.isNotBlank() }
                    ?: "Le serveur Lumière a rencontré une erreur (${it.code})."
                throw ApiException(it.code, message)
            }
            text
        }
    }
}
