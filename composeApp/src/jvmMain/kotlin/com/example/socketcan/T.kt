package com.example.socketcan

fun main() {
    val dataBytes: ByteArray = byteArrayOf(0x01, 0x02, 0xFF.toByte(), 0xF2.toByte())
    val format = HexFormat {
        upperCase = true
        bytes.byteSeparator = " "
    }
    val str = dataBytes.toHexString(format)
    println(str)

    val str2 = dataBytes.joinToString(" ") { it.toString(16).uppercase() }
    println(str2)
}