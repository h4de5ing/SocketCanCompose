package com.example.socketcan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.socketcan.other.copyLibraryToTemp
import com.example.socketcan.other.nowTime
import com.example.socketcan.other.parseDataValue
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.experimental.and
import kotlin.random.Random
import kotlin.text.toString

@Composable
@Preview
fun App() {
    MaterialTheme { AppSocketUI() }
}

var value0: String by mutableStateOf("")
var value1: String by mutableStateOf("")
var value2: String by mutableStateOf("")
var value3: String by mutableStateOf("")
var value4: String by mutableStateOf("")
var value5: String by mutableStateOf("")

data class CanFrame(val id: Long, val eff: Long, val rtr: Long, val len: Int, val data: List<Long>) {
    fun toPrettyString(isOrigin: Boolean): String {
        if (isOrigin) {
            val idStr = if (eff == 0L) id.toString(16).padStart(3, '0')
            else id.toString(16).padStart(8, '0')
            val flag = if (eff == 0L) "标准帧" else "扩展帧"
            val r = if (rtr == 0L) "数据帧" else "远程帧"
            val dataStr = if (data.isNotEmpty()) {
                data.joinToString(" ") { it.toString(16).padStart(2, '0').uppercase() }
            } else {
                "无数据"
            }
            return "📥 $flag | $r | ID:${idStr.uppercase()} | 长度:$len | 数据:$dataStr"
        } else {
            val pgn = id.shr(8).and(0x3FFFF).toInt()
            val value = parseDataValue(pgn, data.map { it.toByte() }.toByteArray())
            return if (value != null) {
                when (value.first) {
                    65266 -> value0 = value.third
                    65153 -> value1 = value.third
                    65246 -> value2 = value.third
                    65262 -> value3 = value.third
                    61444 -> value4 = value.third
                    65215 -> value5 = value.third
                }
                "${value.second}[${pgn}][${value.third}]"
            } else {
                "未解析成功${pgn}"
            }
        }
    }
}

class CanChannelState(var name: String, initialBaudRate: Int) {
    var isUp by mutableStateOf(isCanUp(name))
    var isOpened by mutableStateOf(false)
    var baudRate by mutableIntStateOf(initialBaudRate)
    var txCounter by mutableLongStateOf(0L)
    var rxCounter by mutableLongStateOf(0L)
    var log by mutableStateOf(listOf<String>())
    var isOrigin by mutableStateOf(true)
    var isLogEmpty by mutableStateOf(false)
    var canID by mutableIntStateOf(-1)
    private var job: Job? = null

    fun up(onResult: (ok: Boolean) -> Unit = {}) {
        if (isUp) return
        setCanUp(name) { ok ->
            isUp = ok
            addLog(if (ok) "✅ $name up" else "❌ $name up failed,please setRate and try again")
            onResult(ok)
        }
    }

    fun down(onResult: (ok: Boolean) -> Unit = {}) {
        if (!isUp) return
        close()
        setCanDown(name) { ok ->
            isUp = !ok
            addLog(if (ok) "🔒 $name down" else "❌ $name down failed")
            onResult(ok)
        }
    }

    fun setRate(rate: Int, onResult: (ok: Boolean) -> Unit = {}) {
        setCanBaudRate(name, rate) { ok ->
            if (ok) {
                baudRate = rate
                addLog("⚙️ $name rate → ${rate / 1000}k")
                onResult(true)
            } else {
                addLog("❌ $name setRate failed")
                onResult(false)
            }
        }
    }

    fun open(onResult: (ok: Boolean) -> Unit = {}) {
        if (!isUp || isOpened) return
        canID = canOpen(name)
        if (canID < 0) {
            addLog("❌ $name open failed")
            onResult(false)
            return
        }
        isOpened = true
        addLog("🔌 $name opened")
        startReceive()
        onResult(true)
    }

    fun close() {
        if (!isOpened) return
        job?.cancel(); job = null
        if (canID >= 0) canID = canClose(canID)
        isOpened = false
        addLog("🔌 $name closed")
    }

    fun send(id: Long, eff: Long, rtr: Long, data: ByteArray) {
        if (!isOpened) {
            addLog("⚠️ $name not opened")
            return
        }
        canWrite(canID, id, eff == 1L, rtr == 1L, data.size, data)
        txCounter += 1
    }

    fun addLog(line: String) {
        if (!isLogEmpty) log = log + "${nowTime()} $line"
    }

