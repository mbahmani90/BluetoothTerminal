package com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.model

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceCharacteristic

/** One service of the connected device, as the Settings screen lists it. */
internal data class ServiceSettings(
    val uuid: String,
    val characteristics: List<CharacteristicSettings>,
)

/** A characteristic with what it supports and how the terminal uses it right now. */
internal data class CharacteristicSettings(
    val characteristic: DeviceCharacteristic,
    /** Typed text goes here (at most one in the whole list). */
    val isSendTarget: Boolean,
    /** Its notifications appear in the terminal. */
    val isListening: Boolean,
)
