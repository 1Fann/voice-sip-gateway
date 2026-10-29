# Voice SIP 网关 / Voice SIP Gateway

**Latest: 0.3.5-custom.3** — [新手机部署与权限说明 / New-phone setup](NEW-PHONE-SETUP.md)

首次授权向导，检测 SIM 账户/空白信息；Health 提供明确的启用和授权入口；Shizuku 有界重试及助手恢复。保留默认卡/按次选卡、号码拨出和原生音频功能。

[English](README.en.md) · [下载 APK](https://github.com/1Fann/voice-sip-gateway/releases/latest) · [上游](https://github.com/sparkcyf/android-voice-sip-agent)

基于官方 Voice SIP Agent **v0.3.5** 的非官方定制版本。当前版本 **0.3.5-custom.3**（versionCode6），新增每通电话指定 SIM。`custom` 表示通用定制，替代原本易产生歧义的 `mtk` 版本后缀；这不是联发科专属音频优化。

## 本次更新 0.3.5-custom.3

网关的设置、状态、通话页面、授权说明和通知已完整汉化；原始协议 / 系统日志保留技术原文。新增后台授权提示与系统设置入口，受权限保护的后台恢复入口供助手使用。

已确认的故障是系统限制后台运行 / 小米自启动关闭，以及状态检查仅看到旧注册状态。不是单纯检查间隔过长。

首次打开助手时显示“后台运行准备”。点击“检查 / 允许后台运行”并明确确认后，助手仅为自己及受信任的配套网关解除后台限制、加入电池优化白名单，并在可识别的小米系统允许自启动。电话、麦克风、通知及 Shizuku 权限仍需用户自行允许。遇到不支持的系统设置时说明具体失败项，不再统一提示 Root 失败。

自动检查开启时，每约 10 秒轻量检查服务是否退出；完整检查仍按用户配置的 1–1440 分钟执行。授权后台设置后，检查任务可修复被重新施加的后台限制。Asterisk 与网关的恢复仍分属各自助手，共用恢复锁；通话中或无法确认通话状态时不重启，尊重应用内手动停止。清理最近任务无需加锁，但被杀进程恢复期间仍可能暂时无法接听，不承诺零中断。

专用唤醒锁继续由 Asterisk 的原有开关控制。开启时，Root 检查任务持有可续期、到期自动释放的唤醒锁，防止界面被清理后检查立即休眠；关闭时释放。DDNS 的独立守护和唤醒选项保持原状。

## 相比上游的修改

- 解析 `X-VSA-SIM: 1` / `2`，将卡槽信息传到蜂窝呼出端。
- 呼出时按当前订阅解析真实卡槽，使用 Android Telecom 的对应 PhoneAccount，不改变系统默认呼出卡。
- 指定卡缺失、锁定或无法唯一确认时拒绝呼出，不偷偷换卡；非法/重复选卡头被拒绝。无选卡头时保留上游默认卡行为。
- 独立名称、包名与签名；调整本地构建工具。未修改上游原生音频库，未重写本机静音。只支持一通网关通话，不增加并发双卡通话。

例如由 Asterisk 的已认证账号决定1001→SIM1、1002→SIM2，再向网关添加选卡头。这个映射属于服务器配置，不写死在 APK 中。号码通过 `X-VSA-Dial` 传递。上游紧急号码拒绝逻辑保留；紧急呼叫应使用系统电话。

## 使用与更新

ARM64、Android11+，沿用上游 Shizuku 与通话权限机制；当前实际验证包括 Android 13 / Magisk 红米及此前 BakaSU 红米，其他设备尚未实测。目标 ROM 的权限服务兼容入口在 [Voice SIP 助手](https://github.com/1Fann/voice-sip-assistant)，不是在网关的原生音频库中。

包名继续使用 `com.fann.voicesip.dualsim` 以保留已安装用户的覆盖升级、权限和配置；名字仍是“Voice SIP 网关”。从0.3.5-mtk.1更新时保持相同签名，versionCode由3递增为6。与上游官方 App 签名和包名不同，迁移前先关闭旧网关接管，避免两者同时处理电话。不要把账号密码发到公开 issue。

“本机通话静音”是上游功能。它与具体设备的通话采集兼容性需实测；本仓库不保证所有手机都能静音且保持双向音频。

## 构建与来源

见 [BUILDING.md](BUILDING.md)、[修改与验证](CHANGES.md)、[第三方组件](THIRD_PARTY_NOTICES.md)；原始使用说明保存在 [README.upstream.md](README.upstream.md) 与 [中文原文](README.upstream.zh.md)。

发布附带源码压缩包和校验值。这个发布复用了经校验的上游 v0.3.5 ARM64 原生库，Java JNI 绑定源代码在 `third_party/pjsua2-java`；完整从源码构建原生库的脚本及补丁一并保留。CI 只用临时签名，正式签名私钥不上传。

## 许可证

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version. See [LICENSE](LICENSE). As a special exception, linking with any library licensed under the Apache License, Version 2.0 is permitted, provided all other code complies with the GPL.

保留上游许可证、链接例外及第三方版权声明。
