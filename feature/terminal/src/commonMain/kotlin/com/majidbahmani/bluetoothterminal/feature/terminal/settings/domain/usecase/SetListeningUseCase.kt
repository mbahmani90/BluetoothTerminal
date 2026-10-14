package com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ChannelRepository

/** Shows or hides [ref]'s notifications in the terminal; several can be on. Ignored if [ref] can't notify. */
internal class SetListeningUseCase(private val repository: ChannelRepository) {

    operator fun invoke(ref: CharacteristicRef, listening: Boolean) {
        repository.setListening(ref, listening)
    }
}
