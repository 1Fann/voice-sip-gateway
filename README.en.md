# Voice SIP Gateway

**Latest: 0.3.5-custom.3** — [新手机部署与权限说明 / New-phone setup](NEW-PHONE-SETUP.md)

First-run permission onboarding, SIM-account/empty-data diagnostics, accessible Health actions, bounded Shizuku binding retries and assistant recovery. Existing SIM selection and native audio are retained.

[中文](README.md) · [Download APK](https://github.com/1Fann/voice-sip-gateway/releases/latest) · [Upstream](https://github.com/sparkcyf/android-voice-sip-agent)

Unofficial fork of upstream **v0.3.5**, version **0.3.5-custom.3**, versionCode6. The neutral custom suffix replaces the former mtk label; no MediaTek-specific native audio optimization is claimed.

## Update 0.3.5-custom.3

Settings, status, call UI, consent explanations and notifications are now Chinese. Raw protocol/system logs remain in their original technical form. Added background-access guidance and a permission-protected recovery entry point for the helper.

The reproduced failure involved Android background restrictions, disabled MIUI autostart, and a readiness check accepting stale registration without a foreground service. Shortening the full-check interval alone was insufficient.

The assistants now show a background-access setup prompt. After explicit confirmation, Root changes are limited to the assistant and its trusted paired gateway: background execution, battery-optimization exemption, and recognized MIUI autostart. Phone, microphone, notification, and Shizuku consent remain separate. Unsupported settings identify the failed step instead of incorrectly reporting missing Root.

When monitoring is enabled, an approximately 10-second lightweight check detects a missing foreground service. Full checks retain the configured 1–1440-minute interval. Consented background settings may be repaired if restrictions return. Server and gateway recovery retain separate ownership, a shared repair lock, active-call guards and manual-OFF behavior. Recovery is not uninterrupted availability.

The existing Asterisk dedicated-wake option now also controls a renewable, bounded root-monitor wake lease. Disabling it releases the lease. DDNS keeps its separate existing daemon and wake option.

## Changes

Both SIP stacks parse per-call `X-VSA-SIM: 1` or2. The cellular endpoint resolves the active physical slot and supplies its Telecom PhoneAccount without changing Android's default voice SIM. Missing, locked, ambiguous or inaccessible selections fail closed; invalid/duplicate headers are refused. No selector preserves upstream default-SIM behavior. A wrong initially reported call account is rejected. One gateway call at a time; concurrent dual-SIM calls are not added. Native audio libraries and local-output muting remain upstream implementations.

A PBX may map authenticated1001→SIM1 and1002→SIM2 and add the header; that mapping is configuration, not hardcoded account routing in this APK. `X-VSA-Dial` carries the number. Existing emergency refusal remains; use the system phone for emergency calls.

## Compatibility and update

ARM64, Android11+, upstream Shizuku/call permissions. Hardware validation currently covers one rooted MediaTek Redmi. Its ROM-specific bootstrap is in [Voice SIP Assistant](https://github.com/1Fann/voice-sip-assistant), not a native gateway audio modification. Other handsets remain untested.

The package stays `com.fann.voicesip.dualsim` and retains its signing identity so users of0.3.5-mtk.1 can update with settings/permissions retained; versionCode rises3→4. The display name remains Voice SIP 网关. This is a different package/signature from upstream. Disable any old gateway before migration. Never post SIP credentials in issues. Local-output muting compatibility requires actual device audio tests.

See [BUILDING.md](BUILDING.md), [CHANGES.md](CHANGES.md), [third-party notices](THIRD_PARTY_NOTICES.md), and the preserved [upstream README](README.upstream.md). Releases include APK, checksums and a source archive. This release reuses the verified upstream v0.3.5 ARM64 native libraries; generated Java JNI sources are included under third_party/pjsua2-java. Original native build scripts and patches remain. Production signing keys are never uploaded.

## License

GPL-2.0-or-later with the upstream exception permitting Apache-2.0 library linking provided the remaining code complies with the GPL. Full original grant is in [README.md](README.md); [LICENSE](LICENSE) and third-party notices are preserved.