    private fun startReceive() {
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val arr = canRead(canID)
                if (arr.size > 4) {
                    val frame = CanFrame(
                        id = arr[0],
                        eff = arr[1],
                        rtr = arr[2],
                        len = arr[3].toInt(),
                        data = arr.drop(4).take(arr[3].toInt())
                    )
                    withContext(Dispatchers.Main) {
                        rxCounter += 1
                        addLog(frame.toPrettyString(isOrigin = isOrigin))
                    }
                }
            }
        }
    }
}

private val globalPadding = 8.dp

@Composable
private fun AppSocketUI(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val settings = Settings()
    var can0Name by remember { mutableStateOf(settings.get<String>("can0_name") ?: "can0") }
    var can1Name by remember { mutableStateOf(settings.get<String>("can1_name") ?: "can1") }
    var can0Baud by remember { mutableIntStateOf(settings.get<Int>("can0_baud") ?: 250000) }
    var can1Baud by remember { mutableIntStateOf(settings.get<Int>("can1_baud") ?: 250000) }
    val can0 = remember { CanChannelState(can0Name, can0Baud) }
    val can1 = remember { CanChannelState(can1Name, can1Baud) }
    var showTips by remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        scope.launch {
            try {
                copyLibraryToTemp()
            } catch (e: Exception) {
                e.fillInStackTrace()
            }
        }
        onDispose { }
    }
    if (showTips) {
        Dialog(
            onDismissRequest = { }, properties = DialogProperties(dismissOnClickOutside = false)
        ) {
            Column {
                Card {
                    val platform = getPlatform()
                    Text(
                        text = "${platform.os}-${platform.arch}" + "\n${getCanList().joinToString(",")}" + (if (needUpdateSystem()) "❌需要root权限才能正常操作CAN设备\n" else "") + "\n首次使用提示：\n1. 【未打开时】点击Open上方文字可以设置CAN ID和速率\n5. 【已打开】点击 Tx/Rx 可清零计数",
                        modifier = Modifier.fillMaxWidth().padding(globalPadding),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp
                    )
                }
                Button(
                    onClick = { showTips = false },
                    modifier = Modifier.fillMaxWidth().padding(globalPadding),
                ) {
                    Text(if (showTips) "我知道了" else "关闭")
                }
            }
        }
    }
    MaterialTheme {
        Column(
            modifier = modifier.fillMaxSize().padding(globalPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(globalPadding)
            ) {
                CanCard(ch = can0, "can0", modifier = Modifier.weight(1f))
                CanCard(ch = can1, "can1", modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                J1939Panel("节气门", value0)
                J1939Panel("燃油量", value1)
                J1939Panel("空气压力", value2)
                J1939Panel("温度", value3)
                J1939Panel("转速", value4)
                J1939Panel("车速", value5)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "日志:", fontSize = 10.sp)
                Text(text = "清空", fontSize = 10.sp, modifier = Modifier.clickable {
                    can0.log = emptyList()
                    can1.log = emptyList()
                })
            }
            LogPanel(listOf(can0.log, can1.log).flatten())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CanCard(
    ch: CanChannelState, configKey: String, modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val settings = Settings()
    var newCanId by remember { mutableStateOf(ch.name) }
    var baudInput by remember { mutableStateOf(ch.baudRate.toString()) }
    var showConfigDialog by remember { mutableStateOf(false) }
    // 发送帧相关状态
    var frameType by remember { mutableLongStateOf(0L) } // 0: 标准帧, 1: 扩展帧
    var frameFormat by remember { mutableLongStateOf(0L) } // 0: 数据帧, 1: 远程帧
    var canIdInput by remember { mutableStateOf("123") }
    var canDataInput by remember { mutableStateOf("A0 A1 A2 A3 A4 A5 A6 A7") }
    // 下拉菜单展开状态
    var frameTypeExpanded by remember { mutableStateOf(false) }
    var frameFormatExpanded by remember { mutableStateOf(false) }

    var sendCount by remember { mutableStateOf("1") }
    var sendInterval by remember { mutableStateOf("100") }
    var idIncrement by remember { mutableStateOf(false) }
    var dataIncrement by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var sendJob by remember { mutableStateOf<Job?>(null) }

    var isExpand by remember { mutableStateOf(true) }
    var isTimeoutExpanded by remember { mutableStateOf(false) }
    val timeoutOptions = listOf("can0", "can1", "can2", "can3")
    Card(
        modifier = modifier.fillMaxWidth().padding(4.dp), colors = CardDefaults.cardColors(
            containerColor = when {
                ch.isOpened -> Color(0xFFE8F5E9)
                ch.isUp -> Color(0xFFFFF9C4)
                else -> Color(0xFFFFEBEE)
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(globalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val appendText = when {
                    ch.isOpened -> "OPEN @ ${ch.baudRate / 1000}k Tx:${ch.txCounter} Rx:${ch.rxCounter}"
                    ch.isUp -> "UP\tTx:${ch.txCounter} Rx:${ch.rxCounter}"
                    else -> "DOWN\tTx:${ch.txCounter} Rx:${ch.rxCounter}"
                }
                Text(
                    text = "${ch.name} ->${appendText}",
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.clickable(onClick = {
                        if (ch.isOpened) {
                            ch.rxCounter = 0
                            ch.txCounter = 0
                        } else {
                            newCanId = ch.name
                            showConfigDialog = true
                        }
                    })
                )
                if (ch.isOpened) Icon(
                    imageVector = if (isExpand) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.clickable {
                        isExpand = !isExpand
                    })
            }
            if (showConfigDialog) {
                Dialog(
                    onDismissRequest = { showConfigDialog = false }) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(globalPadding)
                    ) {
                        Column(
                            modifier = Modifier.padding(globalPadding)
                        ) {
                            Text(
                                text = "CAN通道配置",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = globalPadding)
                            )
                            ExposedDropdownMenuBox(expanded = isTimeoutExpanded, onExpandedChange = { }) {
                                OutlinedTextField(
                                    value = newCanId,
                                    onValueChange = { newCanId = it },
                                    label = { Text("CAN接口名称") },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { isTimeoutExpanded = !isTimeoutExpanded }) {
                                            Icon(
                                                imageVector = if (isTimeoutExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                contentDescription = ""
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(
                                        type = MenuAnchorType.PrimaryEditable, isTimeoutExpanded
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = isTimeoutExpanded, onDismissRequest = { isTimeoutExpanded = false }) {
                                    timeoutOptions.forEach { option ->
                                        DropdownMenuItem(text = { Text(option) }, onClick = {
                                            newCanId = option
                                            isTimeoutExpanded = false
                                        })
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = baudInput,
                                onValueChange = { baudInput = it },
                                label = { Text("波特率 (bps)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { showConfigDialog = false },
                                    modifier = Modifier.padding(end = globalPadding)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        if (newCanId != ch.name) {
                                            ch.name = newCanId
                                            settings["${configKey}_name"] = newCanId
                                        }
                                        val rate = baudInput.toIntOrNull() ?: 250000
                                        if (rate != ch.baudRate) {
                                            ch.setRate(rate)
                                            settings["${configKey}_baud"] = rate
                                        }
                                        showConfigDialog = false
                                    }, enabled = newCanId.isNotBlank() && baudInput.isNotBlank()
                                ) {
                                    Text("Ok")
                                }
                            }
                        }
                    }
                }
            }
            if (ch.isOpened && isExpand) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = globalPadding)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(globalPadding)
                    ) {
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = frameTypeExpanded,
                                onExpandedChange = { frameTypeExpanded = !frameTypeExpanded }) {
                                OutlinedTextField(
                                    value = if (frameType == 0L) "标准帧" else "扩展帧",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = frameTypeExpanded)
                                    },
                                    label = { Text("帧类型") },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )

                                ExposedDropdownMenu(
                                    expanded = frameTypeExpanded, onDismissRequest = { frameTypeExpanded = false }) {
                                    DropdownMenuItem(text = { Text("标准帧") }, onClick = {
                                        frameType = 0L
                                        frameTypeExpanded = false
                                        if (canIdInput.length > 3) {
                                            canIdInput = canIdInput.take(3)
                                        }
                                    })
                                    DropdownMenuItem(text = { Text("扩展帧") }, onClick = {
                                        frameType = 1L
                                        frameTypeExpanded = false
                                    })
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = frameFormatExpanded,
                                onExpandedChange = { frameFormatExpanded = !frameFormatExpanded }) {
                                OutlinedTextField(
                                    value = if (frameFormat == 0L) "数据帧" else "远程帧",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = frameFormatExpanded)
                                    },
                                    label = { Text("帧格式") },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )

                                ExposedDropdownMenu(
                                    expanded = frameFormatExpanded,
                                    onDismissRequest = { frameFormatExpanded = false }) {
                                    DropdownMenuItem(text = { Text("数据帧") }, onClick = {
                                        frameFormat = 0L
                                        frameFormatExpanded = false
                                    })
                                    DropdownMenuItem(text = { Text("远程帧") }, onClick = {
                                        frameFormat = 1L
                                        frameFormatExpanded = false
                                    })
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // CAN ID输入
                        OutlinedTextField(
                            value = canIdInput,
                            onValueChange = {
                                if (it.all { c -> c.isDigit() || c in 'A'..'F' || c in 'a'..'f' }) {
                                    canIdInput = it.uppercase()
                                }
                            },
                            label = { Text("CAN ID") },
                            singleLine = true,
                            modifier = Modifier.weight(3f),
                            placeholder = { Text(if (frameType == 0L) "000-7FF" else "00000000-1FFFFFFF") })

                        // 数据输入
                        OutlinedTextField(
                            value = canDataInput,
                            onValueChange = {
                                if (it.all { c -> c.isDigit() || c in 'A'..'F' || c in 'a'..'f' || c == ' ' } && it.length <= 23) {
                                    canDataInput = it.uppercase()
                                }
                            },
                            label = { Text("数据") },
                            singleLine = true,
                            modifier = Modifier.weight(8f),
                            placeholder = { Text("例如: A0 A1 A2 A3 A4 A5 A6 A7") })
                    }
                    // 发送控制参数
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(globalPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 发送次数
                        OutlinedTextField(
                            value = sendCount,
                            onValueChange = {
                                if (it.all { c -> c.isDigit() } && it.length <= 7) {
                                    sendCount = it
                                }
                            },
                            label = { Text("发送次数") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("1") },
                        )

                        // 发送间隔
                        OutlinedTextField(
                            value = sendInterval,
                            onValueChange = {
                                if (it.all { c -> c.isDigit() } && it.length <= 6) {
                                    sendInterval = it
                                }
                            },
                            label = { Text("间隔(ms)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("100") },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(globalPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { idIncrement = !idIncrement }) {
                            Checkbox(
                                checked = idIncrement, onCheckedChange = { idIncrement = it })
                            Text("ID递增", fontSize = 12.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { dataIncrement = !dataIncrement }) {
                            Checkbox(
                                checked = dataIncrement, onCheckedChange = { dataIncrement = it })
                            Text("数据递增", fontSize = 12.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                                ch.isOrigin = !ch.isOrigin
                            }) {
                            Checkbox(
                                checked = ch.isOrigin, onCheckedChange = { ch.isOrigin = it })
                            Text(if (ch.isOrigin) "原数据" else "解析", fontSize = 12.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { ch.isLogEmpty = !ch.isLogEmpty }) {
                            Checkbox(
                                checked = ch.isLogEmpty, onCheckedChange = { ch.isLogEmpty = it })
                            Text("仅统计", fontSize = 12.sp)
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(globalPadding), modifier = Modifier.fillMaxWidth()
            ) {
                if (!ch.isOpened) {
                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                if (!ch.isUp) {
                                    val rate = baudInput.toIntOrNull() ?: ch.baudRate
                                    ch.setRate(rate) { success ->
                                        if (success) {
                                            ch.up { upSuccess ->
                                                if (upSuccess) ch.open()
                                            }
                                        }
                                    }
                                } else if (!ch.isOpened) {
                                    ch.open()
                                }
                            }
                        }, modifier = Modifier.weight(1f)
                    ) {
                        Text("打开")
                    }
                }

                if (ch.isUp) {
                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                if (ch.isOpened) {
                                    sendJob?.cancel()
                                    isSending = false
                                }
                                if (ch.isUp) ch.down()
                            }
                        }, enabled = ch.isUp || ch.isOpened, modifier = Modifier.weight(1f)
                    ) {
                        Text("关闭")
                    }
                }
                if (ch.isOpened) {
                    Button(
                        onClick = {
                            if (isSending) {
                                // 停止发送
                                sendJob?.cancel()
                                isSending = false
                                ch.addLog("⏹️ 停止发送")
                            } else {
                                // 开始发送
                                startSending(
                                    ch,
                                    sendCount,
                                    sendInterval,
                                    canIdInput,
                                    canDataInput,
                                    frameType,
                                    frameFormat,
                                    idIncrement,
                                    dataIncrement,
                                    onStart = { isSending = true },
                                    onComplete = { isSending = false })?.let { job ->
                                    sendJob = job
                                }
                            }
                        }, enabled = ch.isOpened, modifier = Modifier.weight(2f)
                    ) {
                        Text(if (isSending) "停止发送" else "发送")
                    }
                }
            }
        }
    }
}

