package eu.kanade.tachiyomi.data.sync

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

@Serializable
data class SyncPayload(
    val type: String,
    val sourceId: Long,
    val mangaUrl: String,
    val chapterNum: Float,
    val deviceId: String = "android_client"
)

@Serializable
data class SyncInstruction(
    val action: String,
    val sourceId: Long,
    val mangaUrl: String,
    val chapterNum: Float
)

interface SyncService {
    @POST("/sync/push")
    suspend fun pushUpdate(@Body payload: SyncPayload)

    @GET("/sync/pull")
    suspend fun pullInstructions(): List<SyncInstruction>
}

object SyncManager {
    private val json = Json { ignoreUnknownKeys = true }

    private var cachedApi: SyncService? = null
    private var cachedHost: String = ""


    private fun getApi(): SyncService? {
        try {
            val prefs = Injekt.get<SyncPreferences>()
            val currentHost = prefs.syncHost().get().trim()

            if (currentHost.isBlank()) {
                logcat(LogPriority.WARN) { "SYNC: Host non configurato nelle impostazioni" }
                return null
            }

            val validUrl = if (currentHost.endsWith("/")) currentHost else "$currentHost/"

            if (cachedApi == null || validUrl != cachedHost) {
                logcat { "SYNC: Rilevato cambio host. Nuovo: $validUrl (Vecchio: $cachedHost)" }

                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                cachedApi = Retrofit.Builder()
                    .baseUrl(validUrl)
                    .client(client)
                    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                    .build()
                    .create(SyncService::class.java)

                cachedHost = validUrl
            }
            return cachedApi
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SYNC: Errore creazione client: ${e.message}" }
            return null
        }
    }

    fun push(type: String, sourceId: Long, mangaUrl: String, chapterNum: Float) {
        GlobalScope.launch {
            try {
                val api = getApi() ?: return@launch
                api.pushUpdate(SyncPayload(type, sourceId, mangaUrl, chapterNum))
                logcat { "SYNC: Push inviato a $cachedHost ($chapterNum)" }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "SYNC: Push fallito: ${e.message}" }
            }
        }
    }

    suspend fun fetchInstructions(): List<SyncInstruction> {
        return try {
            val api = getApi() ?: return emptyList()
            api.pullInstructions()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SYNC: Pull fallito su $cachedHost: ${e.message}" }
            emptyList()
        }
    }

    fun updateSyncStatus(enabled: Boolean) {
        val prefs = Injekt.get<SyncPreferences>()
        prefs.syncEnabled().set(enabled)
    }

    fun updateInterval(minutes: Long) {
        val prefs = Injekt.get<SyncPreferences>()
        prefs.syncInterval().set(minutes.toInt())
    }

    suspend fun performImmediateSync() {
        fetchInstructions()
    }
}
