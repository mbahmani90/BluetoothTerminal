package com.majidbahmani.bluetoothterminal.feature.terminal.connection.di

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.data.repository.ChannelRepositoryImpl
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.data.repository.ConnectionRepositoryImpl
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ChannelRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ConnectionRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.usecase.DisconnectUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.usecase.ObserveConnectionUseCase
import org.koin.dsl.module

/** Shared by the screen packages. Needs a `BluetoothSession` (provided by `:app`). */
internal val connectionModule = module {
    single<ConnectionRepository> { ConnectionRepositoryImpl(session = get()) }
    // Singleton: it holds the selection both the Terminal and Characteristics screens see.
    single<ChannelRepository> { ChannelRepositoryImpl(session = get()) }
    factory { ObserveConnectionUseCase(repository = get()) }
    factory { DisconnectUseCase(repository = get()) }
}
