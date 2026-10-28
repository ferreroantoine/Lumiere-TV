package fr.lumiere.tv.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import fr.lumiere.tv.data.api.MediaRef
import fr.lumiere.tv.data.api.TitleKind
import fr.lumiere.tv.data.server.CheckResult
import fr.lumiere.tv.data.server.message
import fr.lumiere.tv.ui.appContainer
import fr.lumiere.tv.ui.catalog.CatalogScreen
import fr.lumiere.tv.ui.components.AmbientBackground
import fr.lumiere.tv.ui.components.EmptyMessage
import fr.lumiere.tv.ui.components.ErrorScreen
import fr.lumiere.tv.ui.components.LoadingScreen
import fr.lumiere.tv.ui.components.LocalSidebarFocus
import fr.lumiere.tv.ui.home.HomeScreen
import fr.lumiere.tv.ui.library.LibraryScreen
import fr.lumiere.tv.ui.player.PlayerScreen
import fr.lumiere.tv.ui.search.SearchScreen
import fr.lumiere.tv.ui.settings.SettingsScreen
import fr.lumiere.tv.ui.setup.SetupScreen
import fr.lumiere.tv.ui.theme.Safe
import fr.lumiere.tv.ui.title.TitleScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface Startup {
    data object Loading : Startup
    data object NeedsSetup : Startup
    data class Unreachable(val message: String) : Startup
    data object Ready : Startup
}

/** Point d'entrée de l'interface : vérifie le serveur retenu, puis ouvre la navigation. */
@Composable
fun AppShell() {
    val container = appContainer()
    var startup by remember { mutableStateOf<Startup>(Startup.Loading) }
    var attempt by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(attempt) {
        startup = Startup.Loading
        val address = container.loadAddress()
        startup = if (address == null) {
            Startup.NeedsSetup
        } else {
            when (val result = container.serverCheck.check(address)) {
                is CheckResult.Ok -> Startup.Ready
                else -> Startup.Unreachable("Lumière est injoignable. ${result.message()}")
            }
        }
    }

    when (val s = startup) {
        Startup.Loading -> Box {
            AmbientBackground(null)
            LoadingScreen()
        }
        is Startup.Unreachable -> Box {
            AmbientBackground(null)
            ErrorScreen(
                message = s.message,
                onRetry = { attempt++ },
                onChangeServer = { startup = Startup.NeedsSetup },
            )
        }
        Startup.NeedsSetup -> AppNavHost(start = Route.Setup)
        Startup.Ready -> AppNavHost(start = Route.Main)
    }
}

@Composable
private fun AppNavHost(start: Route) {
    val nav = rememberNavController()
    val container = appContainer()
    NavHost(navController = nav, startDestination = start) {
        composable<Route.Setup> {
            SetupScreen(initialAddress = container.currentAddress.value?.baseUrl.orEmpty()) {
                nav.navigate(Route.Main) { popUpTo(0) { inclusive = true } }
            }
        }
        composable<Route.Main> { MainScreen(nav) }
        composable<Route.Title> { entry ->
            val route = entry.toRoute<Route.Title>()
            val navigator = remember(nav) { Navigator(nav) }
            key(route.id) { TitleScreen(route.id, route.autoPlay, navigator) }
        }
        composable<Route.Player> { entry ->
            val route = entry.toRoute<Route.Player>()
            PlayerScreen(route.media, onExit = { nav.popBackStack() })
        }
    }
}

/** Actions de navigation offertes aux sections. */
class Navigator(
    private val nav: NavHostController,
    private val ambient: (String?) -> Unit = {},
    private val backdrop: (String?) -> Unit = {},
) {
    fun openTitle(id: Int) = nav.navigate(Route.Title(id))

    /** Ouvre la fiche et lance aussitôt la lecture (version préférée ou reprise). */
    fun playTitle(id: Int) = nav.navigate(Route.Title(id, autoPlay = true))
    fun play(media: MediaRef, titleId: Int? = null) = nav.navigate(Route.Player.of(media, titleId))
    fun changeServer() = nav.navigate(Route.Setup) { popUpTo(0) { inclusive = true } }

    /** Image dont la couleur teinte le fond d'ambiance. */
    fun setAmbient(imageUrl: String?) = ambient(imageUrl)

    /** Illustration plein cadre derrière l'Accueil (`null` : aucune). */
    fun setBackdrop(imageUrl: String?) = backdrop(imageUrl)
}

/** Écart entre le bord de l'écran et la barre latérale (ses icônes restent dans la zone de sécurité). */
private val SidebarInset = 28.dp

/** Temps laissé à la section choisie pour se composer avant d'y donner le focus. */
private const val SECTION_FOCUS_DELAY_MS = 80L

@Composable
private fun MainScreen(nav: NavHostController) {
    var section by rememberSaveable { mutableStateOf(Section.Home) }
    var ambient by rememberSaveable { mutableStateOf<String?>(null) }
    var backdrop by rememberSaveable { mutableStateOf<String?>(null) }
    val navigator = remember(nav) { Navigator(nav, ambient = { ambient = it }, backdrop = { backdrop = it }) }

    // Retour : d'une section vers l'Accueil ; depuis l'Accueil, l'appli se ferme.
    BackHandler(enabled = section != Section.Home) { section = Section.Home }

    val itemFocus = remember { Section.entries.associateWith { FocusRequester() } }
    val contentFocus = remember { FocusRequester() }
    var sidebarFocused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        AmbientBackground(ambient, backdropUrl = backdrop.takeIf { section == Section.Home })
        Box(
            Modifier.fillMaxSize()
                .padding(start = SidebarInset + SidebarSize.Collapsed + 28.dp, end = Safe.Horizontal, top = Safe.Vertical)
                .focusRequester(contentFocus)
                // Revenir du menu ramène le focus sur l'élément quitté.
                .focusRestorer()
                .focusGroup(),
        ) {
            CompositionLocalProvider(LocalSidebarFocus provides itemFocus.getValue(section)) {
                SectionContent(section, navigator)
            }
        }
        // Menu ouvert : voile sombre à gauche, le menu se détache du contenu.
        AnimatedVisibility(sidebarFocused, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(0f to Color.Black.copy(alpha = 0.78f), 0.5f to Color.Transparent),
                ),
            )
        }
        Sidebar(
            section = section,
            expanded = sidebarFocused,
            itemFocus = itemFocus,
            onSelect = {
                section = it
                // La section choisie s'ouvre et prend le focus (le menu se referme).
                scope.launch {
                    delay(SECTION_FOCUS_DELAY_MS)
                    runCatching { contentFocus.requestFocus() }
                }
            },
            modifier = Modifier.padding(start = SidebarInset, top = 18.dp, bottom = 18.dp)
                .onFocusChanged { sidebarFocused = it.hasFocus }
                // Le menu ouvert recouvre le contenu : Droite y retourne directement.
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.DirectionRight && event.type == KeyEventType.KeyDown) {
                        runCatching { contentFocus.requestFocus() }
                        true
                    } else {
                        false
                    }
                },
        )
    }
}

@Composable
private fun SectionContent(section: Section, navigator: Navigator) {
    when (section) {
        Section.Home -> HomeScreen(navigator)
        Section.Movies -> CatalogScreen(TitleKind.MOVIE, navigator)
        Section.Series -> CatalogScreen(TitleKind.SERIES, navigator)
        Section.Search -> SearchScreen(navigator)
        Section.Library -> LibraryScreen(navigator)
        Section.Settings -> SettingsScreen(onChangeServer = navigator::changeServer)
        else -> EmptyMessage("${section.label} : bientôt disponible")
    }
}
