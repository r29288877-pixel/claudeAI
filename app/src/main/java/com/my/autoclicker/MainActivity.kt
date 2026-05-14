package com.my.autoclicker

import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 定義腳本動作資料結構
data class ActionCmd(val type: String, val x: Float, val y: Float, val delayMs: Long)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                MainControlPanel()
            }
        }
    }

    @Composable
    fun MainControlPanel() {
        var isRecording by remember { mutableStateOf(false) }
        var isPlaying by remember { mutableStateOf(false) }
        var loopCount by remember { mutableStateOf("1") }
        val actionList = remember { mutableStateListOf<ActionCmd>() }
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("精靈高級輔助控制器", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(10.dp))

            // 系統權限檢查與引導
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                    Text("1. 啟用模擬點擊權限")
                }
                Button(onClick = { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) }) {
                    Text("2. 允許懸浮窗")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // 控制開關區
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { 
                        isRecording = !isRecording 
                        if(isRecording) actionList.clear() // 開始新錄製
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isRecording) "🛑 停止錄製" else "🔴 開始錄製腳本")
                }

                Button(
                    onClick = {
                        isPlaying = !isPlaying
                        if (isPlaying) {
                            scope.launch {
                                val loops = loopCount.toIntOrNull() ?: 1
                                for (i in 0 until loops) {
                                    if (!isPlaying) break
                                    for (cmd in actionList) {
                                        if (!isPlaying) break
                                        MacroAccessibilityService.instance?.simulateClick(cmd.x, cmd.y)
                                        delay(cmd.delayMs)
                                    }
                                }
                                isPlaying = false
                            }
                        }
                    },
                    enabled = actionList.isNotEmpty()
                ) {
                    Text(if (isPlaying) "⏸ 停止播放" else "▶ 播放腳本")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 參數設定區
            OutlinedTextField(
                value = loopCount,
                onValueChange = { loopCount = it },
                label = { Text("重複循環次數") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("當前已錄製動作清單：", style = MaterialTheme.typography.titleMedium)

            // 虛擬錄製捕獲測試（實務上可透過全螢幕透明懸浮窗點擊監聽來收集 X, Y）
            if (isRecording) {
                Button(onClick = { 
                    actionList.add(ActionCmd("CLICK", 500f, 1000f, 1000L)) 
                }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("模擬按下螢幕中央 (500, 1000)")
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(actionList) { cmd ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("[${cmd.type}] 座標: (${cmd.x}, ${cmd.y}) -> 延遲: ${cmd.delayMs}ms", modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}
