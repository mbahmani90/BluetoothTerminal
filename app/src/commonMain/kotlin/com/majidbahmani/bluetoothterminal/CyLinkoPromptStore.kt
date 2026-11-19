package com.majidbahmani.bluetoothterminal

/** Remembers, across launches, that the user ticked "Don't ask again" on the CyLinko dialog. */
interface CyLinkoPromptStore {
    val isMuted: Boolean
    fun mute()
}

internal const val CYLINKO_PROMPT_MUTED_KEY = "cylinko_prompt_muted"
