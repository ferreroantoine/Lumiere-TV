package fr.lumiere.tv.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Copie des contrats de Lumiere/packages/shared/src (catalog.ts, library.ts, system.ts).
// Relire le fichier TypeScript correspondant avant de modifier un champ.

@Serializable
enum class TitleKind(val apiValue: String) {
    @SerialName("movie") MOVIE("movie"),
    @SerialName("series") SERIES("series"),
}

@Serializable
data class ReleaseInfo(
    val resolution: String? = null,
    val source: String? = null,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val hdr: String? = null,
    val languages: List<String> = emptyList(),
    val service: String? = null,
    val edition: String? = null,
    val group: String? = null,
)

@Serializable
data class WatchState(
    val position: Double,
    val duration: Double? = null,
    val completed: Boolean = false,
    val updatedAt: String? = null,
)

/** Téléchargement en cours d'un fichier (seuls les champs utiles à la TV). */
@Serializable
data class DownloadRef(val id: Int, val status: String, val progress: Double? = null)

@Serializable
data class MediaAvailability(
    val download: DownloadRef? = null,
    val libraryItemId: Int? = null,
    val watch: WatchState? = null,
)

@Serializable
data class RemoteFile(
    val name: String,
    val path: String,
    val size: Long? = null,
    val modifiedAt: String? = null,
    val release: ReleaseInfo = ReleaseInfo(),
    val season: Int? = null,
    val episode: Int? = null,
    val episodeEnd: Int? = null,
    val availability: MediaAvailability = MediaAvailability(),
    val label: String? = null,
)

@Serializable
data class TitleSummary(
    val id: Int,
    val kind: TitleKind,
    val title: String,
    val year: Int? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: Double? = null,
    val genres: List<String> = emptyList(),
    val overview: String? = null,
    val libraryCount: Int = 0,
    val sourceStatus: String? = null,
)

@Serializable
data class EpisodeEntry(
    val key: String,
    val season: Int? = null,
    val episode: Int? = null,
    val episodeEnd: Int? = null,
    val title: String,
    val overview: String? = null,
    val stillUrl: String? = null,
    val airDate: String? = null,
    val runtime: Int? = null,
    val files: List<RemoteFile> = emptyList(),
)

@Serializable
data class SeasonEntry(
    val key: String,
    val number: Int? = null,
    val name: String,
    val posterUrl: String? = null,
    val overview: String? = null,
    val episodes: List<EpisodeEntry> = emptyList(),
)

@Serializable
data class TitleDetails(
    val id: Int,
    val kind: TitleKind,
    val title: String,
    val year: Int? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: Double? = null,
    val genres: List<String> = emptyList(),
    val overview: String? = null,
    val libraryCount: Int = 0,
    val originalTitle: String? = null,
    val tagline: String? = null,
    val runtime: Int? = null,
    val releaseDate: String? = null,
    val files: List<RemoteFile> = emptyList(),
    val seasons: List<SeasonEntry> = emptyList(),
    val sourceNotice: String? = null,
)

@Serializable
data class EpisodeFiles(val files: List<RemoteFile> = emptyList(), val notice: String? = null)

@Serializable
data class Page<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val hasMore: Boolean,
)

@Serializable
data class HomeFeed(
    val featured: List<TitleSummary> = emptyList(),
    val popularMovies: List<TitleSummary> = emptyList(),
    val popularSeries: List<TitleSummary> = emptyList(),
)

@Serializable
data class GenreCount(val name: String, val count: Int)

@Serializable
data class LibraryItem(
    val id: Int,
    val kind: String,
    val titleId: Int? = null,
    val title: String,
    val season: Int? = null,
    val episode: Int? = null,
    val year: Int? = null,
    val fileName: String,
    val size: Long = 0,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val release: ReleaseInfo = ReleaseInfo(),
    val watch: WatchState? = null,
    val exists: Boolean = true,
)

@Serializable
data class SystemInfo(val version: String, val tmdbConfigured: Boolean = false)

@Serializable
data class HealthStatus(val status: String)

@Serializable
data class ApiErrorBody(val error: String = "", val message: String)
