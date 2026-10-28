package fr.lumiere.tv.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.tv.material3.Icon
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import fr.lumiere.tv.data.api.MediaRef
import fr.lumiere.tv.data.server.CheckResult
import fr.lumiere.tv.data.server.message
import fr.lumiere.tv.ui.appContainer
import fr.lumiere.tv.ui.components.EmptyMessage
import fr.lumiere.tv.ui.components.ErrorScreen
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import fr.lumiere.tv.ui.components.AmbientBackground
import fr.lumiere.tv.ui.components.LoadingScreen
import fr.lumiere.tv.ui.home.HomeScreen
import fr.lumiere.tv.ui.catalog.CatalogScreen
import fr.lumiere.tv.ui.search.SearchScreen
import fr.lumiere.tv.data.api.TitleKind
import fr.lumiere.tv.ui.theme.LumiereColors
import androidx.tv.material3.NavigationDrawerItemDefaults
import fr.lumiere.tv.ui.settings.SettingsScreen
import fr.lumiere.tv.ui.setup.SetupScreen
import fr.lumiere.tv.ui.theme.Safe

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
    var startup by androidx.compose.runtime.remember { mutableStateOf<Startup>(Startup.Loading) }
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
        Startup.Loading -> LoadingScreen()
        is Startup.Unreachable -> ErrorScreen(
            message = s.message,
            onRetry = { attempt++ },
            onChangeServer = { startup = Startup.NeedsSetup },
        )
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
            key(route.id) { EmptyMessage("Fiche ${route.id} : bientôt disponible") }
        }
        composable<Route.Player> { entry ->
            val route = entry.toRoute<Route.Player>()
            EmptyMessage("Lecture de ${route.media} : bientôt disponible")
        }
    }
}

/** Actions de navigation offertes aux sections. */
class Navigator(private val nav: NavHostController, private val ambient: (String?) -> Unit = {}) {
    fun openTitle(id: Int) = nav.navigate(Route.Title(id))

    /** Ouvre la fiche et lance aussitôt la lecture (version préférée ou reprise). */
    fun playTitle(id: Int) = nav.navigate(Route.Title(id, autoPlay = true))
    fun play(media: MediaRef, titleId: Int? = null) = nav.navigate(Route.Player.of(media, titleId))
    fun changeServer() = nav.navigate(Route.Setup) { popUpTo(0) { inclusive = true } }

    /** Image dont la couleur teinte le fond d'ambiance. */
    fun setAmbient(imageUrl: String?) = ambient(imageUrl)
}

private fun Section.icon(): ImageVector = when (this) {
    Section.Search -> Icons.Default.Search
    Section.Home -> Icons.Default.Home
    Section.Movies -> Icons.Default.Star
    Section.Series -> Icons.Default.List
    Section.Library -> Icons.Default.CheckCircle
    Section.Settings -> Icons.Default.Settings
}

@Composable
private fun MainScreen(nav: NavHostController) {
    var section by rememberSaveable { mutableStateOf(Section.Home) }
    var ambient by rememberSaveable { mutableStateOf<String?>(null) }
    val navigator = androidx.compose.runtime.remember(nav) { Navigator(nav) { ambient = it } }

    // Retour : d'une section vers l'Accueil ; depuis l'Accueil, l'appli se ferme.
    BackHandler(enabled = section != Section.Home) { section = Section.Home }

    // En entrant dans le menu, le focus va sur la section affichée.
    val itemFocus = androidx.compose.runtime.remember { Section.entries.associateWith { FocusRequester() } }

    AmbientBackground(ambient)
    NavigationDrawer(
        drawerContent = { _ ->
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                modifier = Modifier.fillMaxHeight().padding(vertical = Safe.Vertical, horizontal = 12.dp)
                    .focusProperties { onEnter = { itemFocus.getValue(section).requestFocus() } }
                    .focusGroup()
                    .selectableGroup(),
            ) {
                Section.entries.forEach { item ->
                    NavigationDrawerItem(
                        selected = section == item,
                        onClick = { section = item },
                        modifier = Modifier.focusRequester(itemFocus.getValue(item)),
                        leadingContent = { Icon(item.icon(), contentDescription = null) },
                        colors = NavigationDrawerItemDefaults.colors(
                            focusedContainerColor = LumiereColors.Brand400,
                            focusedContentColor = LumiereColors.Ink950,
                            selectedContainerColor = LumiereColors.Ink700,
                            selectedContentColor = LumiereColors.Brand200,
                        ),
                    ) { Text(item.label) }
                }
            }
        },
    ) {
        Box(Modifier.fillMaxSize().padding(start = 24.dp, end = Safe.Horizontal, top = Safe.Vertical, bottom = Safe.Vertical)) {
            SectionContent(section, navigator)
        }
    }
}

@Composable
private fun SectionContent(section: Section, navigator: Navigator) {
    when (section) {
        Section.Home -> HomeScreen(navigator)
        Section.Movies -> CatalogScreen(TitleKind.MOVIE, navigator)
        Section.Series -> CatalogScreen(TitleKind.SERIES, navigator)
        Section.Search -> SearchScreen(navigator)
        Section.Settings -> SettingsScreen(onChangeServer = navigator::changeServer)
        else -> EmptyMessage("${section.label} : bientôt disponible")
    }
}
