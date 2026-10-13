package com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ConnectionRepository

/** Ends the connection on purpose (Disconnect, or leaving the Terminal): the status becomes Disconnected, not Lost. */
internal class DisconnectUseCase(private val repository: ConnectionRepository) {

    suspend operator fun invoke() {
        repository.disconnect()
    }
}
