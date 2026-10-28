package fr.lumiere.tv.testing

import fr.lumiere.tv.data.server.ServerAddress
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Réponse JSON (ou texte) préparée pour MockWebServer. */
fun MockWebServer.reply(code: Int = 200, body: String = "", delayMs: Long = 0, contentType: String = "application/json") {
    val builder = MockResponse.Builder().code(code).setHeader("Content-Type", contentType).body(body)
    if (delayMs > 0) builder.headersDelay(delayMs, TimeUnit.MILLISECONDS)
    enqueue(builder.build())
}

/** Adresse du faux serveur, sans barre finale. */
fun MockWebServer.address(): ServerAddress = ServerAddress(url("/").toString().trimEnd('/'))

/** Client de test aux délais courts. */
fun testClient(readTimeoutMs: Long = 1_000): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(1, TimeUnit.SECONDS)
    .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
    .build()

fun fixture(name: String): String =
    requireNotNull(object {}.javaClass.classLoader?.getResource("fixtures/$name.json")) { "fixture $name absente" }
        .readText()
