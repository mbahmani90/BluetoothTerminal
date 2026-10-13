package com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import kotlinx.coroutines.flow.Flow

/** The active connection, shared by the Devices and Terminal screens. Connecting lives in `devices`. */
internal interface ConnectionRepository {

    /** Current status first, then every change. */
    val status: Flow<ConnectionStatus>

    suspend fun disconnect()
}
