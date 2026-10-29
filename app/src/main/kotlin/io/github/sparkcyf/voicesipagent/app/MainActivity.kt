package io.github.sparkcyf.voicesipagent.app

import android.os.Bundle
import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import io.github.sparkcyf.voicesipagent.pjsua2.SipTransport
import io.github.sparkcyf.voicesipagent.pjsua2.SrtpPolicy
import io.github.sparkcyf.voicesipagent.privileged.HealthKeys
import io.github.sparkcyf.voicesipagent.privileged.AudioMethod
import io.github.sparkcyf.voicesipagent.privileged.CallAccess
import io.github.sparkcyf.voicesipagent.privileged.CallAudioStatus
import io.github.sparkcyf.voicesipagent.privileged.ShizukuConnector

class MainActivity : DialerRoleActivity() {
    private var backgroundSummary by mutableStateOf("正在检查后台运行权限")
    private fun refreshBackground() {
        val restricted = getSystemService(android.app.ActivityManager::class.java).isBackgroundRestricted
        val exempt = getSystemService(android.os.PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        val notifications = getSystemService(android.app.NotificationManager::class.java).areNotificationsEnabled()
        backgroundSummary = when {
            restricted -> "系统限制了后台运行，清后台或息屏后可能断开。请设为不限制。"
            !exempt -> "尚未允许忽略电池优化，息屏后可能延迟接听。"
            !notifications -> "通知权限未允许，请点击授权 / 重新检查，允许显示网关状态。"
            else -> "系统后台运行检查通过；小米自启动权限请在 Voice SIP 助手中核对。"
        }
    }
    private fun backgroundSettings() {
        android.app.AlertDialog.Builder(this).setTitle("允许持续后台运行")
            .setMessage("网关需要不限制后台运行、忽略电池优化及小米自启动权限，才能在清后台和息屏后继续接打电话。推荐在 Voice SIP 助手中点击检查 / 允许后台运行；也可以到系统设置手动允许。可能增加耗电，无需锁定最近任务。")
            .setPositiveButton("打开助手授权") { _, _ -> openHelper() }
            .setNeutralButton("系统设置") { _, _ ->
                val power = getSystemService(android.os.PowerManager::class.java)
                val action = if (power.isIgnoringBatteryOptimizations(packageName))
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS else
                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                runCatching { startActivity(android.content.Intent(action, android.net.Uri.parse("package:$packageName"))) }
                    .onFailure { openPhonePermissions() }
            }.setNegativeButton("稍后",null).show()
    }
    override fun onResume() {
        super.onResume()
        refreshBackground()
        if (PhoneAccess.missing(this).isEmpty() && !getPreferences(0).getBoolean("backgroundExplained",false)) {
            getPreferences(0).edit().putBoolean("backgroundExplained",true).apply()
            backgroundSettings()
        }
    }

    private val permissionRequest = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) {
        GatewayRuntime.get(this).refreshCallAccess()
        refreshBackground()
        if (PhoneAccess.missing(this).isEmpty() && !getPreferences(0).getBoolean("backgroundExplained",false)) {
            getPreferences(0).edit().putBoolean("backgroundExplained",true).apply(); backgroundSettings()
        }
    }
    private fun requestPhonePermissions() {
        val missing = PhoneAccess.missing(this).toMutableList()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            missing += android.Manifest.permission.POST_NOTIFICATIONS
        if (missing.isNotEmpty()) permissionRequest.launch(missing.toTypedArray())
        else GatewayRuntime.get(this).refreshCallAccess()
    }
    private fun openPhonePermissions() {
        // MIUI's permission editor exposes "blank pass"; always retain the portable fallback.
        val miui = android.content.Intent("miui.intent.action.APP_PERM_EDITOR")
            .setPackage("com.miui.securitycenter").putExtra("extra_pkgname", packageName)
        if (runCatching { startActivity(miui) }.isFailure)
            startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:$packageName")))
    }
    private fun openHelper() {
        val intent = packageManager.getLaunchIntentForPackage("com.fann.voicesiphelper")
        if (intent != null) startActivity(intent)
        else android.widget.Toast.makeText(this,"请安装 Voice SIP 助手以准备 Shizuku",android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runtime = GatewayRuntime.get(this)
        val editor = ViewModelProvider(this)[SipSettingsDraft::class.java]
        editor.syncSaved(runtime.settings, runtime.pendingSettingsFlow.value)
        GatewayService.start(this)
        if (!getPreferences(0).getBoolean("permissionsExplained", false)) {
            getPreferences(0).edit().putBoolean("permissionsExplained", true).apply()
            android.app.AlertDialog.Builder(this).setTitle("首次使用：允许通话权限")
                .setMessage("网关需要拨打电话、读取 SIM 信息及音频权限。小米系统请选择允许真实信息，不要选返回空白信息。授权不会自动拨号；SIP 账号请在下方设置。")
                .setPositiveButton("继续授权") { _, _ -> requestPhonePermissions() }
                .setNegativeButton("稍后", null).show()
        }
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                val message by roleMessage.collectAsState()
                Scaffold { pad -> Screen(runtime, editor, Modifier.padding(pad), ::requestDialer, ::switchDialer,
                    ::chooseDialerInSettings, ::cancelDialerChange, ::setTakeoverFromUi, message,
                    ::requestPhonePermissions, ::openPhonePermissions, ::openHelper, backgroundSummary, ::backgroundSettings) }
            }
        }
    }
}

