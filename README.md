# Voice SIP 网关

中文 | [English](README.en.md)

把安卓手机的蜂窝通话与 SIP 服务器连接，用 Groundwire 等 SIP 客户端远程通话。

[下载安装包](https://github.com/1Fann/voice-sip-gateway/releases/latest) · [更新说明](https://github.com/1Fann/voice-sip-gateway/releases)

## 功能

- 蜂窝来电转 SIP，SIP 呼叫通过手机 SIM 拨出。
- 按通话选择 SIM，分别设置各卡的来电接管。
- 支持 IPv4／IPv6，以及 UDP、TCP、TLS 和 SRTP。
- 导入、导出配置，管理受信任的 TLS 证书。
- 配合通讯互联与服务端配置，显示自定义手机名和卡名。

## 使用

需要 Android 11+、ARM64、Shizuku 和相应通话权限，另需 SIP 服务器。不同手机的通话音频兼容性需要实测。配置导出文件包含明文 SIP 密码，请妥善保管。

从 Releases 下载，按安装说明完成授权，在本机填写自己的账号和连接参数。同包名、同签名的版本可覆盖升级并保留数据。

## 文档

- [新手机安装](NEW-PHONE-SETUP.md)
- [构建说明](BUILDING.md)
- [IPv6 配置](docs/ipv6-domain.md)
- [来电名称配置](docs/incoming-line.md)
- [上游使用说明](README.upstream.zh.md)

## 许可证

基于 [Voice SIP Agent](https://github.com/sparkcyf/android-voice-sip-agent)。采用 GPL-2.0-or-later，保留上游链接例外及第三方声明，见 [LICENSE](LICENSE) 和 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
