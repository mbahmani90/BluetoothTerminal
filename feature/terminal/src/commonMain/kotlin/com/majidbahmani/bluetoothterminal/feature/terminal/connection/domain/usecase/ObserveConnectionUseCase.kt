package com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The connection status for a screen (e.g. "Connected to HM-10", Send enabled or not).
 *
 * Repeated equal statuses are skipped, so screens only update on a real change.
 */
internal class ObserveConnectionUseCase(private val repository: ConnectionRepository) {

    operator fun invoke(): Flow<ConnectionStatus> = repository.status.distinctUntilChanged()
}
