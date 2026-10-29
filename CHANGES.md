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