@Composable
private fun Screen(rt: GatewayRuntime, editor: SipSettingsDraft, modifier: Modifier, requestDialer: () -> Unit, switchDialer: () -> Unit,
    chooseDialer: () -> Unit, cancelDialer: () -> Unit, setTakeover: (Boolean) -> Unit, roleMessage: String, requestPermissions: () -> Unit, permissionSettings: () -> Unit, openHelper: () -> Unit, backgroundSummary: String, backgroundSettings: () -> Unit) {
    val h by rt.health.collectAsState()
    val callAudio by rt.callAudioStatus.collectAsState()
    val directMuteIncompatible by rt.directMuteIncompatible.collectAsState()
    val pendingSettings by rt.pendingSettingsFlow.collectAsState()
    val log by AppLog.tail.collectAsState()
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Voice SIP 网关", style = MaterialTheme.typography.headlineSmall)
        val cur by rt.settingsFlow.collectAsState()
        Section("使用准备") {
            Text(h.phoneAccessProblem ?: "电话权限与 SIM 拨号账户可用")
            Text("Root、Shizuku 授权需本人确认；仅安装 APK 不会授予系统权限。")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = requestPermissions) { Text("授权 / 重新检查") }
                OutlinedButton(onClick = permissionSettings) { Text("系统隐私权限") }
                OutlinedButton(onClick = openHelper) { Text("准备 / 修复 Shizuku") }
            }
        }
        Section("后台运行准备") {
            Text(backgroundSummary)
            OutlinedButton(onClick = backgroundSettings) { Text("检查 / 允许后台运行") }
        }
        Section("手机通话网关") {
            Toggle("启用来电转接与 SIM 呼出", cur.takeover, setTakeover) {
                when {
                    h.takeoverOffPending -> "当前通话结束后关闭"
                    h.takeoverOnPending -> "当前通话结束后启用"
                    h.dialerChangeRunning -> "正在切换默认电话应用"
                    h.dialerChangePending -> if (h.dialerChangeAcquire) "等待设为默认电话应用" else "等待恢复原来的电话应用"
                    cur.takeover -> if (h.cellularReady && !h.loopbackTest) "已就绪，可通过 SIP 服务器接打电话" else UiChinese.readiness(h)
                    h.holdsDialer && cur.keepDefaultDialer -> "网关已关闭；此应用仍是默认电话应用"
                    h.holdsDialer -> "网关已关闭，请检查默认电话应用是否恢复"
                    else -> "网关已关闭，使用系统电话接打电话"
                }
            }
            if (h.takeover || h.holdsDialer) {
                Check("通话访问方式", h.callAccessReady,
                    if (h.effectiveCallAccess == CallAccess.DIALER) {
                        if (h.holdsDialer) "此应用已是默认电话应用" else "需要选择默认电话应用"
                    } else if (Build.VERSION.SDK_INT == 30) "Android 11 不支持与系统电话并用"
                    else "与现有电话应用并用")
            }
            if (h.holdsDialer || h.effectiveCallAccess == CallAccess.DIALER) {
                Text(when {
                    !h.holdsDialer -> "启用网关后，将此应用设为默认电话应用。"
                    cur.keepDefaultDialer -> "关闭网关后，此应用仍保留为默认电话应用。"
                    else -> "关闭网关时，尝试恢复原来的电话应用。"
                }, style = MaterialTheme.typography.bodySmall)
            }
            if (h.effectiveCallAccess == CallAccess.DIALER && !h.holdsDialer) {
                if (cur.takeover && !h.dialerChangePending) {
                    Button(onClick = { setTakeover(true) }, enabled = h.dialerRoleAvailable && !h.dialerChangeRunning) { Text("启用网关") }
                }
                if (!h.dialerRoleAvailable) Text("此设备不支持选择默认电话应用。")
            }
            if (roleMessage.isNotEmpty()) Text(UiChinese.detail(roleMessage))
            if (h.holdsDialer || h.dialerChangeFailed) {
                if (h.holdsDialer && cur.takeover && h.shizuku == ShizukuConnector.State.CONNECTED) {
                    OutlinedButton(onClick = switchDialer, enabled = !h.dialerChangeRunning) { Text("恢复原来的电话应用") }
                }
                OutlinedButton(onClick = chooseDialer, enabled = !h.dialerChangeRunning) { Text("前往系统设置选择电话应用") }
            }
            if (h.dialerChangePending) OutlinedButton(onClick = cancelDialer) { Text("取消切换电话应用") }
            Toggle("本机通话静音", cur.muteEarpiece, { rt.setMuteEarpiece(it) }) {
                val unavailable = h.privileged != null &&
                    (h.privileged?.get(HealthKeys.LOCAL_SUPPRESSION) != true || !h.audioReady)
                if (cur.muteEarpiece && unavailable)
                    "当前配置无法将本机听筒或扬声器静音。网关通话期间仍会关闭本机麦克风。修改从下一通电话生效。"
                else if (cur.muteEarpiece && directMuteIncompatible)
                    "本机静音曾导致此手机的直接通话音频中断，使用该方式时将跳过静音。关闭此开关再开启，可重新测试。"
                else
                    "启用后，尝试关闭本机听筒或扬声器声音。网关通话期间仍会关闭本机麦克风。修改从下一通电话生效。"
            }
            if (h.settingsPending && pendingSettings?.hasSameSipSettings(cur) != false) {
                Text("修改将在当前通话结束后生效", style = MaterialTheme.typography.bodySmall)
            }
        }
        Section("运行状态") {
            val p = h.privileged
            val actionsEnabled = h.gatewayRunning && cur.takeover && !h.dialerChangeRunning
            Text(when {
                h.takeover -> UiChinese.readiness(h)
                h.takeoverOnPending -> "当前通话结束后开始检查。"
                else -> "网关关闭期间暂停检查。"
            }, style = MaterialTheme.typography.bodySmall)
            Check("Shizuku", h.shizuku == ShizukuConnector.State.CONNECTED,
                if (h.takeover) UiChinese.shizuku(h.shizuku) else "网关关闭期间已停止")
            Check("通话音频权限", h.audioReady)
            Check("已加入电池优化白名单", p?.get(HealthKeys.DOZE_ALLOWLISTED) == true)
            Check("SIP 注册", h.sip.ready,
                if (h.takeover) UiChinese.sip(h.sip.ready, h.sip.detail) else "网关关闭期间已停止")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (h.shizuku != ShizukuConnector.State.CONNECTED) {
                    Button(onClick = { rt.shizuku.requestPermission() }, enabled = !h.dialerChangeRunning) { Text("允许 Shizuku 授权") }
                    if (!cur.takeover) OutlinedButton(onClick = { setTakeover(true) }) { Text("启用网关并连接") }
                    if (cur.takeover && h.shizuku == ShizukuConnector.State.ERROR && h.calls.count == 0 && h.session == null) {
                        OutlinedButton(onClick = { rt.shizuku.retryBinding(); rt.requestBindingRecovery() }) { Text("重试权限连接") }
                    }
                }
                OutlinedButton(onClick = { rt.runSetup() },
                    enabled = actionsEnabled && h.shizuku == ShizukuConnector.State.CONNECTED) { Text("重新准备通话权限") }
                OutlinedButton(onClick = { rt.restartSip() },
                    enabled = actionsEnabled && cur.sipConfigured) { Text("重新连接 SIP") }
            }
        }
        Section("当前通话") {
            Text(UiChinese.calls(h.calls))
            Text(if (h.session == null) "暂无网关通话" else "网关通话正在进行")
            if (callAudio.active) {
                Text("当前音频方式：${if (callAudio.available) UiChinese.detail(callAudio.backendLabel) else "暂时无法读取状态"}")
                Text("本机通话声音：" + when {
                    !callAudio.available -> "无法读取静音状态"
                    !callAudio.muteRequested -> "未启用本机静音"
                    callAudio.outputMute == CallAudioStatus.APPLIED -> "已应用静音请求"
                    callAudio.outputMute == CallAudioStatus.PRESERVED -> "已保留原有静音"
                    callAudio.outputMute == CallAudioStatus.NOT_APPLIED -> "静音请求未生效"
                    callAudio.outputMute == CallAudioStatus.UNSUPPORTED -> "当前音频方式不支持静音"
                    else -> "静音状态未知"
                })
                if (callAudio.available && callAudio.recoveryDetail.isNotEmpty()) {
                    Text("通话音频已执行恢复，详情见原始诊断日志", style = MaterialTheme.typography.bodySmall)
                }
                Text("下一通电话：${if (cur.muteEarpiece) "启用本机静音" else "不启用本机静音"}",
                    style = MaterialTheme.typography.bodySmall)
            } else if (h.calls.owned > 0 || h.session != null) {
                Text("通话音频尚未打开。")
            }
            if (callAudio.restorationPending) {
                Text("尚未确认本机通话声音已恢复，听筒或扬声器可能仍处于静音状态。",
                    color = MaterialTheme.colorScheme.error)
                if (callAudio.ownsOutputChange && h.shizuku == ShizukuConnector.State.CONNECTED) {
                    OutlinedButton(onClick = { rt.retryAudioRestoration() },
                        enabled = !callAudio.active && h.calls.count == 0 && h.session == null) {
                        Text("重试恢复本机声音")
                    }
                } else {
                    Text("如果本机通话仍无声，请在正常手机通话中调高通话音量。",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Toggle("来电回声测试", h.loopbackTest, { rt.setLoopbackTest(it) }) {
                "网关启用时，接听手机来电并回放对方声音，用于本机测试。测试期间来电不会转到 SIP 服务器。修改在当前通话结束后生效。"
            }
            if (h.loopbackTest) {
                Text("回声测试已启用", style = MaterialTheme.typography.titleSmall)
                Button(onClick = { rt.setLoopbackTest(false) }) { Text("关闭回声测试") }
            }
        }
        SettingsSection(rt, editor)
        var advanced by rememberSaveable { mutableStateOf(false) }
        OutlinedButton(onClick = { advanced = !advanced }) { Text(if (advanced) "收起高级设置" else "高级设置") }
        if (advanced) {
            Section("系统与音频设置") {
                val preferredCallAccess = pendingSettings?.callAccess ?: cur.callAccess
                Choice("通话访问方式", listOf(CallAccess.AUTO, CallAccess.COMPANION, CallAccess.DIALER), preferredCallAccess,
                    { when (it) { CallAccess.AUTO -> "自动选择"; CallAccess.COMPANION -> "与系统电话并用"; else -> "作为默认电话应用" } },
                    enabled = { it != CallAccess.COMPANION || Build.VERSION.SDK_INT >= 31 }) {
                    rt.applySettings { current -> current.copy(callAccess = it) }
                }
                val callAccessHint = when (preferredCallAccess) {
                    CallAccess.AUTO -> if (Build.VERSION.SDK_INT < 31)
                        "Android 11 需要将此应用设为默认电话应用。"
                    else "通常与系统电话并用；若已设为默认电话，则使用默认电话模式。"
                    CallAccess.COMPANION -> when {
                        Build.VERSION.SDK_INT < 31 -> "需要 Android 12 或更新版本。"
                        h.holdsDialer -> "请先恢复常用的电话应用，再使用此模式。"
                        else -> "保留当前默认电话应用。"
                    }
                    else -> if (h.holdsDialer) null
                        else "启用网关，或点击“设为默认电话应用”进行切换。"
                }
                if (callAccessHint != null) Text(callAccessHint, style = MaterialTheme.typography.bodySmall)
                if (preferredCallAccess != cur.callAccess) {
                    Text("当前通话结束后生效。", style = MaterialTheme.typography.bodySmall)
                }
                val dialerControlsEnabled = h.effectiveCallAccess == CallAccess.DIALER && !h.dialerChangeRunning
                Toggle("关闭网关后仍保留为默认电话应用", cur.keepDefaultDialer,
                    { rt.setKeepDefaultDialer(it) }, enabled = dialerControlsEnabled) {
                    "关闭此选项后，停用网关时会尝试恢复原来的电话应用。"
                }
                if (!h.holdsDialer && h.dialerRoleAvailable) {
                    OutlinedButton(onClick = requestDialer, enabled = dialerControlsEnabled) { Text("设为默认电话应用") }
                }
                OutlinedButton(onClick = chooseDialer) { Text("前往系统设置选择电话应用") }
                Text("手动选择电话应用不会改变网关开关。上方选择默认电话模式后，将在下次启用网关时生效。",
                    style = MaterialTheme.typography.bodySmall)
                val preferredAudioMethod = pendingSettings?.audioMethod ?: cur.audioMethod
                Choice("手机通话音频方式", listOf(AudioMethod.AUTO, AudioMethod.LEGACY, AudioMethod.OFFICIAL), preferredAudioMethod,
                    { when (it) { AudioMethod.AUTO -> "自动选择"; AudioMethod.LEGACY -> "直接读取通话音频"; else -> "系统通话音频重定向" } },
                    enabled = { it != AudioMethod.OFFICIAL || Build.VERSION.SDK_INT >= 33 }) {
                    rt.applySettings { current -> current.copy(audioMethod = it) }
                }
                Text(when (preferredAudioMethod) {
                    AudioMethod.AUTO -> "优先使用系统音频重定向，准备失败时尝试直接读取通话音频。"
                    AudioMethod.LEGACY -> "直接采集和播放手机通话音频，不自动切换到重定向方式。"
                    else -> "仅使用系统通话音频重定向，需要 Android 13 或更新版本及相应权限。"
                } + " 修改从下一通电话生效。", style = MaterialTheme.typography.bodySmall)
                if (preferredAudioMethod != cur.audioMethod) {
                    Text("当前通话结束后生效。", style = MaterialTheme.typography.bodySmall)
                }
            }
            Section("诊断信息") {
                Text("Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT}")
                Text("通话访问：设置 ${cur.callAccess}，实际 ${h.effectiveCallAccess}；音频方式 ${cur.audioMethod}")
                if (callAudio.backend.isNotEmpty()) Text("最近使用的音频方式：${UiChinese.detail(callAudio.backendLabel)}")
                if (callAudio.muteDetail.isNotEmpty()) Text("本机静音详情：${UiChinese.detail(callAudio.muteDetail)}")
                if (callAudio.restoreDetail.isNotEmpty()) Text("声音恢复详情：${UiChinese.detail(callAudio.restoreDetail)}")
                Text(h.privileged?.entries?.joinToString("\n") { "${it.key}=${it.value}" } ?: "等待权限服务连接")
                Text(UiChinese.detail(h.privilegedDetail))
                Text("权限准备的原始技术结果：\n" + h.lastSetupLog)
                Text("音频权限和数据帧计数不能证明对方已收到声音，请实际测试通话双方的声音。")
            }
        }
        Section("原始诊断日志") {
            Text("下方保留协议及系统原始信息，便于排障；号码已脱敏。", style = MaterialTheme.typography.bodySmall)
            Text(log.takeLast(60).reversed().joinToString("\n"), fontFamily = FontFamily.Monospace,
                fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun SettingsSection(rt: GatewayRuntime, editor: SipSettingsDraft) = with(editor) {
    val cur by rt.settingsFlow.collectAsState()
    val pending by rt.pendingSettingsFlow.collectAsState()
    val health by rt.health.collectAsState()
    LaunchedEffect(cur, pending) { syncSaved(cur, pending) }
    val builtin = stack == SipStack.BUILTIN
    val unsaved = hasUnsavedChanges(cur, pending)
    val sipPending = pending?.hasSameSipSettings(cur) == false
    val portValidationError = portError
    Section("SIP 账号设置") {
        Choice("SIP 实现", SipStack.entries, stack, { if (it == SipStack.BUILTIN) "内置 UDP" else "pjsua2" }) {
            stack = it
            // The built-in stack is UDP only.
            if (it == SipStack.BUILTIN) {
                if (port == transport.defaultPort.toString()) port = SipTransport.UDP.defaultPort.toString()
                transport = SipTransport.UDP
            }
        }
        Choice("传输方式", SipTransport.entries, transport, { it.id }, enabled = { !builtin || it == SipTransport.UDP }) {
            // Follow the default port when it was still the previous transport's default.
            if (port == transport.defaultPort.toString()) port = it.defaultPort.toString()
            transport = it
        }
        Choice("语音加密 SRTP（仅 TLS）", SrtpPolicy.entries, srtp, { when(it) { SrtpPolicy.OFF -> "关闭"; SrtpPolicy.OPTIONAL -> "可选"; else -> "必须加密" } }, enabled = { !builtin && transport == SipTransport.TLS }) {
            srtp = it
        }
        Field("SIP 服务器地址", server) { server = it }
        Field("SIP 服务器端口", port, KeyboardType.Number, error = portValidationError) { port = it }
        Field("SIP 域名（留空使用服务器地址）", domain) { domain = it }
        Field("分机号／用户名", user) { user = it }
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("密码") },
            visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Field("手机来电转接目标（留空使用用户名）", target) { target = it }
        Field("允许呼出的号码前缀（逗号分隔，留空不限制）", prefixes) { prefixes = it }
        if (unsaved) Text("有尚未保存的修改", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary)
        if (sipPending) Text("修改将在当前通话结束后生效", style = MaterialTheme.typography.bodySmall)
        Button(onClick = { rt.applySettings(settingsUpdate()) }, enabled = portValidationError == null) { Text("保存并应用") }
        if (health.calls.count > 0 || health.session != null) {
            if (!sipPending) Text("已保存，当前通话结束后应用修改。", style = MaterialTheme.typography.bodySmall)
        } else if (!cur.takeover) {
            Text("SIP 设置已保存，下次启用网关时使用。", style = MaterialTheme.typography.bodySmall)
        }
        Toggle("手机无法接打电话时暂停 SIP 注册", cur.holdSipWhenNotReady,
            { rt.setHoldSipWhenNotReady(it) }) {
            "启动时，等待手机通话功能就绪后再向 SIP 服务器注册。" +
                "运行中出现故障时，等待 ${GatewayRuntime.SIP_HOLD_GRACE_MS / 1000} 秒恢复；仍未就绪则暂停注册，便于服务器切换线路。" +
                "注册状态变更会等待当前通话结束；手机恢复就绪后立即重新注册。"
        }
        Toggle("pjsip 调试日志", cur.sipDebugLog, { rt.setSipDebugLog(it) }) {
            "记录调试信息，日志中的号码会脱敏"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Choice(
    label: String,
    options: List<T>,
    selected: T,
    name: (T) -> String,
    enabled: (T) -> Boolean = { true },
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            options.forEachIndexed { i, opt ->
                SegmentedButton(
                    modifier = Modifier.fillMaxHeight(),
                    selected = opt == selected,
                    onClick = { onSelect(opt) },
                    enabled = enabled(opt),
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                ) { Text(name(opt)) }
            }
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit,
    enabled: Boolean = true, detail: () -> String) {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = color)
            Text(detail(), style = MaterialTheme.typography.bodySmall, color = color)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun Field(label: String, value: String, type: KeyboardType = KeyboardType.Text,
    error: String? = null, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type), modifier = Modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Check(label: String, ok: Boolean, detail: String = "") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (ok) "●" else "○", color = if (ok) Color(0xFF2E7D32) else Color(0xFFC62828))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}
