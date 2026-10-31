# Validation

## 2026-10-31 live setup-query build (custom.5)

- `:privileged:testDebugUnitTest :app:testDebugUnitTest`: 166 tests in 22 suites, zero failures/errors/skips. Six added tests cover exact target UID/package use, live revocation, foreground/default AppOp rejection, framework failures, unavailable services and fallback behavior.
- Release assembly, R8 and vital lint passed with JDK 21 and the existing SDK. Original production certificate verified: `4611bccce0a83124ffae63faf4ade682fa97ed398430c4558531115a20d2b6a8`. Both packaged ARM64 native libraries are byte-identical to custom.4 and retain their upstream provenance.
- `out/Voice-SIP-Gateway-0.3.5-custom.5.apk`, versionCode 8, SHA-256 `65718a2f610377813077b14a69585c4e6f3f0b3a2e122ff5c7084459e5a8c323`.
- The implementation agent did not install, fault-inject or place calls. Reduced command startup is a code-path optimization; device timing remains a separate measurement. No configuration, authorization or audio method changed.

## 2026-10-31 idle-service recovery build

- `:privileged:testDebugUnitTest :app:testDebugUnitTest`: 160 tests across 21 suites, zero failures/errors/skips. New checks cover idle death/rebind, late old-client callbacks, stale calling PID rejection after rebind, death during a blocked open, queued stale work, failed death registration and bounded retries, manual stop, pending resources, bounded cleanup, and permission revocation/target-package checks.
- `:app:assembleRelease` passed with R8 and release vital lint. JDK 21, SDK 35, build tools 36.0.0; Kotlin compilation ran in process to avoid the host's restricted daemon directory.
- `out/Voice-SIP-Gateway-0.3.5-custom.4.apk`, package `com.fann.voicesip.dualsim`, versionCode 7. Signature was verified against the previous custom.3 APK and the existing production certificate. SHA-256: `ba23dcc02f1cee64d2086313c99bbe1b6a6a1ad575c54286ea1ef2f65c313f51`.
- Packaged `libpjsua2.so` and `libc++_shared.so` match the pinned upstream hashes. No cellular routing, audio backend, SIP credentials or permission consent flow changed.
- This build was not installed and no calls were placed by the implementation agent. The tests establish lifecycle policy, not actual HAL behavior or a measured recovery deadline. A stuck audio/system service may require privileged-process retirement; successful restoration of global output state under such a system fault still needs device validation.

## 2026-10-29 portability release / 新手机部署修复

The APKs package the migration fixes. The original signing identities were compared against APKs pulled from the connected phone before each upgrade; all four updated in place. SIP, DDNS and gateway account configuration hashes were unchanged. The temporary KSU BusyBox alias was removed; Magisk-backed IPv6 selection, both independent monitors and Asterisk authenticated echo self-test (61 returned packets) still passed. The temporary Wi-Fi boot hook was removed by the APK-owned runtime.

电话权限向导在实机完成授权后，网关自行恢复 Shizuku CONNECTED、audioReady、SIP ready。绑定超时重试日志已在实机观察到；未自动批准系统权限。号码库及空姓名修复已由 APK 部署，AGI 四项回归测试和部署八项检查通过。完整网关 JVM 测试通过，包括权限缺失、返回空数据、绑定超时、晚到回调及停止时取消重试。

An APK install is not a silent private-account restore or a cross-ROM audio guarantee. Fresh installation still requires user-approved Root/Shizuku/system permissions and local account setup. No private credentials, signing keys or permission databases are included in this release. The original cellular audio implementation is unchanged.

## Final manual call validation / 最终实际通话验证

2026-10-29: after disabling the iPhone VPN, the user confirmed SIP echo, two-way cellular audio, Groundwire lock-screen incoming push and caller-number grouping. The failed media attempt advertised the VPN address `198.18.0.1` and delivered no client RTP. Packet diagnostics recorded only addresses and counters, not audio. No real cellular calls were initiated automatically.

用户确认关闭 iPhone VPN 后，回声、普通号码双向声音、锁屏推送和号码分段均正常。网关原生音频实现未改动；新手机流程通过隔离配置与现有手机原签名升级验证，没有清空用户手机做出厂重置测试。

## 2026-10-31 phone recovery follow-up

The configured Redmi remained plugged in with its existing CPU and high-performance Wi-Fi locks active. Phone process observation now uses a one-second lightweight path; full checks retain their configured real-time intervals.

Three independent gateway process-exit measurements reached foreground + takeover + Shizuku connected + audio-ready + SIP-ready in [2.48, 2.62, 2.53] seconds. Three PBX engine process-exit measurements reached running + gateway registration in [6.0, 5.17, 5.22] seconds. Each fault was preceded by an idle cellular/PBX call check. No real call, SMS or notification was generated.

The final unattended reboot reported all services ready after 67.31 seconds, with no unlock input or activity/service launch by the observer. Measurements and artifact hashes: `D:/CodeX/HELP/reliability-20261031/phone-speed-verification.json`. These are single-device observations with polling overhead, not upper bounds.

After correcting the cold-start pending-request lifetime check, a further gateway process-exit test after reboot reached stable readiness in 2.47 seconds and retained the same idle privileged process. The final installed APK hashes were checked byte-for-byte against the local builds.

<!-- recovery-target-20261031:start -->
## 2026-10-31 恢复目标实机复测

红米 Note 10 Pro / MIUI 14，接电、息屏、ADB 连接，沿用现有唤醒设置。每次终止单个进程前均确认蜂窝与 PBX 空闲。

- 守护工作进程（电话网关）：0.41, 0.36 s（范围 0.36–0.41 s）。
- 电话网关业务进程退出至本地就绪：3.09, 2.69 s（范围 2.69–3.09 s）。整机重启后该项首次观测就绪：61.89 s。
- 网关由电话助手的守护工作进程监测；该守护样本与电话助手相同。

先前候选的实测记录（按管理 APK 的 SHA-256 区分，以下未计入最终版本样本）：

- `07d225e20b21`，电话网关守护：0.36 s。
- `07d225e20b21`，电话网关业务：2.66 s。
- `07d225e20b21`，电话网关守护：0.44 s。
- `07d225e20b21`，电话网关业务：3.00 s。
- `c12eb5669510`，电话网关守护：0.41 s。
- `c12eb5669510`，电话网关业务：3.09 s。
- `c12eb5669510`，电话网关守护：0.39 s。
- `c12eb5669510`，电话网关业务：2.97 s。

重启各时间均从同一次重启请求起算。全部六项首次齐备 61.97 s，稳定确认 66.12 s；无安全锁屏凭据，观察者未输入解锁、未主动启动界面或服务。强制深度休眠 77.66 s，4 次抽查六项服务与五个工作进程均正常，结束后已解除强制状态。

数值为本机含探测开销的首次观测，不是普适上限。守护与业务分别连续抽样确认至少 1 秒和 3 秒。电话就绪检查含网关状态及匹配的 PBX 注册记录；本次未拨打真实电话、发送短信或通知，也未验证外网送达或双向通话音频。
<!-- recovery-target-20261031:end -->