private fun startSending(
    ch: CanChannelState,
    sendCountStr: String,
    sendIntervalStr: String,
    canIdInput: String,
    canDataInput: String,
    frameType: Long,
    frameFormat: Long,
    idIncrement: Boolean,
    dataIncrement: Boolean,
    onStart: () -> Unit,
    onComplete: () -> Unit
): Job? {
    return try {
        // 验证输入
        if (canIdInput.isEmpty()) {
            ch.addLog("❌ CAN ID不能为空")
            return null
        }

        val count = sendCountStr.toIntOrNull() ?: 1
        if (count !in 1..9999999) {
            ch.addLog("❌ 发送次数应在1-9999999之间")
            return null
        }

        val interval = sendIntervalStr.toLongOrNull() ?: 1000
        if (interval !in 0..600000) {
            ch.addLog("❌ 发送间隔应在0-600000毫秒之间")
            return null
        }

        if (canIdInput.length > (if (frameType == 0L) 3 else 8)) {
            ch.addLog("❌ CAN ID长度超过限制")
            return null
        }

        val id = canIdInput.toLong(16)
        val maxId = if (frameType == 0L) 0x7FFL else 0x1FFFFFFFL
        if (id > maxId) {
            ch.addLog("❌ CAN ID超出范围")
            return null
        }

        val dataBytes: ByteArray =
            canDataInput.split(" ").filter { it.isNotBlank() }.map { it.toInt(16).toByte() }.toByteArray()

        onStart()
        ch.addLog("▶️ 开始发送 $count 帧，间隔 ${interval}ms")

        // 启动发送任务
        CoroutineScope(Dispatchers.IO).launch {
            var currentId = id
            val currentData = dataBytes.copyOf()

            for (i in 1..count) {
                if (!isActive) break // 检查协程是否被取消

                // 发送当前帧
                withContext(Dispatchers.Main) {
                    ch.send(currentId, frameType, frameFormat, currentData)
                    val format = HexFormat {
                        upperCase = true
                        bytes.byteSeparator = " "
                    }
                    val dataString = currentData.toHexString(format)
                    ch.addLog(
                        "📤 第 $i/$count 帧: ID=${
                            currentId.toString(16).uppercase()
                        },Data=${dataString}"
                    )
                }

                // 递增ID和数据
                if (idIncrement) {
                    currentId = (currentId + 1) and maxId // 确保不超出范围
                }

                if (dataIncrement) {
                    for (j in currentData.indices) {
                        currentData[j] = (currentData[j] + 1).toByte() and 0xFF.toByte() // 确保不超出字节范围
                    }
                }

                // 等待间隔（除了最后一次）
                if (i < count && interval > 0) {
                    delay(interval)
                }
            }

            withContext(Dispatchers.Main) {
                onComplete()
                ch.addLog("✅ 发送完成")
            }
        }
    } catch (e: Exception) {
        ch.addLog("❌ 发送失败: ${e.message}")
        onComplete()
        null
    }
}

