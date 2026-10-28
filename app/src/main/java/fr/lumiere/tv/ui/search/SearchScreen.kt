package fr.lumiere.tv.ui.search

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.lumiere.tv.data.api.TitleSummary
import fr.lumiere.tv.ui.components.LumiereButton
import fr.lumiere.tv.ui.home.metaLine
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereIcons
import fr.lumiere.tv.ui.UiState
import fr.lumiere.tv.ui.appContainer
import fr.lumiere.tv.ui.components.EmptyMessage
import fr.lumiere.tv.ui.components.ErrorScreen
import fr.lumiere.tv.ui.components.LoadingScreen
import fr.lumiere.tv.ui.components.PosterGrid
import fr.lumiere.tv.ui.components.TvTextField
import fr.lumiere.tv.ui.containerViewModel
import fr.lumiere.tv.ui.navigation.Navigator

/** Recherche dans tout le catalogue, au clavier ou à la voix (micro de la télécommande). */
@Composable
fun SearchScreen(navigator: Navigator) {
    val vm = containerViewModel { SearchViewModel(it.api) }
    val api = appContainer().api
    val state by vm.state.collectAsStateWithLifecycle()
    val searched by vm.searched.collectAsStateWithLifecycle()
    var text by rememberSaveable { mutableStateOf("") }
    var focused by remember { mutableStateOf<TitleSummary?>(null) }
    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fieldFocus.requestFocus() } }

    val context = LocalContext.current
    val voiceIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Que voulez-vous regarder ?")
    }
    val voiceAvailable = remember { voiceIntent.resolveActivity(context.packageManager) != null }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let {
            text = it
            vm.setQuery(it)
        }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Rechercher", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TvTextField(
                value = text,
                onValueChange = { text = it; vm.setQuery(it) },
                placeholder = "Rechercher un film, une série…",
                imeAction = ImeAction.Search,
                leadingIcon = LumiereIcons.Search,
                modifier = Modifier.weight(1f).focusRequester(fieldFocus),
            )
            if (voiceAvailable) {
                LumiereButton("Recherche vocale", onClick = { voice.launch(voiceIntent) }, icon = LumiereIcons.AudioLines)
            }
        }
        when (val s = state) {
            UiState.Loading -> LoadingScreen()
            is UiState.Error -> ErrorScreen(s.message, onRetry = vm::retry)
            is UiState.Ready -> when {
                s.value.isNotEmpty() -> {
                    Text(
                        focused?.let { "${it.title}  ·  ${it.metaLine()}" } ?: "${s.value.size} résultat(s)",
                        style = MaterialTheme.typography.titleMedium,
                        color = LumiereColors.Zinc200,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                    PosterGrid(
                        titles = s.value,
                        imageUrl = api::absoluteUrl,
                        onOpen = { navigator.openTitle(it.id) },
                        onFocus = {
                            focused = it
                            navigator.setAmbient(it.posterUrl?.let(api::absoluteUrl))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                searched.isNotEmpty() -> EmptyMessage("Aucun résultat pour « $searched ».")
                else -> EmptyMessage("Saisissez au moins deux lettres, ou utilisez la recherche vocale.")
            }
        }
    }
}
