package com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ChannelRepository

/** Typed text goes to [ref] from now on (replaces the previous target). Ignored if [ref] can't be written. */
internal class SetSendTargetUseCase(private val repository: ChannelRepository) {

    operator fun invoke(ref: CharacteristicRef) {
        repository.setSendTarget(ref)
    }
}
