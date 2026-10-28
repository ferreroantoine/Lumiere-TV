# Client Android TV de Lumière : plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Une application Android TV (Kotlin, Compose for TV, Media3) qui se connecte par Internet au serveur Lumière de l'utilisateur pour parcourir le catalogue, la bibliothèque, et lire films et épisodes à la télécommande.

**Architecture:** Un seul module Gradle `app` (paquet `fr.lumiere.tv`). Couche `data` (adresse du serveur, client HTTP unique, DTO miroirs de `Lumiere/packages/shared`), couche `player` (capacités, sous-titres, contrôleur de lecture testable sans Android derrière l'interface `PlayerPort`), couche `ui` (un ViewModel + `StateFlow` par écran). Composition manuelle dans `AppContainer`.

**Tech Stack:** AGP 9.4.1 (Kotlin intégré), Gradle 9.8.2, JDK 21, Kotlin 2.4.21, Compose BOM 2026.09.00, androidx.tv:tv-material 1.1.0, Media3 1.11.1, OkHttp 5.5.1 (+ mockwebserver3), kotlinx.serialization 1.12.0, kotlinx.coroutines 1.11.0, Coil 3.6.3, DataStore 1.3.0, Navigation Compose 2.10.2, Lifecycle 2.11.0, Activity Compose 1.13.0, core-ktx 1.19.1, Palette 1.1.0, JUnit 4.13.2.

**Spec:** [docs/superpowers/specs/2026-10-28-client-android-tv-design.md](../specs/2026-10-28-client-android-tv-design.md). Lire aussi `CLAUDE.md` à la racine.

## Global Constraints

- Versions ci-dessus vérifiées le 2026-10-28 (dépôts Maven Google et Maven Central). AGP 9.4 exige Gradle 9.6.0 minimum, JDK 17 minimum, SDK Build Tools 36.0.0, API maximale 37.
- AGP 9 intègre Kotlin : **ne pas** appliquer `org.jetbrains.kotlin.android`. Appliquer `org.jetbrains.kotlin.plugin.compose` et `org.jetbrains.kotlin.plugin.serialization` en 2.4.21. Si AGP signale un écart de version de Kotlin, aligner ces deux plugins sur la version qu'il indique, et le noter dans `CLAUDE.md`.
- `compileSdk = 37`, `targetSdk = 36`, `minSdk = 26`. `applicationId` et paquet : `fr.lumiere.tv`. Nom affiché : `Lumière`.
- Appli TV uniquement : `android.software.leanback` requis, `android.hardware.touchscreen` non requis, activité avec catégorie `LEANBACK_LAUNCHER`, bannière 320 x 180.
- HTTP en clair autorisé (`network_security_config.xml`, `cleartextTrafficPermitted="true"`), HTTPS accepté.
- **Un seul `OkHttpClient`** pour l'API, Coil, ExoPlayer (via `OkHttpDataSource`) et les sous-titres.
- Délais de l'API : connexion 10 s, lecture 30 s. Port par défaut 8484.
- `Json { ignoreUnknownKeys = true; explicitNulls = false }`. Discriminant de `MediaRef` : `type` (`remote`, `library`, `episode`).
- Capacités : noms identiques à `Lumiere/apps/web/src/lib/capabilities.ts` (`h264`, `hevc`, `av1`, `vp9` ; `aac`, `mp3`, `opus`, `flac`, `ac3`, `eac3` ; conteneurs `mp4`, `mkv`, `webm`).
- Recalage des sous-titres : instant d'affichage = `rate * t + offset` (secondes) ; `rate` entre 0,8 et 1,25 ; pas manuel 0,25 s.
- Progression envoyée toutes les 10 s, à la pause et en quittant. Compte à rebours de 10 s dans les 20 dernières secondes pour l'épisode suivant. Avance et recul : 10 s.
- Zone de sécurité : marges de 48 dp horizontales et 27 dp verticales. Pas de flou en temps réel.
- Toute l'interface en français ; messages du serveur (`ApiErrorBody.message`) affichés tels quels. Pas d'emoji ni de tiret cadratin.
- Le dépôt Lumière n'est **pas** modifié.
- Commandes Gradle sous Windows : `.\gradlew.bat …` depuis `androidclient`.

## Review Focus

1. **Serveur qui disparaît en cours d'usage** (PC éteint, IP publique changée) : chaque écran affiche l'écran d'erreur avec Réessayer et Changer de serveur au lieu de planter ; le lecteur affiche un message et revient à la fiche. Tests : Task 4 (`IOException` vers `ServerUnreachableException`), Task 6 (`UiState.Error` quand l'API échoue).
2. **Titre sans fichier ou extension à sa limite** (`files` vide, `sourceNotice` renseigné) : Lecture désactivée, message du serveur visible. Test : Task 8 (`playAction`).
3. **Quitter le lecteur pendant le chargement ou la lecture** (Retour) : la position est envoyée et la session fermée (`DELETE`), sans fuite. Test : Task 12 (`stop`).
4. **Sous-titres vides ou mal formés** (VTT sans en-tête, horodatage invalide, fichier vide) : aucun plantage, liste de répliques vide. Test : Task 11.
5. **Adresse saisie maladroitement** (espaces, barre finale, port non numérique, `https` sans port) : normalisée ou refusée avec un message clair. Test : Task 3.

---

## Structure des fichiers

```
settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew(.bat), gradle/wrapper/*
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/res/xml/network_security_config.xml
app/src/main/res/drawable/banner.xml, app/src/main/res/values/strings.xml
app/src/main/java/fr/lumiere/tv/
  LumiereApp.kt, AppContainer.kt, MainActivity.kt
  data/api/        Dto.kt (catalogue, bibliothèque, système, erreurs), PlaybackDto.kt, LumiereJson.kt,
                   LumiereApi.kt, ApiExceptions.kt
  data/server/     ServerAddress.kt, ServerCheck.kt
  data/prefs/      ServerPrefs.kt
  player/          Capabilities.kt, PlayerPort.kt, ExoPlayerPort.kt, PlaybackController.kt, StreamIds.kt
  player/subtitles/ WebVtt.kt, SubtitleTimeline.kt
  ui/theme/        Theme.kt (couleurs, typographie, formes)
  ui/components/   PosterCard.kt, MediaRow.kt, AmbientBackground.kt, StatusScreens.kt, ReleaseBadges.kt
  ui/navigation/   Routes.kt, AppShell.kt
  ui/setup/ ui/home/ ui/catalog/ ui/search/ ui/title/ ui/library/ ui/player/ ui/settings/
  ui/Format.kt     textes calculés (reprise, durées)
app/src/test/java/fr/lumiere/tv/…   tests JVM
app/src/test/resources/fixtures/*.json
```

---

### Task 0: Outillage du PC et dépôt git

**Files:**
- Create: `.gitignore`

**Interfaces:**
- Consumes: rien.
- Produces: JDK 21, SDK Android dans `%LOCALAPPDATA%\Android\Sdk` (`platform-tools`, `platforms;android-37`, `build-tools;36.0.0`), `adb` dans le PATH, dépôt git initialisé.

