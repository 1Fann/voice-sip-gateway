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
