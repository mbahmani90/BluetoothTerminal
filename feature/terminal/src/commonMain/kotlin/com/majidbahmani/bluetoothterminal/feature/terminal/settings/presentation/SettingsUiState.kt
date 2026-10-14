package com.majidbahmani.bluetoothterminal.feature.terminal.settings.presentation

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.model.ServiceSettings

/** Everything the Settings screen shows (doc 29: no screen state in composables). */
internal data class SettingsUiState(
    /** Empty while not connected. */
    val services: List<ServiceSettings> = emptyList(),
    /** The last Read of each characteristic that was read on this screen. */
    val reads: Map<CharacteristicRef, ReadResult> = emptyMap(),
)

internal sealed interface ReadResult {
    data object Reading : ReadResult

    /** [value] as text when it's printable, otherwise as hex bytes ("0A FF 01"). */
    data class Value(val value: String) : ReadResult

    data class Failed(val error: ChannelError) : ReadResult
}