- [ ] **Step 1: Demander l'accord de l'utilisateur** pour installer le JDK 21 (`winget install EclipseAdoptium.Temurin.21.JDK`), les outils en ligne de commande du SDK Android et Gradle 9.8.2 (pour générer le wrapper), et pour **accepter les licences du SDK Android** (`sdkmanager --licenses`) : l'acceptation des licences revient à l'utilisateur, la lui faire valider explicitement.
- [ ] **Step 2: Installer le JDK 21**, puis vérifier : `& "C:\Program Files\Eclipse Adoptium\jdk-21*\bin\java.exe" -version` affiche `21`. Définir `JAVA_HOME` pour l'utilisateur.
- [ ] **Step 3: Installer les outils du SDK.** Lire le nom exact de l'archive Windows sur https://developer.android.com/studio#command-tools (ne pas deviner le numéro), la décompresser dans `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest`, définir `ANDROID_HOME`, puis : `sdkmanager "platform-tools" "platforms;android-37" "build-tools;36.0.0"`. Vérifier : `adb version` répond.
- [ ] **Step 4: Initialiser git** dans `androidclient` (`git init -b main`), `.gitignore` Android standard (`.gradle/`, `build/`, `local.properties`, `.idea/`, `*.iml`, `captures/`, `.kotlin/`).
- [ ] **Step 5: Commit** de `CLAUDE.md`, `docs/`, `.gitignore` : `git commit -m "docs: conception et plan du client Android TV"`.

### Task 1: Squelette Gradle et appli vide installée sur le stick

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, wrapper Gradle, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/network_security_config.xml`, `app/src/main/res/drawable/banner.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/java/fr/lumiere/tv/MainActivity.kt`, `app/src/main/java/fr/lumiere/tv/LumiereApp.kt`
- Test: `app/src/test/java/fr/lumiere/tv/SmokeTest.kt`

**Interfaces:**
- Produces: `MainActivity` (hôte Compose), `LumiereApp : Application` (déclarée dans le manifeste), catalogue de versions `libs.*` utilisé par toutes les tâches.

- [ ] **Step 1: Générer le wrapper** avec Gradle 9.8.2 téléchargé (`gradle wrapper --gradle-version 9.8.2`), puis supprimer l'installation temporaire.
- [ ] **Step 2: Écrire la configuration Gradle** avec les versions et contraintes de « Global Constraints » (catalogue `libs.versions.toml`, plugins `com.android.application`, `org.jetbrains.kotlin.plugin.compose`, `org.jetbrains.kotlin.plugin.serialization`), `buildFeatures { compose = true }`, `testOptions.unitTests.isReturnDefaultValues = true`. Dépendances de test : `junit`, `kotlinx-coroutines-test`, `mockwebserver3`.
- [ ] **Step 3: Manifeste** : permission `INTERNET`, `uses-feature` leanback requis et touchscreen non requis, `android:banner="@drawable/banner"`, `android:networkSecurityConfig`, `MainActivity` exportée avec `MAIN` + `LEANBACK_LAUNCHER`. Bannière : fond sombre et texte « Lumière ».
- [ ] **Step 4: `MainActivity`** affiche « Lumière » centré dans un `Surface` de tv-material.
- [ ] **Step 5: Test fumée** `SmokeTest.junitFonctionne` (assertion triviale) pour valider la chaîne de test.
- [ ] **Step 6: Construire et tester.** Run: `.\gradlew.bat assembleDebug testDebugUnitTest`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 7: Installer sur le stick.** Run: `adb connect 192.168.1.160:5555` (l'utilisateur accepte l'autorisation sur la TV), `.\gradlew.bat installDebug`, `adb shell am start -n fr.lumiere.tv/.MainActivity`. Expected: « Lumière » s'affiche sur la TV et l'icône apparaît dans les applis. Noter la version d'Android du stick (`adb shell getprop ro.build.version.release`) dans `CLAUDE.md`.
- [ ] **Step 8: Mettre à jour la section Commandes de `CLAUDE.md`** avec les commandes réelles, puis commit : `git commit -m "feat: squelette de l'appli Android TV"`.

### Task 2: DTO de l'API et décodage JSON

**Files:**
- Create: `app/src/main/java/fr/lumiere/tv/data/api/Dto.kt`, `PlaybackDto.kt`, `LumiereJson.kt`
- Create: `app/src/test/resources/fixtures/{home,titles-page,title-movie,title-series,episode-files,library,continue,system,session-direct,session-transcode,error}.json`
- Test: `app/src/test/java/fr/lumiere/tv/data/api/DtoDecodingTest.kt`

**Interfaces:**
- Produces (champs identiques aux noms TypeScript de `Lumiere/packages/shared/src`, `null` pour les champs `| null`, `List` pour les tableaux) :
  - `catalog.ts` : `enum class TitleKind(val apiValue: String) { MOVIE("movie"), SERIES("series") }` (avec `@SerialName`), `ReleaseInfo`, `WatchState`, `MediaAvailability` (sans le statut détaillé de téléchargement : `download` décodé en `DownloadRef(id: Int, status: String, progress: Double?)`), `RemoteFile`, `TitleSummary`, `EpisodeEntry`, `SeasonEntry`, `TitleDetails`, `EpisodeFiles`, `Page<T>`, `HomeFeed` (sans `sections`), `GenreCount`.
  - `library.ts` : `LibraryItem` (`kind` en `String`).
  - `playback.ts` : `sealed interface MediaRef` avec `Remote(path)`, `Library(id)`, `Episode(titleId, season, episode)` ; `ClientCapabilities`, `PlaybackRequest(media, capabilities, audioTrack: Int? = null, mode: String = "auto")`, `AudioTrackInfo`, `SubtitleTrackInfo`, `SubtitleSync`, `SubtitleSyncState`, `SubtitleSyncResult`, `OnlineSubtitlesResult`, `PlaybackSession` (dont `VideoInfo`, `NextMedia(media, label)`, `OnlineSubtitlesInfo(available, autoLanguage)`), `ProgressUpdate`, `ContinueWatchingItem`, `StreamOrigin(start: Double)`.
  - `system.ts` : `SystemInfo` (seulement `version`, `tmdbConfigured`), `ApiErrorBody(error, message)`, `HealthStatus(status)`.
  - `val LumiereJson: Json` (voir Global Constraints).

- [ ] **Step 1: Capturer les fixtures** depuis la démo de Lumière : dans `../Lumiere`, `npm install`, `npm run build`, `npm run demo` (exige ffmpeg ; demander à l'utilisateur s'il manque), puis enregistrer les réponses de `/api/home`, `/api/titles?kind=movie&page=1&pageSize=5`, une fiche film, une fiche série, un épisode, `/api/library`, `/api/playback/continue`, `/api/system`, une session `POST /api/playback/sessions` en mode direct puis avec `"mode":"transcode"`, et une erreur (`/api/titles/999999`). Si la démo ne peut pas tourner, écrire les fixtures à la main à partir des types TypeScript et le dire dans le message de commit.
- [ ] **Step 2: Écrire les tests** dans `DtoDecodingTest` : chaque fixture se décode sans exception ; `title-series` a au moins une saison avec des épisodes ; `session-direct.mode == "direct"` et `seek == "native"` ; `error.message` non vide ; `LumiereJson.encodeToString<MediaRef>(MediaRef.Episode(12, 1, 3))` vaut `{"type":"episode","titleId":12,"season":1,"episode":3}` ; `PlaybackRequest` avec `audioTrack = null` n'émet pas la clé `audioTrack`.
- [ ] **Step 3: Lancer les tests.** Run: `.\gradlew.bat testDebugUnitTest --tests "*DtoDecodingTest*"`. Expected: FAIL (classes absentes).
- [ ] **Step 4: Écrire les DTO** et `LumiereJson`.
- [ ] **Step 5: Relancer.** Expected: PASS.
- [ ] **Step 6: Commit** : `git commit -m "feat: contrats de l'API Lumière en Kotlin"`.

### Task 3: Adresse du serveur, test de connexion, mémorisation

**Files:**
- Create: `app/src/main/java/fr/lumiere/tv/data/server/ServerAddress.kt`, `ServerCheck.kt`, `app/src/main/java/fr/lumiere/tv/data/prefs/ServerPrefs.kt`
- Test: `app/src/test/java/fr/lumiere/tv/data/server/ServerAddressTest.kt`, `ServerCheckTest.kt`

**Interfaces:**
- Consumes: `LumiereJson`, `HealthStatus`, `SystemInfo` (Task 2).
- Produces:
  - `@JvmInline value class ServerAddress(val baseUrl: String)` (sans barre finale) ; `companion fun parse(input: String): ParseResult` ; `sealed interface ParseResult { data class Ok(val address: ServerAddress); data class Invalid(val message: String) }`.
  - `class ServerCheck(client: OkHttpClient) { suspend fun check(address: ServerAddress): CheckResult }` ; `sealed interface CheckResult { data class Ok(val version: String); data object Timeout; data object Unreachable; data object NotLumiere; data class HttpError(val code: Int) }` ; `fun CheckResult.message(): String` (textes en français).
  - `class ServerPrefs(dataStore: DataStore<Preferences>) { val address: Flow<ServerAddress?>; suspend fun save(address: ServerAddress); suspend fun clear() }`.

- [ ] **Step 1: Tests `ServerAddressTest`** : `"1.2.3.4"` donne `http://1.2.3.4:8484` ; `"  mon.dyndns.org:9000  "` donne `http://mon.dyndns.org:9000` ; `"http://exemple.fr:8484/"` donne `http://exemple.fr:8484` ; `"https://exemple.fr"` donne `https://exemple.fr` (port HTTPS implicite, pas 8484) ; `"HTTP://Exemple.FR"` donne `http://exemple.fr:8484` ; `""` donne `Invalid("Saisissez l'adresse du serveur.")` ; `"exemple.fr:abc"` donne `Invalid("Le port doit être un nombre entre 1 et 65535.")` ; `"exemple.fr:70000"` donne le même `Invalid` ; `"ftp://exemple.fr"` donne `Invalid("Adresse en http:// ou https:// uniquement.")`.
- [ ] **Step 2: Tests `ServerCheckTest`** (MockWebServer, client de test avec délai de lecture de 1 s) : `/api/health` `{"status":"ok"}` puis `/api/system` `{"version":"1.0.0","tmdbConfigured":true}` donnent `Ok("1.0.0")` ; `/api/health` 404 donne `NotLumiere` ; `/api/health` 200 avec du HTML donne `NotLumiere` ; `/api/health` 500 donne `HttpError(500)` ; réponse retardée de 3 s donne `Timeout` ; serveur arrêté donne `Unreachable`.
- [ ] **Step 3: Vérifier l'échec** : `.\gradlew.bat testDebugUnitTest --tests "*server*"`. Expected: FAIL.
- [ ] **Step 4: Implémenter** `ServerAddress.parse` (via `HttpUrl` d'OkHttp après ajout du schéma `http://` si absent ; port 8484 seulement quand ni port ni schéma `https` n'est donné), `ServerCheck`, `ServerPrefs` (clé `server_base_url`). Messages de `CheckResult.message()` : `Timeout` = « Le serveur ne répond pas. Vérifiez que le PC est allumé, que Lumière tourne et que le port est bien redirigé sur la box. » ; `Unreachable` = « Connexion refusée ou adresse introuvable. Vérifiez l'adresse et la redirection de port. » ; `NotLumiere` = « Un serveur répond, mais ce n'est pas Lumière. » ; `HttpError` = « Le serveur a répondu avec l'erreur <code>. ».
- [ ] **Step 5: Vérifier le succès.** Expected: PASS.
- [ ] **Step 6: Commit** : `git commit -m "feat: adresse du serveur et test de connexion"`.

