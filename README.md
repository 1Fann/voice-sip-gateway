# Voice SIP 网关 / Voice SIP Gateway

[English](README.en.md) · [下载 APK](https://github.com/1Fann/voice-sip-gateway/releases/latest) · [上游](https://github.com/sparkcyf/android-voice-sip-agent)

基于官方 Voice SIP Agent **v0.3.5** 的非官方定制版本。当前版本 **0.3.5-custom.1**（versionCode4），新增每通电话指定 SIM。`custom` 表示通用定制，替代原本易产生歧义的 `mtk` 版本后缀；这不是联发科专属音频优化。

## 相比上游的修改

- 解析 `X-VSA-SIM: 1` / `2`，将卡槽信息传到蜂窝呼出端。
- 呼出时按当前订阅解析真实卡槽，使用 Android Telecom 的对应 PhoneAccount，不改变系统默认呼出卡。
- 指定卡缺失、锁定或无法唯一确认时拒绝呼出，不偷偷换卡；非法/重复选卡头被拒绝。无选卡头时保留上游默认卡行为。
- 独立名称、包名与签名；调整本地构建工具。未修改上游原生音频库，未重写本机静音。只支持一通网关通话，不增加并发双卡通话。

例如由 Asterisk 的已认证账号决定1001→SIM1、1002→SIM2，再向网关添加选卡头。这个映射属于服务器配置，不写死在 APK 中。号码通过 `X-VSA-Dial` 传递。上游紧急号码拒绝逻辑保留；紧急呼叫应使用系统电话。

## 使用与更新

ARM64、Android11+，沿用上游 Shizuku 与通话权限机制；当前实际验证的是 BakaSU/联发科红米，其他设备尚未实测。目标 ROM 的权限服务兼容入口在 [Voice SIP 助手](https://github.com/1Fann/voice-sip-assistant)，不是在网关的原生音频库中。

包名继续使用 `com.fann.voicesip.dualsim` 以保留已安装用户的覆盖升级、权限和配置；名字仍是“Voice SIP 网关”。从0.3.5-mtk.1更新时保持相同签名，versionCode由3递增为4。与上游官方 App 签名和包名不同，迁移前先关闭旧网关接管，避免两者同时处理电话。不要把账号密码发到公开 issue。

“本机通话静音”是上游功能。它与具体设备的通话采集兼容性需实测；本仓库不保证所有手机都能静音且保持双向音频。

## 构建与来源

见 [BUILDING.md](BUILDING.md)、[修改与验证](CHANGES.md)、[第三方组件](THIRD_PARTY_NOTICES.md)；原始使用说明保存在 [README.upstream.md](README.upstream.md) 与 [中文原文](README.upstream.zh.md)。

发布附带源码压缩包和校验值。这个发布复用了经校验的上游 v0.3.5 ARM64 原生库，Java JNI 绑定源代码在 `third_party/pjsua2-java`；完整从源码构建原生库的脚本及补丁一并保留。CI 只用临时签名，正式签名私钥不上传。

## 许可证

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version. See [LICENSE](LICENSE). As a special exception, linking with any library licensed under the Apache License, Version 2.0 is permitted, provided all other code complies with the GPL.

保留上游许可证、链接例外及第三方版权声明。
