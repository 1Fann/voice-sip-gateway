# voice-sip-gateway v0.3.5-custom.1

## 中文

统一网关与按次选择 SIM：基于上游 v0.3.5，支持 X-VSA-SIM 按通话选择卡槽；未指定时保留默认卡行为。版本更名 0.3.5-custom.1，升级编号 3→4，保留原包名、签名和配置。

下载 `Voice-SIP-Gateway-0.3.5-custom.1.apk`，可以覆盖升级相同签名的旧版。附件还提供对应源码和 SHA256SUMS。不含手机账号、密码或签名私钥。

两个助手的 Root 检查任务独立于界面，清理最近任务后仍能检查；深度休眠可能延后检查，恢复也不能证明实际通话音质。已验证范围和已知限制见 README / VALIDATION 或 CHANGES。

## English

Fork of upstream v0.3.5 with per-call X-VSA-SIM slot selection and default-SIM fallback. Neutral 0.3.5-custom.1 name, versionCode 3→4, same package/signing identity/settings. Native audio libraries remain upstream bytes.

Download `Voice-SIP-Gateway-0.3.5-custom.1.apk` to update an installation with the matching signature. Corresponding source and SHA256SUMS are attached. No device accounts, passwords or signing keys are included. See the repository's validation notes for tested scope and remaining device/call checks.
