## 中文

本次解决部分小米 13 作为语音网关时，本机听筒仍播放通话声音的问题。

- 新增可选的“直接音频＋听筒功放静音（Root）”。在已验证的小米 13（fuxi）上，可让安卓听筒静音，同时保持 iPhone Groundwire 双向声音正常。
- 正常挂断或控制服务退出后自动释放静音；系统已经关闭的听筒线路不会被重新开启。
- 默认音频方式保持不变。其他机型请继续使用原来正常的方式；此选项不覆盖蓝牙、耳机和其他输出。

使用方法：在支持的小米 13 上启用 Root 模式 Shizuku，选择上述音频方式，并开启本机静音。本次仅更新 Voice SIP Gateway；通讯互联和 Asterisk 无需升级。

已验证：从设备实际呼出、双向声音连续、安卓听筒静音、挂断恢复。旧签名保留，可以覆盖安装并保留现有设置。

## English

Adds an optional root-only receiver amplifier mute strategy for verified Xiaomi 13 (fuxi) gateways. It keeps direct call capture, silences the Android receiver and preserves normal two-way Groundwire audio. The receiver lease is released on hang-up or control-service exit.

Enable root-mode Shizuku, select the direct-audio receiver-amplifier strategy and turn on local mute. Default audio behavior is unchanged; other devices should keep their working strategy. Bluetooth, headsets and other outputs are not covered.

Only Voice SIP Gateway needs updating. The original signing identity is retained for an in-place update. Actual two-way calling, receiver silence and hang-up cleanup were verified on the target handset.
