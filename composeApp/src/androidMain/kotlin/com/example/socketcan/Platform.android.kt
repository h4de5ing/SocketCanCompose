package com.example.socketcan

import android.os.INetworkManagementService
import android.os.ServiceManager
import java.lang.reflect.Method
import java.net.NetworkInterface

class AndroidPlatform : Platform {
    override val os: String = System.getProperty("os.name") ?: ""
    override val arch: String = System.getProperty("os.arch") ?: ""
}

actual fun getPlatform(): Platform = AndroidPlatform()
actual fun isCanUp(name: String): Boolean {
    var isUp = false
    try {
        val nis = NetworkInterface.getNetworkInterfaces()
        if (nis != null) {
            while (nis.hasMoreElements()) {
                val ni = nis.nextElement()
                if (ni.name == name) {
                    isUp = ni.isUp
                    break
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return isUp
}

actual fun setCanUp(name: String, onCanUp: (Boolean) -> Unit) {
    try {
        val iNetworkManagementService =
            INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"))
        iNetworkManagementService.setInterfaceUp(name)
        onCanUp(true)
    } catch (_: Exception) {
        onCanUp(false)
    }
}

actual fun setCanDown(name: String, onCanDown: (Boolean) -> Unit) {
    try {
        val iNetworkManagementService =
            INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"))
        iNetworkManagementService.setInterfaceDown(name)
        onCanDown(true)
    } catch (_: Exception) {
        onCanDown(false)
    }
}

actual fun needUpdateSystem(): Boolean {
    var need = false
    try {
        need =
            (INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management")).javaClass.methods.firstOrNull {
                it.name == "setCanInterfaceBitrate"
            } == null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return need
}

actual fun setCanBaudRate(name: String, baudRate: Int, onCanBaudRate: (Boolean) -> Unit) {
    try {
        val iNetworkManagementService =
            INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"))
        val setCanInterfaceBitrate: Method = iNetworkManagementService.javaClass.getMethod(
            "setCanInterfaceBitrate", String::class.java, Int::class.javaPrimitiveType
        )
        setCanInterfaceBitrate.invoke(iNetworkManagementService, name, baudRate)
        println("设置can波特率:${name} -> $baudRate")
        onCanBaudRate(true)
    } catch (_: Exception) {
        onCanBaudRate(false)
    }
}

actual fun getCanList(): List<String> {
    var canList = emptyList<String>()
    try {
        val iNetworkManagementService =
            INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"))
        canList =
            iNetworkManagementService.listInterfaces().filter { it.contains("can") }.sorted()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return canList
}

actual fun canOpen(canName: String): Int {
    return SocketCAN.socketcanOpen(canName)
}

actual fun canClose(socketFd: Int): Int {
    return SocketCAN.socketcanClose(socketFd)
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
    return SocketCAN.socketcanWrite(socketFd, canId, isExtended, isRemote, dateLength, data)
}