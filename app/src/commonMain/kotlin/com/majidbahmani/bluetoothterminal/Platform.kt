package com.majidbahmani.bluetoothterminal

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform