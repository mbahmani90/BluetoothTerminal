package com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceInfo
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveConnectionUseCaseTest {

    @Test
    fun invoke_repeatedStatus_isEmittedOnce() = runTest {
        val device = DeviceInfo(id = "1", name = "HM-10")
        val repository = FakeConnectionRepository(
            ConnectionStatus.Disconnected,
            ConnectionStatus.Connecting(device),
            ConnectionStatus.Connecting(device),
            ConnectionStatus.Connected(device),
            ConnectionStatus.Connected(device),
        )

        val statuses = ObserveConnectionUseCase(repository)().toList()

        assertEquals(
            listOf(ConnectionStatus.Disconnected, ConnectionStatus.Connecting(device), ConnectionStatus.Connected(device)),
            statuses,
        )
    }

    private class FakeConnectionRepository(vararg statuses: ConnectionStatus) : ConnectionRepository {
        override val status: Flow<ConnectionStatus> = flowOf(*statuses)
        override suspend fun disconnect() = Unit
    }
}
