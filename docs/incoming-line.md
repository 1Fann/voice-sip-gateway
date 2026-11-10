# 0.3.5-custom.7 · 来电 SIM 标记 / Incoming SIM metadata

通过 Telecom 来电的实际 PhoneAccountHandle 查找当前活动订阅和物理卡槽，将卡槽放入 Party.simSlot，在蜂窝转 SIP 时发送 X-VSA-SIM。不会根据默认拨出卡、订阅数字或账户列表顺序猜测。无匹配账户、权限被拒绝或 SIM 卡槽未知时不发送错误标记。

此版本包含 custom.6 的 IPv6/切网改动及 custom.5 的既有功能，使用相同签名覆盖升级保留账号。设备名由 PBX 的已认证端点确定，不接受来电 SIP 包伪造设备名称。

配套通讯互联 0.9.8 导出短信设置的手机名和卡名，受控 Root 同步脚本和 AGI 模板见 SMS_CONNECT_ASSISTANT-public/ops/asterisk。网关 APK 不会自行部署 PBX；需要同步安装这些集成配置。普通短信收发不依赖 Root 名称同步服务。

验证：telephony 50、pjsua2 20、app 96 项测试通过，发布构建与原签名校验通过。已部署两台安卓。用户先确认来电设备/卡槽正确，再确认 iPhone 自定义名称与短信设置一致。完整锁屏/本地联系人匹配行为仍取决于 Groundwire/CallKit。

## English

Resolve incoming SIM metadata through the actual Telecom account and active subscription, preserving the physical slot in Party.simSlot and X-VSA-SIM. Missing, ambiguous or inaccessible account metadata is not guessed. Gateway identity comes from authenticated PBX endpoints.

This release retains custom.5 features and custom.6 IPv6/network recovery, using the original signing identity. CommLink 0.9.8 provides a names-only export; separate root synchronization and AGI deployment integrate it into caller display. No PBX is silently installed by the APK.

Validation: telephony 50, pjsua2 20 and app 96 tests passed; release build and signature checks passed. Both devices were upgraded. The user verified actual device/slot labels and then configured aliases on iPhone. Groundwire/CallKit contact matching remains client-dependent.
