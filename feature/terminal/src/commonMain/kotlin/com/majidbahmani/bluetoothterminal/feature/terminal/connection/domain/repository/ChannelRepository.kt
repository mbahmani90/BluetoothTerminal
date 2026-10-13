package com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelSelection
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceService
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.IncomingData
import kotlinx.coroutines.flow.Flow

/**
 * The connected device's characteristics and which of them the terminal uses, shared by the Terminal and
 * Characteristics screens.
 *
 * The selection starts from a detected default (a known serial profile, else a writable + notifying pair)
 * on every new connection and is kept in memory only.
 */
internal interface ChannelRepository {

    /** Services of the connected device; empty while not connected. */
    val services: Flow<List<DeviceService>>

    /** Current selection first, then every change; empty while not connected. */
    val selection: Flow<ChannelSelection>

    /** Data from every characteristic in [selection]'s `listening`, while connected. */
    val incoming: Flow<IncomingData>

    /** Sends to [ref] from now on; null sends nowhere. Ignored if [ref] can't be written. */
    fun setSendTarget(ref: CharacteristicRef?)

    /** Starts or stops listening to [ref]. Ignored if [ref] can't notify. */
    fun setListening(ref: CharacteristicRef, listening: Boolean)

    /** Sends [bytes] to the send target, split into as many writes as needed. Fails with `ChannelException`. */
    suspend fun send(bytes: ByteArray): Result<Unit>

    /** Reads [ref]'s current value. Fails with `ChannelException`. */
    suspend fun read(ref: CharacteristicRef): Result<ByteArray>
}
