package io.github.sparkcyf.voicesipagent.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.sparkcyf.voicesipagent.privileged.ShizukuConnector

/** Safe to display over the lockscreen: no configuration, caller numbers, or diagnostic logs. */
class GatewayDialerActivity : DialerRoleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runtime = GatewayRuntime.get(this)
        setContent {
            val health by runtime.health.collectAsState()
            val settings by runtime.settingsFlow.collectAsState()
            val message by roleMessage.collectAsState()
            LaunchedEffect(health.holdsDialer, health.calls) {
                setShowWhenLocked(health.holdsDialer && health.calls.count > 0)
                setTurnScreenOn(health.holdsDialer && health.calls.incoming)
            }
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Scaffold { inset ->
                    Column(Modifier.fillMaxSize().padding(inset).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Voice SIP 网关", style = MaterialTheme.typography.headlineSmall)
                        Text(UiChinese.calls(health.calls))
                        Text(if (health.session == null) "暂无网关通话" else "正在转接网关通话")
                        // Avoid putting server names or low-level failure details on a locked screen.
                        Text(when {
                            health.takeoverOffPending -> "当前通话结束后关闭网关。"
                            health.takeoverOnPending -> "当前通话结束后启用网关。"
                            health.dialerChangePending -> "等待当前通话结束后切换电话应用。"
                            !health.takeover -> "网关已关闭，请使用常用的电话应用接打电话。"
                            health.cellularReady -> "网关已就绪，可以通过 SIP 服务器接打电话。"
                            else -> "网关尚未就绪，请打开设置检查，或使用常用的电话应用。"
                        })
                        Button(onClick = { setTakeoverFromUi(!settings.takeover) }, enabled = !health.dialerChangeRunning) {
                            Text(if (settings.takeover) "关闭网关" else "启用网关")
                        }
                        Text("如需直接在手机拨号，请恢复常用的电话应用，或前往系统设置选择。")
                        if (health.holdsDialer && settings.takeover && health.shizuku == ShizukuConnector.State.CONNECTED) {
                            Button(onClick = ::switchDialer, enabled = !health.dialerChangeRunning) { Text("恢复原来的电话应用") }
                        }
                        OutlinedButton(onClick = ::chooseDialerInSettings, enabled = !health.dialerChangeRunning) { Text("前往系统设置选择电话应用") }
                        if (health.dialerChangePending) OutlinedButton(onClick = ::cancelDialerChange) { Text("取消切换电话应用") }
                        if (!health.holdsDialer && health.dialerRoleAvailable) {
                            OutlinedButton(onClick = { setTakeoverFromUi(true) }, enabled = !health.dialerChangeRunning) { Text("启用网关") }
                        }
                        OutlinedButton(onClick = ::openConfiguration) { Text("打开网关设置") }
                        if (message.isNotEmpty()) Text(UiChinese.detail(message))
                    }
                }
            }
        }
    }
}
