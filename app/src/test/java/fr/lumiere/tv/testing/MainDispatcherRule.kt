package fr.lumiere.tv.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Remplace `Dispatchers.Main` (utilisé par `viewModelScope`) pendant les tests.
 *
 * Volontairement sans `resetMain()` : une requête OkHttp lancée par le ViewModel d'un test
 * précédent peut se terminer plus tard et reprendre sur `Main` ; elle tombe alors sur un ancien
 * répartiteur de test inactif au lieu de faire échouer le test en cours.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
}
