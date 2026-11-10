# voice-sip-gateway 0.3.5-custom.7

## 中文

- 支持通过 IPv6 域名连接 SIP 服务器，并改进切换网络后的重新注册。
- 蜂窝来电会传递实际接听的 SIM 卡信息。
- 配合通讯互联 0.9.8 和服务端配置，可在 iPhone 显示自定义手机名和卡名。

**升级说明：** 沿用原签名，可覆盖升级并保留账号。只有 AAAA 记录的服务器仍需要客户端网络支持 IPv6；来电名称需要额外的服务端配置。

## English

- Adds IPv6-domain SIP access and improves registration after network changes.
- Forwards the actual SIM used for an incoming cellular call.
- Works with CommLink 0.9.8 and PBX integration to show configured phone/SIM aliases on iPhone.

**Upgrade:** Same-signature upgrade preserves accounts. AAAA-only servers still require IPv6 on the client network; caller labels require PBX configuration.
