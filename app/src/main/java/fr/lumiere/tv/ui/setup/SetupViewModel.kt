package fr.lumiere.tv.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.lumiere.tv.data.prefs.ServerStore
import fr.lumiere.tv.data.server.CheckResult
import fr.lumiere.tv.data.server.ParseResult
import fr.lumiere.tv.data.server.ServerAddress
import fr.lumiere.tv.data.server.message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface SetupState {
    data object Idle : SetupState
    data object Checking : SetupState
    data class Failed(val message: String) : SetupState
    data class Connected(val version: String) : SetupState
}

/** Saisie de l'adresse du serveur : normalisation, test de connexion, puis mémorisation. */
class SetupViewModel(
    private val check: suspend (ServerAddress) -> CheckResult,
    private val store: ServerStore,
) : ViewModel() {
    private val _state = MutableStateFlow<SetupState>(SetupState.Idle)
    val state: StateFlow<SetupState> = _state

    fun submit(input: String) {
        val address = when (val parsed = ServerAddress.parse(input)) {
            is ParseResult.Invalid -> {
                _state.value = SetupState.Failed(parsed.message)
                return
            }
            is ParseResult.Ok -> parsed.address
        }
        _state.value = SetupState.Checking
        viewModelScope.launch {
            when (val result = check(address)) {
                is CheckResult.Ok -> {
                    store.save(address)
                    _state.value = SetupState.Connected(result.version)
                }
                else -> _state.value = SetupState.Failed(result.message())
            }
        }
    }
}
