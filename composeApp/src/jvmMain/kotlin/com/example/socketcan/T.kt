package com.example.socketcan

fun main() {
    val id = 419332352
    val pgn = id.shr(8).and(0x3FFFF).toInt()

    println("pgn=${pgn}")
}