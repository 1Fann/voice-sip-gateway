# voice-sip-gateway 0.3.5-custom.5

## 中文

- 改进网关进程退出后的重新连接，减少空闲时重复准备权限服务。
- 通话或音频处理异常退出后，会清理旧资源，避免影响下一次连接。
- 改进后台权限与电池设置检查。

**升级说明：** 可覆盖升级并保留配置。建议配合 Voice SIP 助手 1.4.6，最低需要 1.4.5；实际恢复时间取决于手机与当前状态。

## English

- Reduces repeated permission-service setup when reconnecting an idle gateway.
- Cleans up resources after exits during calls or audio work.
- Improves background-permission and battery-setting checks.

**Upgrade:** Same-signature upgrade preserves configuration. Use Voice SIP Assistant 1.4.6 where possible, with 1.4.5 as the minimum for persistent requests. Recovery time varies by device.