### Task 4: Client de l'API et conteneur

**Files:**
- Create: `app/src/main/java/fr/lumiere/tv/data/api/LumiereApi.kt`, `ApiExceptions.kt`, `app/src/main/java/fr/lumiere/tv/AppContainer.kt`
- Modify: `LumiereApp.kt` (crée `AppContainer`)
- Test: `app/src/test/java/fr/lumiere/tv/data/api/LumiereApiTest.kt`

**Interfaces:**
- Consumes: DTO (Task 2), `ServerAddress`, `ServerPrefs`, `ServerCheck` (Task 3).
- Produces:
  - `class ApiException(val status: Int, override val message: String) : Exception(message)` ; `class ServerUnreachableException(cause: Throwable) : Exception(cause)`.
  - `class LumiereApi(client: OkHttpClient, baseUrl: () -> ServerAddress)` avec (toutes `suspend`) : `home(): HomeFeed`, `titles(kind: TitleKind?, q: String?, sort: String?, genre: String?, page: Int, pageSize: Int = 40): Page<TitleSummary>`, `genres(kind: TitleKind?): List<GenreCount>`, `title(id: Int): TitleDetails`, `episodeFiles(id: Int, season: Int, episode: Int): EpisodeFiles`, `library(): List<LibraryItem>`, `continueWatching(): List<ContinueWatchingItem>`, `createSession(request: PlaybackRequest): PlaybackSession`, `streamOrigin(sessionId: String, streamId: String): Double`, `subtitles(sessionId: String, trackId: String): String`, `onlineSubtitles(sessionId: String, force: Boolean = false): OnlineSubtitlesResult`, `subtitleSync(sessionId: String, trackId: String): SubtitleSyncState`, `saveSubtitleSync(sessionId: String, trackId: String, offset: Double, rate: Double): SubtitleSync`, `resetSubtitleSync(sessionId: String, trackId: String)`, `autoSubtitleSync(sessionId: String, trackId: String): SubtitleSyncResult`, `saveProgress(sessionId: String, update: ProgressUpdate)`, `stopSession(sessionId: String)`, `setWatched(media: MediaRef, watched: Boolean)` ; et non suspendue `absoluteUrl(path: String): String` (laisse intactes les URL déjà absolues).
  - `class AppContainer(context: Context)` : `httpClient` (unique, délais 10 s / 30 s), `serverPrefs`, `serverCheck`, `api`, `imageLoader` (Coil sur `httpClient`), `currentAddress: StateFlow<ServerAddress?>`.

