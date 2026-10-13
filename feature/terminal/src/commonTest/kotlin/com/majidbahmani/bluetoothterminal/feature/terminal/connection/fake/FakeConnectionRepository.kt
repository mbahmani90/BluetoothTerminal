package com.majidbahmani.bluetoothterminal.feature.terminal.connection.fake

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** [ConnectionRepository] whose status the test sets directly. */
internal class FakeConnectionRepository(
    initial: ConnectionStatus = ConnectionStatus.Disconnected,
) : ConnectionRepository {

    override val status = MutableStateFlow(initial)

    var disconnectCalls = 0
        private set

    override suspend fun disconnect() {
        disconnectCalls++
        status.value = ConnectionStatus.Disconnected
    }
}
