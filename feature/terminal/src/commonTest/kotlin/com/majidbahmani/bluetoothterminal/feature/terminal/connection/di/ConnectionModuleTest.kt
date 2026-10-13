package com.majidbahmani.bluetoothterminal.feature.terminal.connection.di

import com.majidbahmani.bluetooth.BluetoothSession
import com.majidbahmani.bluetooth.testing.FakeBluetoothClient
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.data.repository.ConnectionRepositoryImpl
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ConnectionRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.usecase.ObserveConnectionUseCase
import kotlinx.coroutines.test.TestScope
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class ConnectionModuleTest {

    // The session normally comes from :app; a local KoinApplication never touches global Koin (doc 33).
    private val app = koinApplication {
        modules(
            module { single { BluetoothSession(FakeBluetoothClient(), TestScope()) } },
            connectionModule,
        )
    }

    @AfterTest
    fun tearDown() = app.close()

    @Test
    fun repository_isSingletonImplementation() {
        val repository = app.koin.get<ConnectionRepository>()

        assertIs<ConnectionRepositoryImpl>(repository)
        assertSame(repository, app.koin.get<ConnectionRepository>())
    }

    @Test
    fun observeConnectionUseCase_resolves() {
        assertNotNull(app.koin.get<ObserveConnectionUseCase>())
    }
}
