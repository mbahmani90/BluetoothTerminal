package com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ChannelRepository

/** Reads [ref]'s current value once. Fails with a `ChannelException` (not connected, not readable, …). */
internal class ReadCharacteristicUseCase(private val repository: ChannelRepository) {

    suspend operator fun invoke(ref: CharacteristicRef): Result<ByteArray> = repository.read(ref)
}
