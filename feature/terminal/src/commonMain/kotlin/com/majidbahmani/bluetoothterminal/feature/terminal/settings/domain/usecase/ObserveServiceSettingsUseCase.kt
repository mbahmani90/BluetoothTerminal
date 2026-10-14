package com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ChannelRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.model.CharacteristicSettings
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.model.ServiceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Every service and characteristic of the connected device, with the current selection; empty while not connected. */
internal class ObserveServiceSettingsUseCase(private val repository: ChannelRepository) {

    operator fun invoke(): Flow<List<ServiceSettings>> =
        combine(repository.services, repository.selection) { services, selection ->
            services.map { service ->
                ServiceSettings(
                    uuid = service.uuid,
                    characteristics = service.characteristics.map {
                        CharacteristicSettings(
                            characteristic = it,
                            isSendTarget = it.ref == selection.sendTarget,
                            isListening = it.ref in selection.listening,
                        )
                    },
                )
            }
        }.distinctUntilChanged()
}
