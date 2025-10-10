package com.example.socketcan

import com.example.socketcan.other.exec

class JVMPlatform : Platform {
    override val os: String = System.getProperty("os.name") ?: ""
    override val arch: String = System.getProperty("os.arch") ?: ""
}

actual fun getPlatform(): Platform = JVMPlatform()
actual fun isCanUp(name: String): Boolean {
    val result = exec("ip link show $name")
    return (result.result == 0) && (result.successMsg?.contains("LOWER_UP", true) == true)
}

actual fun setCanUp(name: String, onCanUp: (Boolean) -> Unit) {
    val result = exec("ip link set $name up")
    onCanUp(result.result == 0)
}

actual fun setCanDown(name: String, onCanDown: (Boolean) -> Unit) {
    val result = exec("ip link set $name down")
    onCanDown(result.result == 0)
}

actual fun needUpdateSystem(): Boolean {
    val result = exec("whoami")
    return !(result.result == 0 && (result.successMsg?.contains("root") == true))
}

actual fun setCanBaudRate(name: String, baudRate: Int, onCanBaudRate: (Boolean) -> Unit) {
    val result = exec("ip link set $name type can bitrate $baudRate")
    onCanBaudRate(result.result == 0)
}

actual fun getCanList(): List<String> {
    return emptyList()
}


actual fun canOpen(canName: String): Int {
    return if (SocketCAN.isSupported()) SocketCAN.socketcanOpen(canName) else -1
}

actual fun canClose(socketFd: Int): Int {
    return if (SocketCAN.isSupported()) SocketCAN.socketcanClose(socketFd) else -1
}

actual fun canRead(socketFd: Int): LongArray {
    try {
        return SocketCAN.socketcanRead(socketFd)
    } catch (e: Exception) {
        e.fillInStackTrace()
    }
    return longArrayOf()
}

actual fun canWrite(
    socketFd: Int,
    canId: Long,
    isExtended: Boolean,
    isRemote: Boolean,
    dateLength: Int,
    data: ByteArray
): Int {
    return SocketCAN.socketcanWrite(socketFd, canId, isExtended, isExtended, dateLength, data)
}