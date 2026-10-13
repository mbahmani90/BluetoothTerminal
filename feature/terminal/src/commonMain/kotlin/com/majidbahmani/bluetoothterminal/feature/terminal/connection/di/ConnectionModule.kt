package com.majidbahmani.bluetoothterminal.feature.terminal.connection.di

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.data.repository.ConnectionRepositoryImpl
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ConnectionRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.usecase.ObserveConnectionUseCase
import org.koin.dsl.module

/** Shared by the Devices and Terminal packages. Needs a `BluetoothSession` (provided by `:app`). */
internal val connectionModule = module {
    single<ConnectionRepository> { ConnectionRepositoryImpl(session = get()) }
    factory { ObserveConnectionUseCase(repository = get()) }
}
