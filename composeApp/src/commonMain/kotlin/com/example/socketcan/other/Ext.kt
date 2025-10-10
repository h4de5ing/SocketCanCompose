package com.example.socketcan.other

import socketcancompose.composeapp.generated.resources.Res
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun nowTime(): String = SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(Date(System.currentTimeMillis()))

fun isDebug(): Boolean = true


/**
 * 参数组编号(GPN)，用于索引
 * 特定参数号(SPN)，用于计算实际的数据方法：
 * 十六进制转十进制
 * 乘以转换系数
 * 加上偏置
 */
fun parseDataValue(pgn: Int, data: ByteArray): Pair<String, String>? {
    var name = ""
    var value = ""
    try {
        when (pgn) {
            0 -> {}
            256 -> {}
            54528 -> {}
            56320 -> {}
            56576 -> {}
            56832 -> {}
            57344 -> {}
            61440 -> {}
            61441 -> {}
            61442 -> {}
            61443 -> {//0x18F00300 加速踏板位置 1
                name = "加速踏板位置 1"
                value = "${(data[1].toInt().and(0xFF)) * 0.4}%"
            }

            61444 -> {//0x0CF00400 电子发动机控制器#1(发动机转速)
                name = "电子发动机控制器#1(发动机转速)"
                value = "${byteArray2Int(byteArrayOf(data[4], data[3])) * 0.125} rpm"
            }

            61445 -> {}
            61446 -> {}
            65130 -> {}
            65131 -> {}
            65132 -> {}
            65134 -> {}
            65135 -> {}
            65136 -> {}
            65137 -> {}
            65138 -> {}
            65139 -> {}
            65140 -> {}
            65141 -> {}
            65142 -> {}
            65143 -> {}
            65144 -> {}
            65145 -> {}
            65146 -> {}
            65147 -> {}
            65148 -> {}
            65149 -> {}
            65150 -> {}
            65151 -> {}
            65154 -> {}
            65155 -> {}
            65156 -> {}
            65157 -> {}
            65158 -> {}
            65159 -> {}
            65160 -> {}
            65161 -> {}
            65162 -> {}
            65163 -> {//0x18FE8100 发动机燃油流量1
                name = "发动机燃油流量1"
                value = "${byteArray2Int(data.slice(0..5).reversed().toByteArray()) * 0.1} m^3/h"
            }

            65164 -> {}
            65165 -> {}
            65166 -> {}
            65167 -> {}
            65168 -> {}
            65169 -> {}
            65170 -> {}
            65171 -> {}
            65172 -> {}
            65173 -> {}
            65174 -> {}
            65175 -> {}
            65176 -> {}
            65177 -> {}
            65178 -> {}
            65179 -> {}
            65180 -> {}
            65181 -> {}
            65182 -> {}
            65183 -> {}
            65184 -> {}
            65185 -> {}
            65186 -> {}
            65187 -> {}
            65188 -> {}
            65189 -> {}
            65190 -> {}
            65191 -> {}
            65192 -> {}
            65193 -> {}
            65194 -> {}
            65195 -> {}
            65196 -> {}
            65197 -> {}
            65198 -> {}
            65199 -> {//0x18FEAF00 总体燃料消耗
                name = "总体燃料消耗"
                value = "${byteArray2Int(byteArrayOf(data[7], data[6], data[5], data[4])) * 0.5} kg"
            }

            65200 -> {}
            65201 -> {}
            65202 -> {}
            65203 -> {}
            65207 -> {}
            65209 -> {}
            65210 -> {}
            65211 -> {}
            65212 -> {}
            65213 -> {}
            65214 -> {//0x18FEBE00 发动机额定转速
                name = "发动机额定转速"
                value = "${byteArray2Int(byteArrayOf(data[3], data[2])) * 0.125} rpm"
            }

            65215 -> {//0x18FEBF00 车轮速度信息(前车轴速度)
                name = "车轮速度信息(前车轴速度)"
                value = "${byteArray2Int(byteArrayOf(data[1], data[0])) / 256} km/h"
            }

            65216 -> {}
            65217 -> {}
            65218 -> {}
            65219 -> {}
            65221 -> {}
            65223 -> {//0x18FEC700 变速箱拨叉齿轮位置
                name = "变速箱拨叉齿轮位置"
                value = "${(data[0].toInt().and(0xFF)) * 0.4}%"
            }

            65237 -> {}
            65241 -> {}
            65242 -> {}
            65243 -> {}
            65244 -> {
                name = "空转操作"
                val byte14 = data.slice(0..4).toByteArray()
                val data1 = byteArray2Int(byte14) * 0.5f
                value = "$data1"
            }

            65245 -> {}
            65246 -> {//0x18FEDE00 发动机空气启动压力
                name = "发动机空气启动压力"
                value = "${data[0] * 4} kPa"
            }

            65247 -> {}
            65248 -> {//0x18FEE000 车辆总里程
                name = "车辆总里程"
                value = "${byteArray2Int(byteArrayOf(data[5], data[4])) * 0.125} km"
            }

            65249 -> {}
            65250 -> {}
            65251 -> {}
            65252 -> {}
            65253 -> {}
            65254 -> {}
            65255 -> {}
            65256 -> {}
            65257 -> {}
            65258 -> {}
            65259 -> {}
            65260 -> {}
            65261 -> {}
            65262 -> {//0x18FEEE00 发动机冷却液温度
                name = "发动机冷却液温度"
                value = "${data[1] - 40} deg C"
            }

            65263 -> {}
            65264 -> {}
            65265 -> {}
            65266 -> {//0x18FEF200 发动机节气门位置
                name = "发动机节气门位置"
                value = "${(data[6].toInt().and(0xFF)) * 0.4}%"
            }

            65267 -> {}
            65268 -> {}
            65269 -> {}
            65270 -> {}
            65271 -> {}
            65272 -> {}
            65273 -> {}
            65274 -> {}
            65275 -> {}
            65276 -> {//0x18FEFC00 燃油液面水平
                name = "燃油液面水平"
                value = "${(data[1].toInt().and(0xFF)) * 0.4}%"
            }

            65277 -> {}
            65278 -> {}
            65279 -> {}
        }
        return Pair(name, value)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun byteArray2Int(byteArray: ByteArray): Int = byteArray.fold(0) { v, b -> v.shl(8) or (b.toInt() and 0xFF) }

suspend fun copyLibraryToTemp() {
    val currentRuntime = System.getProperty("user.dir")
    println("currentRuntime=${currentRuntime}")
    val libraryName = "libsocketcan.so"
    val osName = System.getProperty("os.name")?.lowercase()?:""
    val osArch = System.getProperty("os.arch")?.lowercase()?:""

    val path = when {
        osName.contains("linux") && osArch.contains("aarch64") -> "linux-arm64"
        osName.contains("linux") -> "linux-x64"
        osName.contains("win") -> "linux-x64"
        else -> throw UnsupportedOperationException("Unsupported platform: $osName $osArch")
    }

    val resourcePath = "files/native/${path}/$libraryName"
    val bytes = Res.readBytes(resourcePath)
    val tempDir = System.getProperty("java.io.tmpdir")
    val targetFile = File(tempDir, libraryName)
    println("copy[${resourcePath}]to[${targetFile.absolutePath}]")
    targetFile.outputStream().use { output -> output.write(bytes) }
    exec("chmod 777 ${targetFile.absolutePath}")
}