- [ ] **Step 1: Tests `LumiereApiTest`** (MockWebServer) : `titles(TitleKind.MOVIE, "amélie", "popular", null, 2)` envoie `GET /api/titles?kind=movie&q=am%C3%A9lie&sort=popular&page=2&pageSize=40` ; `createSession` envoie un `POST` dont le corps contient `"type":"episode"` ; `saveSubtitleSync` envoie `PUT …/subtitles/3/sync` avec `{"offset":1.5,"rate":1.0}` ; une réponse 404 `{"error":"not_found","message":"Titre introuvable"}` lève `ApiException(404, "Titre introuvable")` ; une réponse 500 sans JSON lève `ApiException(500, "Le serveur Lumière a rencontré une erreur (500).")` ; serveur arrêté lève `ServerUnreachableException` ; `absoluteUrl("/api/images/w342/a.jpg")` vaut `<base>/api/images/w342/a.jpg` et `absoluteUrl("https://x/y.jpg")` reste inchangée ; `streamOrigin` lit `{"start":12.48}` en `12.48`.
- [ ] **Step 2: Vérifier l'échec.** Run: `.\gradlew.bat testDebugUnitTest --tests "*LumiereApiTest*"`. Expected: FAIL.
- [ ] **Step 3: Implémenter** `LumiereApi` (OkHttp + `LumiereJson`, appels sur `Dispatchers.IO`, `IOException` transformée en `ServerUnreachableException`), `ApiExceptions`, `AppContainer`, et brancher `LumiereApp`.
- [ ] **Step 4: Vérifier le succès.** Expected: PASS.
- [ ] **Step 5: Commit** : `git commit -m "feat: client de l'API Lumière"`.

### Task 5: Thème, coquille de navigation et écran de configuration

**Files:**
- Create: `ui/theme/Theme.kt`, `ui/components/StatusScreens.kt`, `ui/navigation/Routes.kt`, `ui/navigation/AppShell.kt`, `ui/setup/SetupScreen.kt`, `ui/setup/SetupViewModel.kt`, `ui/settings/SettingsScreen.kt`
- Modify: `MainActivity.kt`
- Test: `app/src/test/java/fr/lumiere/tv/ui/setup/SetupViewModelTest.kt`

**Interfaces:**
- Consumes: `AppContainer`, `ServerAddress.parse`, `ServerCheck`, `ServerPrefs` (Tasks 3-4).
- Produces:
  - `@Composable fun LumiereTheme(content: @Composable () -> Unit)`. Couleurs reprises des jetons de `Lumiere/docs/DESIGN.md` (lire la section « Jetons » ; fond sombre, texte clair, accent).
  - `@Composable fun LoadingScreen()`, `@Composable fun ErrorScreen(message: String, onRetry: () -> Unit, onChangeServer: (() -> Unit)?)`, `@Composable fun EmptyMessage(text: String)`.
  - `sealed interface Route` : `Setup`, `Home`, `Movies`, `Series`, `Search`, `Library`, `Settings`, `Title(id: Int)`, `Player(media: MediaRef, titleId: Int?)` (sérialisable pour Navigation Compose).
  - `@Composable fun AppShell(container: AppContainer)` : démarre sur `Setup` si aucune adresse ou si `/api/health` échoue, sinon sur `Home` ; `NavigationDrawer` (Rechercher, Accueil, Films, Séries, Bibliothèque, Réglages) autour des écrans de section ; Retour depuis une section ramène à `Home`, depuis `Home` quitte.
  - `class SetupViewModel(check: ServerCheck, prefs: ServerPrefs) { val state: StateFlow<SetupState>; fun submit(input: String) }` ; `sealed interface SetupState { Idle; Checking; data class Failed(val message: String); data class Connected(val version: String) }`. `Connected` après `prefs.save`.

- [ ] **Step 1: Tests `SetupViewModelTest`** (faux `ServerCheck` et `ServerPrefs` en mémoire, `kotlinx-coroutines-test`) : `submit("")` donne `Failed("Saisissez l'adresse du serveur.")` sans appeler `check` ; `submit("1.2.3.4")` avec un `check` qui renvoie `Ok("1.0.0")` passe par `Checking` puis `Connected("1.0.0")` et enregistre `http://1.2.3.4:8484` ; un `check` qui renvoie `Timeout` donne `Failed(CheckResult.Timeout.message())` et n'enregistre rien.
- [ ] **Step 2: Vérifier l'échec**, puis implémenter `SetupViewModel`, puis vérifier le succès. Run: `.\gradlew.bat testDebugUnitTest --tests "*SetupViewModelTest*"`.
- [ ] **Step 3: Implémenter le thème, les écrans d'état, `Routes`, `AppShell`, `SetupScreen`** (titre « Connexion à Lumière », champ d'adresse avec focus initial, bouton « Tester et enregistrer », message d'état, rappel en une ligne : « Votre serveur Lumière est exposé sur Internet sans mot de passe. ») et **`SettingsScreen`** (adresse actuelle, bouton « Changer de serveur » qui efface l'adresse et ouvre `Setup`, version de l'appli). Les autres sections affichent provisoirement `EmptyMessage("Bientôt disponible")`.
- [ ] **Step 4: Vérifier sur le stick.** Lancer la démo Lumière avec `HOST=0.0.0.0` sur le PC, `.\gradlew.bat installDebug`, saisir `192.168.1.25` à la télécommande. Expected: « Connecté à Lumière <version> » puis Accueil ; au redémarrage de l'appli, arrivée directe sur l'Accueil ; adresse fausse : message `Unreachable` ou `Timeout`. Le menu latéral se parcourt au pavé directionnel sans piège.
- [ ] **Step 5: Commit** : `git commit -m "feat: configuration du serveur et navigation"`.

### Task 6: Composants d'affiche et Accueil

**Files:**
- Create: `ui/components/PosterCard.kt`, `MediaRow.kt`, `AmbientBackground.kt`, `ui/home/HomeScreen.kt`, `ui/home/HomeViewModel.kt`, `ui/Format.kt`
- Test: `app/src/test/java/fr/lumiere/tv/ui/home/HomeViewModelTest.kt`, `app/src/test/java/fr/lumiere/tv/ui/FormatTest.kt`

