# Upstream integration 0.4.2

Upstream: sparkcyf/android-voice-sip-agent, release v0.4.2, commit 5105a1f44 (2026-11-08 release).

- Imported per-slot takeover/default routing, JSON configuration import/export, TLS certificate trust policy, local-answer handling and registration-renewal fixes.
- Retained production package/signing identity, Chinese UI and onboarding, trusted companion lifetime/recovery, IPv6 signaling/media selection and DNS refresh.
- X-VSA-SIMSLOT uses zero-based slots. Legacy X-VSA-SIM remains one-based (1/2). Conflicting, malformed or repeated selectors are rejected. Outgoing cellular placement checks the exact selected Telecom account before binding audio.
- Incoming SIP calls from cellular carry both compatible selectors. Existing PBX rules and CommLink name synchronization do not need changing.
- Build tool versions remain pinned to the tested local build chain; the upstream android-json JVM test dependency was added. Upstream 0.4.2 APK and packaged native libraries are SHA-256 pinned in third_party/upstream-native.json. Both native library digests match the previously used 0.3.5 libraries.
- Full JVM/Android unit-test tasks and release build passed. Physical registration checks passed on both devices; real-call acceptance is recorded in the release publication log.

User-facing changes are documented in GitHub Releases. This file records implementation compatibility details for maintainers.
