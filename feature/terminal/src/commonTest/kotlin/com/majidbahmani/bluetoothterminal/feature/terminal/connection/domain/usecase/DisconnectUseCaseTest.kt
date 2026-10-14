package com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceInfo
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.fake.FakeConnectionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DisconnectUseCaseTest {

    @Test
    fun invoke_disconnects() = runTest {
        val repository = FakeConnectionRepository(ConnectionStatus.Connected(DeviceInfo(id = "1", name = "ESP32-NUS")))

        DisconnectUseCase(repository)()

        assertEquals(1, repository.disconnectCalls)
        assertEquals(ConnectionStatus.Disconnected, repository.status.value)
    }
}