**Interfaces:**
- Consumes: `LumiereApi.home`, `continueWatching`, `library`, `absoluteUrl` (Task 4) ; `ErrorScreen`, `Route` (Task 5).
- Produces:
  - `sealed interface UiState<out T> { data object Loading; data class Ready<T>(val value: T); data class Error(val message: String, val unreachable: Boolean) }` dans `ui/Format.kt` ; `fun Throwable.toUiError(): UiState.Error` (`ServerUnreachableException` : « Lumière est injoignable. Le PC est peut-être éteint, Lumière arrêté, ou l'adresse a changé. » avec `unreachable = true` ; `ApiException` : son `message` ; autre : « Une erreur inattendue est survenue. »).
  - `fun formatResume(positionSec: Double): String` : `"Reprendre à 42 min"`, `"Reprendre à 1 h 05"`, en dessous de 60 s `"Reprendre au début"`.
  - `fun formatRuntime(minutes: Int): String` : `"1 h 52"`, `"45 min"`.
  - `@Composable fun PosterCard(title: String, imageUrl: String?, onClick: () -> Unit, onFocus: (String?) -> Unit, progress: Float? = null, wide: Boolean = false)` : agrandie au focus avec bordure lumineuse ; sans image, tuile sombre avec le titre en texte.
  - `@Composable fun MediaRow(title: String, content: LazyListScope.() -> Unit)`.
  - `@Composable fun AmbientBackground(imageUrl: String?)` : couleur dominante (Palette) de l'image en dégradé sombre, transition de 600 ms ; aucune image : fond du thème.
  - `class HomeViewModel(api: LumiereApi)` : `state: StateFlow<UiState<HomeContent>>`, `fun reload()` ; `data class HomeContent(val featured: TitleSummary?, val resume: List<ContinueWatchingItem>, val movies: List<TitleSummary>, val series: List<TitleSummary>, val library: List<LibraryItem>)`.

- [ ] **Step 1: Tests `FormatTest`** avec les valeurs ci-dessus (`formatResume(2520.0)`, `formatResume(3900.0)`, `formatResume(30.0)`, `formatRuntime(112)`, `formatRuntime(45)`).
- [ ] **Step 2: Tests `HomeViewModelTest`** (vrai `LumiereApi` branché sur un `MockWebServer`, comme pour tous les tests de ViewModel) : contenu complet donne `Ready` avec `featured = home.featured.firstOrNull()` ; `ServerUnreachableException` donne `Error(unreachable = true)` ; `reload()` après erreur repasse par `Loading`.
- [ ] **Step 3: Vérifier l'échec, implémenter `Format.kt` et `HomeViewModel`, vérifier le succès.** Run: `.\gradlew.bat testDebugUnitTest --tests "*FormatTest*" --tests "*HomeViewModelTest*"`.
- [ ] **Step 4: Implémenter `PosterCard`, `MediaRow`, `AmbientBackground`, `HomeScreen`** : bannière haute (fond du titre mis en avant, titre, résumé sur 3 lignes, boutons Lecture et Fiche, focus initial sur Lecture), puis rangées Reprendre la lecture (cartes larges avec barre de progression ; clic : `Route.Player(item.media, item.titleId)`), Films populaires, Séries populaires, Dans ma bibliothèque. Rangées vides masquées. Erreur : `ErrorScreen`. Recharger à chaque retour sur l'écran.
- [ ] **Step 5: Vérifier sur le stick** avec la démo : affiches chargées, fond qui change au focus, défilement fluide, Retour quitte l'appli.
- [ ] **Step 6: Commit** : `git commit -m "feat: accueil"`.

### Task 7: Catalogue Films / Séries et recherche

**Files:**
- Create: `ui/catalog/CatalogScreen.kt`, `ui/catalog/CatalogViewModel.kt`, `ui/catalog/PagedLoader.kt`, `ui/search/SearchScreen.kt`, `ui/search/SearchViewModel.kt`
- Test: `app/src/test/java/fr/lumiere/tv/ui/catalog/PagedLoaderTest.kt`, `app/src/test/java/fr/lumiere/tv/ui/search/SearchViewModelTest.kt`

**Interfaces:**
- Consumes: `LumiereApi.titles`, `genres` (Task 4) ; `PosterCard`, `UiState`, `toUiError` (Task 6).
- Produces:
  - `class PagedLoader<T>(scope: CoroutineScope, fetch: suspend (page: Int) -> Page<T>)` : `items: StateFlow<List<T>>`, `state: StateFlow<UiState<Unit>>`, `fun loadMore()` (sans effet si un chargement est en cours ou si `hasMore == false`), `fun reset()`.
  - `class CatalogViewModel(api: LumiereApi, kind: TitleKind)` : tri par défaut `popular`, options `popular`, `recent`, `title`, `rating` (libellés « Popularité », « Sorties récentes », « Titre », « Note ») ; genre facultatif ; `fun setSort(sort: String)`, `fun setGenre(genre: String?)` relancent `PagedLoader.reset()`.
  - `class SearchViewModel(api: LumiereApi)` : `fun setQuery(q: String)` ; requête lancée 400 ms après la dernière frappe, seulement si `q.trim().length >= 2` ; résultats films et séries mélangés (`kind = null`).

- [ ] **Step 1: Tests `PagedLoaderTest`** : `loadMore()` deux fois de suite pendant un chargement ne déclenche qu'un appel ; après une page avec `hasMore = false`, `loadMore()` ne fait rien ; les pages s'ajoutent dans l'ordre ; une erreur donne `UiState.Error` et un nouveau `loadMore()` retente la même page.
- [ ] **Step 2: Tests `SearchViewModelTest`** (`runTest` et temps virtuel) : trois frappes rapides `"a"`, `"am"`, `"ame"` donnent un seul appel avec `q = "ame"` après 400 ms ; `"a"` seul ne déclenche aucun appel.
- [ ] **Step 3: Vérifier l'échec, implémenter, vérifier le succès.** Run: `.\gradlew.bat testDebugUnitTest --tests "*PagedLoaderTest*" --tests "*SearchViewModelTest*"`.
- [ ] **Step 4: Écrans.** `CatalogScreen(kind)` : barre de tri et de genre en haut, grille d'affiches (`LazyVerticalGrid` standard de Compose ; les listes paresseuses de `androidx.tv.foundation` ne sont pas utilisées) avec `loadMore()` quand le focus approche des 2 dernières lignes ; focus initial sur la première affiche. `SearchScreen` : champ texte avec focus initial, bouton micro qui lance `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` (langue `fr-FR`) seulement si une activité y répond, grille des résultats, `EmptyMessage("Aucun résultat")`. Brancher les routes `Movies`, `Series`, `Search`.
- [ ] **Step 5: Vérifier sur le stick** : défilement infini, tri et genre, recherche au clavier et à la voix si disponible.
- [ ] **Step 6: Commit** : `git commit -m "feat: catalogue et recherche"`.

### Task 8: Fiche d'un titre

**Files:**
- Create: `ui/title/TitleScreen.kt`, `ui/title/TitleViewModel.kt`, `ui/title/TitleActions.kt`, `ui/components/ReleaseBadges.kt`
- Test: `app/src/test/java/fr/lumiere/tv/ui/title/TitleActionsTest.kt`, `app/src/test/java/fr/lumiere/tv/ui/components/ReleaseBadgesTest.kt`

**Interfaces:**
- Consumes: `LumiereApi.title`, `episodeFiles` (Task 4) ; `formatResume`, `formatRuntime`, `UiState` (Task 6) ; `Route.Player`.
- Produces:
  - `sealed interface PlayAction { data class Play(val media: MediaRef, val label: String); data class Disabled(val message: String) }`.
  - `fun playAction(details: TitleDetails): PlayAction` : film avec `files` vide : `Disabled(details.sourceNotice ?: "Aucun fichier proposé pour ce titre.")` ; film : fichier de la version avec `availability.libraryItemId` en priorité (`MediaRef.Library`), sinon le premier de `files` (`MediaRef.Remote(path)`) ; libellé `formatResume(watch.position)` si une position existe et que `completed` est faux, sinon « Lecture » ; série : premier épisode non terminé (`files` regardés) sinon S1E1, en `MediaRef.Episode`, libellé « Lecture S1 É1 » ou la reprise.
  - `fun releaseBadges(release: ReleaseInfo): List<String>` : dans l'ordre résolution, `hdr`, `videoCodec`, `audioCodec`, puis `languages`, sans doublon ni valeur nulle.
  - `class TitleViewModel(api: LumiereApi, titleId: Int)` : `state: StateFlow<UiState<TitleDetails>>`, `selectedSeason: StateFlow<Int>`, `fun versions(season: Int?, episode: Int?): Flow<UiState<List<RemoteFile>>>` (film : `details.files` ; épisode : `api.episodeFiles`).

- [ ] **Step 1: Tests `TitleActionsTest`** : film sans fichier avec `sourceNotice = "L'extension a atteint sa limite de liens"` donne `Disabled` avec ce message ; film sans fichier ni notice donne `Disabled("Aucun fichier proposé pour ce titre.")` ; film avec une copie en bibliothèque donne `Play(MediaRef.Library(id))` ; film avec position 2520 s non terminée donne le libellé `"Reprendre à 42 min"` ; série sans aucun visionnage donne `Play(MediaRef.Episode(id, 1, 1), "Lecture S1 É1")`.
- [ ] **Step 2: Test `ReleaseBadgesTest`** : `ReleaseInfo(resolution = "2160p", hdr = "HDR10", videoCodec = "HEVC", audioCodec = "DD+ 5.1", languages = listOf("MULTi", "VFF"), …)` donne `["2160p", "HDR10", "HEVC", "DD+ 5.1", "MULTi", "VFF"]`.
- [ ] **Step 3: Vérifier l'échec, implémenter, vérifier le succès.** Run: `.\gradlew.bat testDebugUnitTest --tests "*TitleActionsTest*" --tests "*ReleaseBadgesTest*"`.
- [ ] **Step 4: Écran.** Fond plein écran (`backdropUrl`) assombri à gauche ; titre, année, note, durée, genres, accroche, résumé ; boutons Lecture (focus initial ; désactivé avec le message si `Disabled`) et Versions (panneau latéral listant les fichiers avec `releaseBadges`, taille, `label` ; choisir lance `Route.Player(MediaRef.Remote(path))`) ; pour une série, onglets de saisons puis rangée d'épisodes (vignette `stillUrl`, « É3 · Titre », durée, barre de progression ; OK lance l'épisode, appui long ouvre ses versions).
- [ ] **Step 5: Vérifier sur le stick** avec un film et une série de la démo.
- [ ] **Step 6: Commit** : `git commit -m "feat: fiche d'un titre"`.

