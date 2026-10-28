package fr.lumiere.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.lumiere.tv.AppContainer
import fr.lumiere.tv.LumiereApp

/** Conteneur de l'appli depuis un écran. */
@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as LumiereApp).container

/** ViewModel construit à partir du conteneur ; `key` distingue plusieurs instances (fiche n° 3, n° 4…). */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    key: String? = null,
    noinline create: (AppContainer) -> VM,
): VM {
    val container = appContainer()
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}
