# Changes and validation / 修改与验证

## 0.3.5-custom.1

- Neutral version suffix, versionCode4; same package, display name and signing identity as the prior local MTK-named build. No functional dialing/audio change in this version rename.
- Per-call SIM selection is carried through both SIP stacks and the call bridge into a fail-closed Telecom PhoneAccount selection.
- Upstream companion/stock dialer, emergency refusal, call ownership and native audio implementations are retained.
-The public checkout built successfully and passed 240 test cases across 34 suites (zero failures). SIM1 ordinary calls and carrier-shortcode calls were confirmed by a user. SIM2's selector/authentication/internal echo path passed, but a physical SIM2 call has not been confirmed.
- The separate assistants' cleanup/recovery checks passed on the tested rooted ROM. These checks cannot establish actual two-way call audio.
- Host shutdown tests allow up to two seconds for Linux to release a UDP descriptor after its receiver unwinds; persistent port retention still fails. This portability adjustment changes tests only, not APK runtime code.

The companion app uses the existing local signing certificate. Self-signing changes the identity and requires updating the helper certificate pins.

## 2026-10-29 local deployment verification

The user confirmed both-SIM outgoing calls and Groundwire lock-screen incoming push with two-way audio on both SIMs using the local 0.3.5-mtk.1 APK. Public IPv6 echo, caller display/grouping, and incoming two-way audio also passed after reboot. This helper/documentation cleanup does not change or rebuild the gateway APK.

## 2026-10-29 background recovery and Chinese UI / 后台恢复与汉化

- Production-signed APKs updated the existing Android 13 / Magisk installation; SIP, dialplan, DDNS and gateway configuration hashes were unchanged.
- Host checks: 26 health-policy cases per assistant; the gateway Gradle unit-test suite passed, including Chinese status/error distinctions and call-count privacy checks. Native audio libraries retain their previously pinned hashes.
- Explicit background setup was exercised in the actual APKs. The ROM rejects the symbolic AUTO_START shell operation; its framework exposes OP_AUTO_START=10008. The fix only uses that vendor operation when the actual MIUI operation appears in the ROM output. Error messages identify the setting instead of blaming Root.
- Screen-off fault injection force-stopped both assistants, the gateway and DDNS UI. Asterisk foreground ownership returned at the 11.1-second sample; complete gateway/Shizuku/audio/SIP readiness returned at 32.6 seconds. No activity was manually launched during recovery.
- A second screen-off run also force-stopped the Shizuku manager UI. All readiness checks passed at 33.3 seconds. Independent IPv6 SIP OPTIONS responses remained available throughout both sampled runs. Timings are measurements on this device, not a service guarantee.
- 用户随后确认：红米息屏且未手动打开软件，iPhone 蜂窝网络 / VPN 关闭时，600 回声、真实呼出双向声音、锁屏来电推送、号码分段和接听双向声音正常。
- No automatic real calls or audio recordings were made. Root and system consent are still required on a new phone; no router changes, account secrets or signing keys are included.
