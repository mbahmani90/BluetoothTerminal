package com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model

/** Address of one characteristic on the connected device. UUIDs are lowercase 128-bit strings. */
internal data class CharacteristicRef(
    val service: String,
    val characteristic: String,
)

/** What a characteristic supports; [canNotify] includes indicate. */
internal data class DeviceCharacteristic(
    val ref: CharacteristicRef,
    val canRead: Boolean,
    val canWrite: Boolean,
    val canNotify: Boolean,
)

internal data class DeviceService(
    val uuid: String,
    val characteristics: List<DeviceCharacteristic>,
)

/**
 * Which characteristics the terminal uses: typed text goes to [sendTarget] (at most one),
 * incoming data comes from every characteristic in [listening].
 */
internal data class ChannelSelection(
    val sendTarget: CharacteristicRef? = null,
    val listening: Set<CharacteristicRef> = emptySet(),
)

/** [bytes] the device sent on [source]; one notification, not necessarily a whole line. */
internal class IncomingData(val source: CharacteristicRef, val bytes: ByteArray) {

    override fun equals(other: Any?): Boolean =
        other is IncomingData && source == other.source && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = 31 * source.hashCode() + bytes.contentHashCode()

    override fun toString(): String = "IncomingData(source=$source, bytes=${bytes.contentToString()})"
}

/** Why sending or reading didn't work, in the terms the UI shows. */
internal enum class ChannelError {
    NotConnected,

    /** Nothing is selected to send to. */
    NoSendTarget,

    /** The device has no such characteristic, or it can't do this (e.g. read a write-only one). */
    NotSupported,

    Failed,
}

/** The failure in a `Result` from the channel; [error] says why. */
internal class ChannelException(val error: ChannelError) : Exception(error.name)
