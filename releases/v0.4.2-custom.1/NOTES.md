# Voice SIP Gateway 0.4.2-custom.1

## 中文

- 跟进上游 0.4.2，增加每张 SIM 的来电接管开关和默认拨出卡设置。
- 支持导入、导出配置文件，导入前可预览要修改的设置。
- 支持导入 TLS 证书，默认保持证书验证开启。
- 保留中文界面、IPv6 域名连接、旧选卡方式和自定义来电手机名／卡名显示。

**升级说明：** 可覆盖升级本项目旧版，保留账号与配置。配置导出文件包含明文 SIP 密码，请妥善保管。不要用上游官方 APK 直接覆盖本定制版；两者包名和签名不同。关闭证书验证会降低连接安全性，不建议日常使用。

## English

- Updates to upstream 0.4.2 with per-SIM incoming takeover and default outgoing SIM selection.
- Adds configuration import/export with a preview before applying changes.
- Supports imported TLS certificates; certificate verification stays enabled by default.
- Retains the Chinese UI, IPv6-domain access, legacy SIM selection and configured incoming device/SIM names.

**Upgrade:** Install over this project's earlier signed versions to retain accounts and settings. Exported configuration contains the SIP password in plain text. Upstream APKs use a different package/signature. Disabling TLS verification weakens connection security and is not recommended for normal use.
