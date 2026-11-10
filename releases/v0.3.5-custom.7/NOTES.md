# 0.3.5-custom.7 · 来电 SIM 标记 / Incoming SIM metadata

通过 Telecom 来电的实际 PhoneAccountHandle 查找当前活动订阅和物理卡槽，将卡槽放入 Party.simSlot，在蜂窝转 SIP 时发送 X-VSA-SIM。不会根据默认拨出卡、订阅数字或账户列表顺序猜测。无匹配账户、权限被拒绝或 SIM 卡槽未知时不发送错误标记。

此版本包含 custom.6 的 IPv6/切网改动及 custom.5 的既有功能，使用相同签名覆盖升级保留账号。设备名由 PBX 的已认证端点确定，不接受来电 SIP 包伪造设备名称。

配套通讯互联 0.9.8 导出短信设置的手机名和卡名，受控 Root 同步脚本和 AGI 模板见 SMS_CONNECT_ASSISTANT-public/ops/asterisk。网关 APK 不会自行部署 PBX；需要同步安装这些集成配置。普通短信收发不依赖 Root 名称同步服务。

验证：telephony 50、pjsua2 20、app 96 项测试通过，发布构建与原签名校验通过。已部署两台安卓。用户先确认来电设备/卡槽正确，再确认 iPhone 自定义名称与短信设置一致。完整锁屏/本地联系人匹配行为仍取决于 Groundwire/CallKit。

## English

Resolve incoming SIM metadata through the actual Telecom account and active subscription, preserving the physical slot in Party.simSlot and X-VSA-SIM. Missing, ambiguous or inaccessible account metadata is not guessed. Gateway identity comes from authenticated PBX endpoints.

This release retains custom.5 features and custom.6 IPv6/network recovery, using the original signing identity. CommLink 0.9.8 provides a names-only export; separate root synchronization and AGI deployment integrate it into caller display. No PBX is silently installed by the APK.

Validation: telephony 50, pjsua2 20 and app 96 tests passed; release build and signature checks passed. Both devices were upgraded. The user verified actual device/slot labels and then configured aliases on iPhone. Groundwire/CallKit contact matching remains client-dependent.

# IPv6 域名接入 / IPv6 domain support

版本：0.3.5-custom.6（versionCode 9）。基于 custom.5 源码快照 acd8eb5a7ea64e148dc98fdd32cdf571dae6db40。

## 变更

- 根据 Android DNS 解析结果的首选地址族，创建匹配的 UDP/TCP/TLS IPv4 或 IPv6 传输。
- 同时设置 SIP 信令和 RTP 媒体的 IPv6 策略；IPv6 字面地址在 SIP URI 中使用方括号。
- 注册刷新前重新解析服务器域名；默认网络改变时刷新注册，即使新旧网络的地址列表相同。
- IPv4 回环及局域网账号继续使用 IPv4。

## 部署条件

网关填写 PBX 的域名和实际 SIP 端口。PBX 必须有对应地址族的监听、可达的 SIP/RTP 端口和正确的媒体地址。公网地址改变时，PBX 的媒体地址也需由部署环境更新。

如果域名只有 AAAA 记录，客户端所在网络必须能访问 IPv6；这次修改不会为 IPv4-only 网络提供中继或隧道。保留已有账号、签名与本机配置后进行覆盖安装。麦克风、电话、Shizuku 及后台运行权限需在升级后核对。

## 验证

构建时 app 96 项单元测试和 sip-pjsua2 20 项单元测试通过；发布构建通过。真机已验证 AAAA 域名注册、从 Wi-Fi 切换到移动数据后注册，以及 Groundwire 经从设备 SIM 呼出和双向语音。验证结果不代表所有运营商网络都可达。

## English

This release selects an IPv4 or IPv6 SIP transport using Android resolver preference, applies the same family to SIP and RTP, brackets IPv6 literals, refreshes registrar DNS before registration recovery, and notices default-network changes even when link addresses remain equal. IPv4 loopback/LAN accounts continue using IPv4.

The PBX must listen on the selected family and expose reachable signaling/media ports with an appropriate advertised media address. An AAAA-only deployment requires working IPv6 on the client network; this release does not add an IPv4 relay. Install as an update with the original signing identity and verify runtime permissions afterward.

Validation: 96 app and 20 pjsua2 unit tests passed, release build passed, and physical devices verified domain registration on Wi-Fi/mobile data plus an outgoing Groundwire call with two-way audio through the mobile peer. Carrier reachability remains deployment-specific.
