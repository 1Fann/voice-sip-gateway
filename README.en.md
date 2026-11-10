# Voice SIP Gateway

[中文](README.md) | English

Bridge Android cellular calls to a SIP PBX for remote calling through clients such as Groundwire.

[Download APK](https://github.com/1Fann/voice-sip-gateway/releases/latest) · [Release notes](https://github.com/1Fann/voice-sip-gateway/releases)

## Features

- Bridge incoming cellular calls to SIP and SIP calls to cellular.
- Select an outgoing SIM and configure incoming takeover per SIM.
- IPv4/IPv6, UDP/TCP/TLS and SRTP.
- Configuration import/export and trusted TLS certificates.
- Configured device/SIM caller labels with CommLink and PBX integration.

## Getting started

Requires Android 11+, ARM64, Shizuku, call permissions and a SIP PBX. Audio compatibility varies by device. Exported configuration contains the SIP password in plain text; keep it private.

Download the release, follow the installation guide, grant the requested permissions and configure your own account locally. Keep the same signing identity when upgrading to retain app data.

## Documentation

- [New-phone setup](NEW-PHONE-SETUP.md)
- [Building](BUILDING.md)
- [IPv6 setup](docs/ipv6-domain.md)
- [Incoming caller labels](docs/incoming-line.md)
- [Upstream guide](README.upstream.md)

## License

Based on [Voice SIP Agent](https://github.com/sparkcyf/android-voice-sip-agent), under GPL-2.0-or-later with upstream linking exceptions and third-party notices. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
