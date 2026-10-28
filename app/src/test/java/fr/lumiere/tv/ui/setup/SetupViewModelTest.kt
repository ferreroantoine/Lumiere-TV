package fr.lumiere.tv.ui.setup

import fr.lumiere.tv.data.prefs.ServerStore
import fr.lumiere.tv.data.server.CheckResult
import fr.lumiere.tv.data.server.ServerAddress
import fr.lumiere.tv.data.server.message
import fr.lumiere.tv.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetupViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private class MemoryStore : ServerStore {
        override val address = MutableStateFlow<ServerAddress?>(null)
        override suspend fun save(address: ServerAddress) { this.address.value = address }
        override suspend fun clear() { address.value = null }
    }

    private val store = MemoryStore()
    private var checked = mutableListOf<ServerAddress>()

    private fun viewModel(result: CheckResult) = SetupViewModel(
        check = { address -> checked += address; result },
        store = store,
    )

    @Test
    fun adresseVideRefuseeSansTest() = runTest {
        val vm = viewModel(CheckResult.Ok("1.0.0"))
        vm.submit("")
        advanceUntilIdle()
        assertEquals(SetupState.Failed("Saisissez l'adresse du serveur."), vm.state.value)
        assertTrue(checked.isEmpty())
    }

    @Test
    fun adresseValideTesteePuisEnregistree() = runTest {
        val vm = viewModel(CheckResult.Ok("1.0.0"))
        val states = mutableListOf<SetupState>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.toList(states) }
        vm.submit("1.2.3.4")
        advanceUntilIdle()
        job.cancel()
        assertTrue(states.contains(SetupState.Checking))
        assertEquals(SetupState.Connected("1.0.0"), vm.state.value)
        assertEquals(ServerAddress("http://1.2.3.4:8484"), store.address.value)
    }

    @Test
    fun serveurQuiNeRepondPasNEstPasEnregistre() = runTest {
        val vm = viewModel(CheckResult.Timeout)
        vm.submit("1.2.3.4")
        advanceUntilIdle()
        assertEquals(SetupState.Failed(CheckResult.Timeout.message()), vm.state.value)
        assertNull(store.address.value)
    }
}