@Composable
private fun J1939Panel(name: String, value: String) {
    // 记住当前的颜色状态
    var textColor by remember(value) {
        mutableStateOf(
            Color(
                red = 0.2f + Random.nextFloat() * 0.8f,
                green = 0.2f + Random.nextFloat() * 0.8f,
                blue = 0.2f + Random.nextFloat() * 0.8f
            )
        )
    }

    // 当value变化时更新颜色
    LaunchedEffect(value) {
        if (value.isNotEmpty()) { // 只在有实际值时改变颜色
            textColor = Color(
                red = 0.2f + Random.nextFloat() * 0.8f,
                green = 0.2f + Random.nextFloat() * 0.8f,
                blue = 0.2f + Random.nextFloat() * 0.8f
            )
        }
    }
    Card {
        Column(
            modifier = Modifier/*.width(200.dp)*/.padding(globalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = value, color = textColor // 应用随机颜色
            )
        }
    }
}

@Composable
private fun LogPanel(lines: List<String>) {
    val logText = remember(lines) { lines.reversed().joinToString("\n") }
    val scrollState = rememberScrollState()
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            scrollState.scrollTo(0)
        }
    }

    OutlinedTextField(
        value = logText,
        onValueChange = { },
        readOnly = true,
        modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
        textStyle = LocalTextStyle.current.copy(
            fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.Black
        ),
        maxLines = Int.MAX_VALUE,
        singleLine = false,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Gray,
            unfocusedIndicatorColor = Color.LightGray
        )
    )
}