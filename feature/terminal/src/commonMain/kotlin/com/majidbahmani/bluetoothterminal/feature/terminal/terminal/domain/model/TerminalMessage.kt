package com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.model

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef

/** One entry in the terminal log: [text] sent to or received from [characteristic]. */
internal data class TerminalMessage(
    val direction: Direction,
    val characteristic: CharacteristicRef,
    val text: String,
) {
    enum class Direction { Sent, Received }
}
