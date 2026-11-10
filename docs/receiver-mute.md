# 听筒功放静音 / Receiver amplifier mute

中文：部分系统的通话流静音已报告成功，听筒仍会出声；系统音频重定向又可能影响网关声音。新增的“直接音频＋听筒功放静音（Root）”使用直接采集，并单独关闭已验证的听筒功放。

- 仅支持已验证的小米 13（设备代号 fuxi），且 Shizuku 用户服务必须以 Root 运行。
- 需要在网关设置中主动选择此音频方式，并开启本机静音。默认音频方式不变。
- 仅控制 `T AMP Enable Switch`；开始前检查 `T Enable RCV Pin Control` 和功放原值均为 On。其他硬件或线路不会尝试套用。
- 不覆盖蓝牙、耳机或其他扬声器路线。若条件不满足，会报告失败，不会擅自切换系统通话模式。
- 控制进程通过独立管道持有租约。正常关闭或用户服务进程死亡导致管道 EOF 后，工作进程恢复功放；若系统已关闭听筒线路，则保持系统状态。恢复失败会重试并报告待恢复状态。
- 不修改系统音频 XML、保存的音量值、麦克风静音或默认拨号应用。

English: An opt-in receiver-only amplifier strategy is available for verified Xiaomi 13 (fuxi) hardware with root-mode Shizuku. It preserves direct call capture and controls only the receiver amplifier while the existing local-mute switch is enabled. Default audio behavior remains unchanged. Bluetooth, headsets and other outputs are outside its scope.

The worker validates the active receiver and original amplifier state before acquiring ownership. Closing its stdin lease, including owner-process death, restores the amplifier if the receiver route is still active. If the system has already removed that route, its state is preserved. Restoration errors are retried and reported. No audio XML, stored volume or microphone setting is changed.

Validation: direct call capture, handset silence and continuous two-way Groundwire audio were checked on the target handset. Hang-up released ownership without pending restoration. An isolated fake-mixer test on Android also checked lease EOF, route teardown before release and an inactive receiver; no actual mixer controls were touched by those fixture tests.
