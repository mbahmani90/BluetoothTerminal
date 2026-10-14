package com.majidbahmani.bluetoothterminal.feature.terminal.terminal.di

import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.usecase.ObserveMessagesUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.usecase.SendMessageUseCase
import org.koin.dsl.module

/** Needs `ChannelRepository` (connection package). */
internal val terminalModule = module {
    factory { ObserveMessagesUseCase(repository = get()) }
    factory { SendMessageUseCase(repository = get()) }
}