### Task 9: Bibliothèque

**Files:**
- Create: `ui/library/LibraryScreen.kt`, `ui/library/LibraryViewModel.kt`
- Test: `app/src/test/java/fr/lumiere/tv/ui/library/LibraryViewModelTest.kt`

**Interfaces:**
- Consumes: `LumiereApi.library` (Task 4) ; `PosterCard`, `MediaRow`, `UiState` (Task 6).
- Produces: `class LibraryViewModel(api: LumiereApi)` : `state: StateFlow<UiState<LibraryContent>>` ; `data class LibraryContent(val movies: List<LibraryItem>, val episodes: List<LibraryItem>)` ; seuls les éléments `exists == true` ; `kind == "movie"` dans `movies`, `kind == "episode"` dans `episodes` triés par titre, saison, épisode ; `"other"` ignorés.

- [ ] **Step 1: Test `LibraryViewModelTest`** : répartition et tri ci-dessus ; un élément `exists = false` n'apparaît pas ; liste vide donne `Ready(LibraryContent(emptyList(), emptyList()))`.
- [ ] **Step 2: Vérifier l'échec, implémenter, vérifier le succès.** Run: `.\gradlew.bat testDebugUnitTest --tests "*LibraryViewModelTest*"`.
- [ ] **Step 3: Écran** : rangées Films et Séries (cartes avec « S1 É3 » pour les épisodes, barre de progression depuis `watch`) ; OK lance `Route.Player(MediaRef.Library(id), titleId)` ; vide : `EmptyMessage("Votre bibliothèque est vide. Les téléchargements se lancent depuis Lumière sur le web.")`.
- [ ] **Step 4: Vérifier sur le stick**, puis commit : `git commit -m "feat: bibliothèque"`.

### Task 10: Capacités de décodage du stick

**Files:**
- Create: `app/src/main/java/fr/lumiere/tv/player/Capabilities.kt`
- Test: `app/src/test/java/fr/lumiere/tv/player/CapabilitiesTest.kt`

**Interfaces:**
- Produces:
  - `fun buildCapabilities(decoderMimeTypes: Set<String>, passthroughAudio: Set<String>): ClientCapabilities` (fonction pure). Correspondances : `video/avc` vers `h264`, `video/hevc` vers `hevc`, `video/av01` vers `av1`, `video/x-vnd.on2.vp9` vers `vp9`, `audio/mp4a-latm` vers `aac`, `audio/mpeg` vers `mp3`, `audio/opus` vers `opus`, `audio/flac` vers `flac`, `audio/ac3` vers `ac3`, `audio/eac3` vers `eac3`. `ac3` et `eac3` aussi présents s'ils sont dans `passthroughAudio`. Conteneurs toujours `["mp4", "mkv", "webm"]`. Ordre des listes : celui des correspondances ci-dessus.
  - `fun detectCapabilities(context: Context): ClientCapabilities` : décodeurs de `MediaCodecList(MediaCodecList.REGULAR_CODECS)` (non encodeurs) ; passthrough d'après la classe `AudioCapabilities` de Media3 1.11.1 (vérifier sa signature dans la documentation de Media3 avant de l'écrire), calculé une fois et gardé en mémoire.

