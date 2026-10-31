# Voice SIP Gateway 0.3.5-custom.5

- 空闲且已授权的权限服务可跨网关进程意外退出保留，减少重新初始化；新客户端必须登记 Binder / PID，并重新通过当前权限、音频和 SIP 检查。
- 客户端在音频操作中或仍持有输出资源时退出，会拒绝新客户端并独立清理，清理阻塞时有界结束旧权限进程。手动关闭仍销毁服务，APK 升级会重建旧代码。
- AppOps 与电池白名单使用目标包的系统实时查询，失败时保留原命令回退。没有缓存 audioReady，没有更改原生音频库、电话路由或账号设置。
- 建议配套 Voice SIP Assistant 1.4.6，最低需要 1.4.5 的常驻请求兼容。166 项网关/权限模块测试、正式构建和 release lint 通过；原生产签名与两份上游原生音频库保持一致。

An authorized idle privileged service is reused after an unexpected gateway process exit. Binder lifetime and PID ownership prevent an old client from using a replacement client's lease. Death during audio work retires the helper with independent bounded cleanup. Every connection checks current permission/audio state and SIP registration; no cached readiness is reported.

Live target-package AppOps and power-allowlist queries remove command startup overhead, with the original fallback on failure. Manual stop, update invalidation, call routing, native libraries, settings and signing identity are retained. Use assistant 1.4.6 (minimum 1.4.5 for daemon requests).

Host tests establish lifecycle policy and build integrity, not a universal recovery deadline or successful real-call audio. Actual timing depends on the device and failure. No real calls, SMS, signing private keys or private account data are included.
