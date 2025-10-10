package com.example.socketcan

interface Platform {
    val os: String
    val arch: String
}

expect fun getPlatform(): Platform
/**********************************/
/**
 * 判断can接口是否up
 */
expect fun isCanUp(name: String): Boolean

/**
 * 设置can接口up
 */
expect fun setCanUp(name: String, onCanUp: (Boolean) -> Unit)

/**
 * 设置can接口down
 */
expect fun setCanDown(name: String, onCanDown: (Boolean) -> Unit)

/**
 * 判断是否需要su权限或者system权限
 */
expect fun needUpdateSystem(): Boolean

/**
 * 设置can波特率
 * 250000  250k
 * 500000 500k
 */
expect fun setCanBaudRate(name: String, baudRate: Int, onCanBaudRate: (Boolean) -> Unit)

/**
 * 枚举所有的CAN设备
 */
expect fun getCanList(): List<String>


/**********************************/

expect fun canOpen(canName: String): Int
expect fun canClose(socketFd: Int): Int
expect fun canRead(socketFd: Int): LongArray
expect fun canWrite(
    socketFd: Int,
    canId: Long,
    isExtended: Boolean,
    isRemote: Boolean,
    dateLength: Int,
    data: ByteArray
): Int