- [ ] **Step 1: Tests `CapabilitiesTest`** : `{video/avc, video/hevc, audio/mp4a-latm}` sans passthrough donne `videoCodecs = [h264, hevc]`, `audioCodecs = [aac]` ; `{video/avc, audio/mp4a-latm}` avec passthrough `{audio/eac3}` donne `audioCodecs = [aac, eac3]` ; un type inconnu (`video/mp4v-es`) est ignoré ; `containers == [mp4, mkv, webm]`.
- [ ] **Step 2: Vérifier l'échec, implémenter, vérifier le succès.** Run: `.\gradlew.bat testDebugUnitTest --tests "*CapabilitiesTest*"`.
- [ ] **Step 3: Vérifier sur le stick** : journaliser une fois `detectCapabilities` au démarrage (`adb logcat -s Lumiere`) et noter le résultat dans `CLAUDE.md` (section Environnement).
- [ ] **Step 4: Commit** : `git commit -m "feat: capacités de décodage du stick"`.

### Task 11: Sous-titres WebVTT et recalage

**Files:**
- Create: `app/src/main/java/fr/lumiere/tv/player/subtitles/WebVtt.kt`, `SubtitleTimeline.kt`
- Test: `app/src/test/java/fr/lumiere/tv/player/subtitles/WebVttTest.kt`, `SubtitleTimelineTest.kt`

**Interfaces:**
- Produces:
  - `data class Cue(val start: Double, val end: Double, val lines: List<String>)` ; `object WebVtt { fun parse(text: String): List<Cue> }` : accepte BOM, `\r\n`, horodatages `HH:MM:SS.mmm` et `MM:SS.mmm`, réglages après l'horodatage ignorés, identifiants de répliques ignorés, balises (`<i>`, `<b>`, `<c.x>`, `<00:01.000>`) retirées, entités `&amp;`, `&lt;`, `&gt;`, `&nbsp;` décodées ; blocs `NOTE`, `STYLE`, `REGION` ignorés ; une réplique mal formée est sautée sans faire échouer les autres ; répliques triées par `start`.
  - `class SubtitleTimeline(cues: List<Cue>)` : `fun activeAt(position: Double, offset: Double, rate: Double): List<Cue>` : une réplique est visible si `rate * start + offset <= position < rate * end + offset` ; recherche dichotomique (le lecteur interroge plusieurs fois par seconde).

- [ ] **Step 1: Tests `WebVttTest`** : un fichier standard de 3 répliques donne 3 `Cue` avec les bons temps (`00:01:02.500` vaut `62.5`) ; `<i>Bonjour</i> &amp; salut` donne `["Bonjour & salut"]` ; réplique sur deux lignes donne deux éléments dans `lines` ; BOM et `\r\n` acceptés ; `""` donne une liste vide ; un texte sans en-tête `WEBVTT` donne une liste vide ; une réplique à l'horodatage invalide est sautée et les autres sont gardées.
- [ ] **Step 2: Tests `SubtitleTimelineTest`** : réplique 10-12 s, `offset = 0`, `rate = 1` : visible à 10.0 et 11.9, pas à 12.0 ; `offset = 1.5` : visible à 11.5, pas à 11.4 ; `rate = 25.0 / 23.976` : début à `10 * rate` ; deux répliques qui se chevauchent sont renvoyées ensemble.
- [ ] **Step 3: Vérifier l'échec, implémenter, vérifier le succès.** Run: `.\gradlew.bat testDebugUnitTest --tests "*subtitles*"`.
- [ ] **Step 4: Commit** : `git commit -m "feat: sous-titres WebVTT et recalage"`.

### Task 12: Contrôleur de lecture

**Files:**
- Create: `app/src/main/java/fr/lumiere/tv/player/PlayerPort.kt`, `PlaybackController.kt`, `StreamIds.kt`, `ExoPlayerPort.kt`
- Test: `app/src/test/java/fr/lumiere/tv/player/PlaybackControllerTest.kt`

