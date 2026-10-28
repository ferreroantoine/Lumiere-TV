package fr.lumiere.tv.data.server

import android.util.Log
import fr.lumiere.tv.data.api.HealthStatus
import fr.lumiere.tv.data.api.LumiereJson
import fr.lumiere.tv.data.api.SystemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InterruptedIOException

/** Vérifie qu'une adresse mène bien à un serveur Lumière : `/api/health` puis `/api/system`. */
class ServerCheck(private val client: OkHttpClient) {

    suspend fun check(address: ServerAddress): CheckResult = withContext(Dispatchers.IO) {
        try {
            val health = get("${address.baseUrl}/api/health")
            if (health !is Body.Json) return@withContext health.toResult()
            val ok = runCatching { LumiereJson.decodeFromString<HealthStatus>(health.text).status == "ok" }
                .getOrDefault(false)
            if (!ok) return@withContext CheckResult.NotLumiere

            val system = get("${address.baseUrl}/api/system")
            if (system !is Body.Json) return@withContext system.toResult()
            val version = try {
                LumiereJson.decodeFromString<SystemInfo>(system.text).version
            } catch (_: SerializationException) {
                return@withContext CheckResult.NotLumiere
            } catch (_: IllegalArgumentException) {
                return@withContext CheckResult.NotLumiere
            }
            CheckResult.Ok(version)
        } catch (e: InterruptedIOException) {
            Log.w("Lumiere", "Serveur ${address.baseUrl} : délai dépassé", e)
            CheckResult.Timeout
        } catch (e: IOException) {
            Log.w("Lumiere", "Serveur ${address.baseUrl} : injoignable", e)
            CheckResult.Unreachable
        }
    }

    private sealed interface Body {
        data class Json(val text: String) : Body
        data object NotJson : Body
        data class Error(val code: Int) : Body
    }

    private fun Body.toResult(): CheckResult = when (this) {
        is Body.Error -> if (code == 404) CheckResult.NotLumiere else CheckResult.HttpError(code)
        else -> CheckResult.NotLumiere
    }

    private fun get(url: String): Body =
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            when {
                !response.isSuccessful -> Body.Error(response.code)
                response.header("Content-Type")?.contains("json") != true -> Body.NotJson
                else -> Body.Json(response.body.string())
            }
        }
}

sealed interface CheckResult {
    data class Ok(val version: String) : CheckResult
    data object Timeout : CheckResult
    data object Unreachable : CheckResult
    data object NotLumiere : CheckResult
    data class HttpError(val code: Int) : CheckResult
}

/** Message affiché à l'écran de configuration. */
fun CheckResult.message(): String = when (this) {
    is CheckResult.Ok -> "Connecté à Lumière $version."
    CheckResult.Timeout ->
        "Le serveur ne répond pas. Vérifiez que le PC est allumé, que Lumière tourne et que le port est bien redirigé sur la box."
    CheckResult.Unreachable ->
        "Connexion refusée ou adresse introuvable. Vérifiez l'adresse et la redirection de port."
    CheckResult.NotLumiere -> "Un serveur répond, mais ce n'est pas Lumière."
    is CheckResult.HttpError -> "Le serveur a répondu avec l'erreur $code."
}
