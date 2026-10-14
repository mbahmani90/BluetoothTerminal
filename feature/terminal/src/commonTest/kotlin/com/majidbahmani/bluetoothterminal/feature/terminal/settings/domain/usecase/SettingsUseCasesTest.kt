package com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelException
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelSelection
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceCharacteristic
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceService
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.fake.FakeChannelRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.model.CharacteristicSettings
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.model.ServiceSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SettingsUseCasesTest {

    private val repository = FakeChannelRepository(ChannelSelection(sendTarget = RX.ref, listening = setOf(TX.ref))).apply {
        services.value = listOf(DeviceService(NUS, listOf(RX, TX, VERSION)))
    }

    @Test
    fun observe_combinesServicesWithSelection() = runTest {
        val settings = ObserveServiceSettingsUseCase(repository)().first()

        assertEquals(
            listOf(
                ServiceSettings(
                    uuid = NUS,
                    characteristics = listOf(
                        CharacteristicSettings(RX, isSendTarget = true, isListening = false),
                        CharacteristicSettings(TX, isSendTarget = false, isListening = true),
                        CharacteristicSettings(VERSION, isSendTarget = false, isListening = false),
                    ),
                ),
            ),
            settings,
        )
    }

    @Test
    fun observe_notConnected_isEmpty() = runTest {
        repository.services.value = emptyList()

        assertEquals(emptyList(), ObserveServiceSettingsUseCase(repository)().first())
    }

    @Test
    fun setSendTarget_changesSelection() {
        SetSendTargetUseCase(repository)(VERSION.ref)

        assertEquals(VERSION.ref, repository.selection.value.sendTarget)
    }

    @Test
    fun setListening_addsAndRemoves() {
        SetListeningUseCase(repository)(VERSION.ref, listening = true)
        SetListeningUseCase(repository)(TX.ref, listening = false)

        assertEquals(setOf(VERSION.ref), repository.selection.value.listening)
    }

    @Test
    fun read_returnsValue() = runTest {
        repository.readValues[VERSION.ref] = "1.0".encodeToByteArray()

        assertContentEquals("1.0".encodeToByteArray(), ReadCharacteristicUseCase(repository)(VERSION.ref).getOrThrow())
    }

    @Test
    fun read_fails_returnsTheError() = runTest {
        repository.error = ChannelException(ChannelError.NotSupported)

        val error = ReadCharacteristicUseCase(repository)(RX.ref).exceptionOrNull()

        assertEquals(ChannelError.NotSupported, (error as? ChannelException)?.error)
    }

    private companion object {
        const val NUS = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
        val RX = characteristic("6e400002-b5a3-f393-e0a9-e50e24dcca9e", canWrite = true)
        val TX = characteristic("6e400003-b5a3-f393-e0a9-e50e24dcca9e", canNotify = true)
        val VERSION = characteristic("6e400004-b5a3-f393-e0a9-e50e24dcca9e", canRead = true)

        fun characteristic(uuid: String, canRead: Boolean = false, canWrite: Boolean = false, canNotify: Boolean = false) =
            DeviceCharacteristic(CharacteristicRef(NUS, uuid), canRead = canRead, canWrite = canWrite, canNotify = canNotify)
    }
}
