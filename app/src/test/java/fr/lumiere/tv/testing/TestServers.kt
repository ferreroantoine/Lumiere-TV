package fr.lumiere.tv.testing

import fr.lumiere.tv.data.server.ServerAddress
import fr.lumiere.tv.data.api.LumiereApi
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Réponse JSON (ou texte) préparée pour MockWebServer. */
fun MockWebServer.reply(code: Int = 200, body: String = "", delayMs: Long = 0, contentType: String = "application/json") {
    val builder = MockResponse.Builder().code(code).setHeader("Content-Type", contentType).body(body)
    if (delayMs > 0) builder.headersDelay(delayMs, TimeUnit.MILLISECONDS)
    enqueue(builder.build())
}

/** Réponses selon le chemin demandé (sans la requête) ; chemin inconnu : 404. */
fun MockWebServer.routes(vararg routes: Pair<String, Pair<Int, String>>) {
    val table = routes.toMap()
    dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val (code, body) = table[request.url.encodedPath] ?: (404 to """{"error":"not_found","message":"Introuvable"}""")
            return MockResponse.Builder().code(code).setHeader("Content-Type", "application/json").body(body).build()
        }
    }
}

/** Adresse du faux serveur, sans barre finale. */
fun MockWebServer.address(): ServerAddress = ServerAddress(url("/").toString().trimEnd('/'))

/** Client de test aux délais courts. */
fun testClient(readTimeoutMs: Long = 1_000): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(1, TimeUnit.SECONDS)
    .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
    .build()

/** API branchée sur le faux serveur. */
fun MockWebServer.api(): LumiereApi {
    val address = address()
    return LumiereApi(testClient()) { address }
}

fun fixture(name: String): String =
    requireNotNull(object {}.javaClass.classLoader?.getResource("fixtures/$name.json")) { "fixture $name absente" }
        .readText()
