package com.example.socketcan

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SocketCan v3.0.0",
    ) {
        App()
    }
}