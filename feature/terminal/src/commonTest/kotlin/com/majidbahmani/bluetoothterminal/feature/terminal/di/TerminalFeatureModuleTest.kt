package com.majidbahmani.bluetoothterminal.feature.terminal.di

import com.majidbahmani.bluetooth.BluetoothClient
import com.majidbahmani.bluetooth.BluetoothSession
import com.majidbahmani.bluetooth.testing.FakeBluetoothClient
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.usecase.ObserveConnectionUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.usecase.ConnectToDeviceUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.usecase.ScanDevicesUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.ObserveServiceSettingsUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.ReadCharacteristicUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.SetListeningUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.SetSendTargetUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.usecase.ObserveMessagesUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.test.TestScope
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class TerminalFeatureModuleTest {

    // What :app provides, then the feature's one public module.
    private val app = koinApplication {
        modules(
            module {
                single<BluetoothClient> { FakeBluetoothClient() }
                single { BluetoothSession(client = get(), scope = TestScope()) }
            },
            terminalFeatureModule,
        )
    }

    @AfterTest
    fun tearDown() = app.close()

    @Test
    fun aggregator_resolvesEveryUseCase() {
        assertNotNull(app.koin.get<ObserveConnectionUseCase>())
        assertNotNull(app.koin.get<ScanDevicesUseCase>())
        assertNotNull(app.koin.get<ConnectToDeviceUseCase>())
        assertNotNull(app.koin.get<ObserveMessagesUseCase>())
        assertNotNull(app.koin.get<SendMessageUseCase>())
        assertNotNull(app.koin.get<ObserveServiceSettingsUseCase>())
        assertNotNull(app.koin.get<SetSendTargetUseCase>())
        assertNotNull(app.koin.get<SetListeningUseCase>())
        assertNotNull(app.koin.get<ReadCharacteristicUseCase>())
    }
}