**Interfaces:**
- Consumes: `LumiereApi` (Task 4), `ClientCapabilities` (Task 10), DTO de lecture (Task 2).
- Produces:
  - `interface PlayerPort { fun load(url: String, startPositionSec: Double); fun play(); fun pause(); fun seekTo(positionSec: Double); val positionSec: Double; val isPlaying: Boolean; fun setListener(listener: Listener); fun release(); interface Listener { fun onEnded(); fun onError(error: Throwable) } }`.
  - `fun newStreamId(): String` (12 caractères `[A-Za-z0-9]`).
  - `class PlaybackController(api: LumiereApi, player: PlayerPort, capabilities: ClientCapabilities, scope: CoroutineScope, clock: () -> Long)` :
    - `val state: StateFlow<PlayerState>` ; `data class PlayerState(val session: PlaybackSession?, val position: Double, val duration: Double?, val loading: Boolean, val error: String?, val nextCountdown: Int?)`.
    - `suspend fun start(media: MediaRef, audioTrack: Int? = null, mode: String = "auto", at: Double? = null)` : crée la session, charge `absoluteUrl(streamUrl)` ; en `seek == "restart"`, URL `…?start=<at>&stream=<id>` puis origine lue sur `streamOrigin` ; position de départ = `at ?: session.resumePosition`.
    - `val position: Double` : `seek == "native"` donne `player.positionSec` ; `"restart"` donne `origin + player.positionSec`.
    - `suspend fun seekTo(target: Double)` : natif : `player.seekTo` ; `restart` : nouvel identifiant de flux, rechargement à `target`, nouvelle origine.
    - `suspend fun changeAudioTrack(index: Int)` : appelée seulement hors lecture directe (en `direct`, l'écran sélectionne la piste dans `ExoPlayerPort.exoPlayer`, Task 13) ; relance `start(media, audioTrack = index, mode = if (session.mode == "transcode") "transcode" else "auto", at = position)`.
    - `fun tick()` (appelée chaque seconde par l'écran) : envoie `saveProgress` si 10 s se sont écoulées depuis le dernier envoi ; met `nextCountdown` à 10 et le décrémente quand `session.next != null` et `duration - position <= 20` ; à 0, `start(session.next.media)`.
    - Erreur du lecteur en `mode == "direct"`, une seule fois : `start(media, mode = "transcode", at = position)` ; sinon `error` = « Lecture impossible : <message> ».
    - `suspend fun stop()` : `saveProgress` puis `stopSession`, `player.release()` ; les erreurs réseau sont ignorées (on quitte quand même).
  - `class ExoPlayerPort(context: Context, httpClient: OkHttpClient) : PlayerPort` : ExoPlayer avec `OkHttpDataSource.Factory(httpClient)`, rendu des pistes texte d'ExoPlayer désactivé (les sous-titres sont rendus par l'appli), langue audio préférée `fr`. Expose aussi `val exoPlayer` pour la vue et la sélection de pistes en lecture directe.

- [ ] **Step 1: Tests `PlaybackControllerTest`** (faux `PlayerPort`, `MockWebServer` pour l'API, horloge contrôlée) :
  - session `direct` avec `resumePosition = 120` : `load(<base>/api/playback/sessions/s1/stream, 120.0)`.
  - session `transcode` (`seek = "restart"`), `seekTo(600.0)` : nouvelle URL contenant `start=600` et un `stream=` différent du précédent ; origine `598.7` et `player.positionSec = 5` donnent `position == 603.7`.
  - `onError` en `direct` : une nouvelle session est demandée avec `"mode":"transcode"` à la position courante ; un second `onError` n'en redemande pas et renseigne `error`.
  - `tick()` : un seul `POST …/progress` sur 9 s d'horloge, un second à 10 s.
  - `next != null`, `duration = 1500`, `position = 1481` : `nextCountdown == 10` ; 10 ticks plus tard, une session est demandée pour `next.media`.
  - `stop()` : envoie `POST …/progress` puis `DELETE /api/playback/sessions/s1`, et appelle `release()` même si le serveur ne répond plus.
- [ ] **Step 2: Vérifier l'échec, implémenter `PlayerPort`, `StreamIds`, `PlaybackController`, vérifier le succès.** Run: `.\gradlew.bat testDebugUnitTest --tests "*PlaybackControllerTest*"`.
- [ ] **Step 3: Implémenter `ExoPlayerPort`** (vérifier les noms Media3 1.11.1 : `OkHttpDataSource.Factory`, `DefaultMediaSourceFactory`, `TrackSelectionParameters`), compiler : `.\gradlew.bat assembleDebug`.
- [ ] **Step 4: Commit** : `git commit -m "feat: contrôleur de lecture"`.

### Task 13: Écran du lecteur

**Files:**
- Create: `ui/player/PlayerScreen.kt`, `ui/player/PlayerViewModel.kt`, `ui/player/TracksPanel.kt`, `ui/player/SubtitleOverlay.kt`, `ui/player/SubtitlesViewModel.kt`
- Test: `app/src/test/java/fr/lumiere/tv/ui/player/SubtitlesViewModelTest.kt`

**Interfaces:**
- Consumes: `PlaybackController`, `ExoPlayerPort` (Task 12), `WebVtt`, `SubtitleTimeline` (Task 11), `detectCapabilities` (Task 10), `LumiereApi` sous-titres et recalage (Task 4).
- Produces:
  - `class SubtitlesViewModel(api: LumiereApi)` : `fun attach(session: PlaybackSession)` (choisit `defaultSubtitleTrack`, lance la recherche en ligne d'office si `onlineSubtitles.autoLanguage != null` et prend le premier résultat dans cette langue) ; `fun select(trackId: String?)` (charge le VTT et l'état de recalage) ; `fun nudge(deltaSec: Double)` (offset ± 0,25 s, `PUT …/sync`) ; `fun autoSync()` (affiche `SubtitleSyncResult.message`) ; `fun searchOnline()` ; `fun cuesAt(position: Double): List<Cue>`. Un VTT illisible donne la piste « indisponible » sans planter.
  - `@Composable fun PlayerScreen(media: MediaRef, titleId: Int?, onExit: () -> Unit)`.

- [ ] **Step 1: Tests `SubtitlesViewModelTest`** (MockWebServer) : `select("2")` charge `/subtitles/2` et `/subtitles/2/sync` puis `cuesAt` applique l'offset reçu ; `nudge(0.25)` deux fois envoie un `PUT` avec `offset = 0.5` ; un VTT vide donne `cuesAt(...) == emptyList()` et la piste marquée indisponible ; `autoLanguage = "fr"` déclenche `GET …/subtitles/online` et sélectionne la première piste `language == "fr"`.
- [ ] **Step 2: Vérifier l'échec, implémenter `SubtitlesViewModel`, vérifier le succès.** Run: `.\gradlew.bat testDebugUnitTest --tests "*SubtitlesViewModelTest*"`.
- [ ] **Step 3: Écran.** Vidéo plein écran (`PlayerView` de Media3 via `AndroidView`, commandes natives désactivées) ; `SubtitleOverlay` (texte blanc à contour noir, grande taille, bas de l'écran dans la zone de sécurité) ; touches : OK = pause/lecture et affichage des commandes (titre, sous-titre, barre de temps, se masquent après 4 s), gauche/droite = `seekTo(position ∓ 10)`, haut = `TracksPanel` (audio : pistes ExoPlayer en `direct`, `session.audioTracks` sinon ; sous-titres : Aucun, pistes de la session, « Chercher en ligne » si `onlineSubtitles.available` ; décalage − / + 0,25 s ; « Recaler automatiquement »), Retour = `controller.stop()` puis `onExit()`. Compte à rebours « Épisode suivant dans N s » avec bouton Annuler. Appel de `tick()` chaque seconde. Erreur : message et retour à la fiche. Écran maintenu allumé pendant la lecture.
- [ ] **Step 4: Vérifier sur le stick** avec la démo : MKV HEVC en lecture directe (`session.mode == "direct"`, à journaliser), changement de piste audio sur un MKV multi-pistes, sous-titres intégrés et externes, décalage, reprise après sortie et retour, épisode suivant, un fichier forcé en conversion (DTS si la démo en a, sinon `mode = "transcode"`) avec avance rapide.
- [ ] **Step 5: Commit** : `git commit -m "feat: lecteur"`.

### Task 14: Finitions, APK de distribution et documentation

**Files:**
- Modify: `app/build.gradle.kts` (signature de la version de distribution), `CLAUDE.md`
- Create: `README.md`

**Interfaces:**
- Consumes: toute l'appli.

- [ ] **Step 1: Parcours complet au pavé directionnel** sur le stick : chaque écran a un focus initial, aucun piège, Retour cohérent (section vers Accueil, Accueil vers sortie, lecteur vers fiche). Corriger ce qui bloque.
- [ ] **Step 2: Essai par Internet** : avec l'utilisateur, Lumière lancé avec `HOST=0.0.0.0`, port redirigé sur la box ; dans l'appli, saisir l'adresse publique et vérifier connexion, affiches et lecture d'un film. Noter le débit constaté.
- [ ] **Step 3: APK signé** pour l'installation hors réseau local : clé de signature créée dans `%USERPROFILE%\.android\lumiere-tv.jks` (hors dépôt), mots de passe lus dans `local.properties` (non versionné), `.\gradlew.bat assembleRelease`. Expected: `app/build/outputs/apk/release/app-release.apk`.
- [ ] **Step 4: `README.md`** (en français) : ce qu'est l'appli, prérequis côté PC (`HOST=0.0.0.0`, redirection de port, pare-feu Windows, DynDNS si l'IP change, avertissement « exposé sans mot de passe »), installation par `adb` sur le réseau local et par APK copié (par exemple avec une appli de transfert de fichiers sur le stick) ailleurs, première configuration, commandes de développement.
- [ ] **Step 5: Mettre à jour `CLAUDE.md`** (commandes réelles, version d'Android du stick, capacités constatées, écarts éventuels par rapport à la spec).
- [ ] **Step 6: Lancer toute la suite.** Run: `.\gradlew.bat testDebugUnitTest assembleDebug`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 7: Commit** : `git commit -m "docs: installation et distribution de l'appli TV"`.
