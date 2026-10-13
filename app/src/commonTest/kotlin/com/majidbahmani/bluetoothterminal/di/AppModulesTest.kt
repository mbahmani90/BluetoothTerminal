package com.majidbahmani.bluetoothterminal.di

import com.majidbahmani.bluetooth.BluetoothClient
import com.majidbahmani.bluetooth.BluetoothSession
import com.majidbahmani.bluetooth.testing.FakeBluetoothClient
import kotlinx.coroutines.CoroutineScope
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/** Koin errors only show at runtime, so the graph is tested (doc 33). The platform client is faked. */
class AppModulesTest {

    private val app = koinApplication {
        modules(listOf(module { single<BluetoothClient> { FakeBluetoothClient() } }) + appModules.filter { it != platformModule })
    }

    @AfterTest
    fun tearDown() = app.close()

    @Test
    fun session_isSingletonUsingTheAppScope() {
        assertNotNull(app.koin.get<CoroutineScope>(named(APP_SCOPE)))
        assertSame(app.koin.get<BluetoothSession>(), app.koin.get<BluetoothSession>())
    }

    @Test
    fun featureModule_resolvesWithAppBindings() {
        // terminalFeatureModule's own test checks each use case; here: it loads with :app's real bindings.
        assertNotNull(app.koin.get<BluetoothSession>())
        assertNotNull(app.koin.get<BluetoothClient>())
    }
}
