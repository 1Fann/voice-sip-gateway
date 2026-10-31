# Changes and validation / 修改与验证

## 0.3.5-custom.5 — live setup checks without command startup

- Warm reconnection queries the target gateway UID's current raw AppOp and the target package's current power allowlist through Android services. Unavailable/failed framework observations fall back to the existing command checks; foreground-only and default AppOps do not count as an unconditional grant.
- No health result is cached. Telecom companion membership, current audio capabilities, runtime permissions and SIP registration are still checked normally. Client PID/Binder ownership, manual stop, call guards, generation checks and idle-only privileged-service reuse are unchanged.
- Same package, native libraries and production signing identity; versionCode 8. Intended to reduce work on the warm startup path with the paired assistant's one-second process observation, without promising an exact recovery deadline across devices.

## 0.3.5-custom.4 — idle privileged-service reuse

- An authorized Shizuku UserService survives an unexpected gateway-process exit while idle. A new gateway registers its Binder lifetime and calling PID before CONNECTED, then runs the normal current permission/audio checks and SIP registration. Ordinary service calls must match that process; stale queued calls cannot borrow a new client's lease. No saved readiness result is reused.
- Client death during audio work, with an open tap, or with owned output changes still pending retires the service instead. New/queued opens and mute requests are rejected; an independent cleanup thread invalidates old tap callbacks, restores owned output changes and closes audio. A two-second exit deadline prevents a blocked native call from retaining an ownerless process. Actual audio methods, call routing and native libraries are unchanged.
- Explicit stop still removes the UserService. APK update time still versions it so a new install recreates stale service code. This requires the paired assistant's daemon-compatible request handling (1.4.5 or later).
- If the old client's Binder death notification is late, registration retries against the same service up to five times at 100 ms intervals. Stop/new-generation guards cancel stale retries; exhaustion reports the existing binding-timeout error and requests the paired assistant's bounded recovery.
- Setup checks each runtime permission for the gateway package and skips a redundant `pm grant` only when the current PackageManager result is granted. Revocations and failed observations still use the existing repair; appops, Telecom, Doze and audio capability checks remain live.
- Same package and signing identity; versionCode 7. Host tests/build passed. Device recovery timing and real call behavior require separate validation; this build does not claim a measured three-second recovery time.

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
