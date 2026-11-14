# Security policy

## Supported versions

Only the latest release published on the GitHub Releases page is supported.
This is a personal/community project provided as-is; there is no commercial
support contract.

## Reporting a vulnerability

Please **do not open a public issue** for security problems.

Use GitHub's private vulnerability reporting:

1. Go to the repository → **Security** tab → **Report a vulnerability**.
2. Include the affected version (APK version / commit), Android version and
   device, reproduction steps, and the impact you expect.

Alternatively, contact the maintainer listed in the repository profile. You can
expect an acknowledgement within a few days. If the report is valid we will
publish a fix and credit you in the release notes unless you ask us not to.

## What this app protects

| Asset | Where it lives | Protection |
| --- | --- | --- |
| VPN password | profile store (`filesDir/profiles.json`) | AES-256-GCM, key in Android Keystore (not extractable) |
| TOTP secret | profile store | same as above |
| Extra CA certificate | profile store | same as above |
| Pinned certificate hash | profile store | not secret (public key fingerprint) |
| Keystore key material | Android Keystore / TEE | never written to disk or logs |
| Release signing key | maintainer's machine (outside the repo) + GitHub Actions secrets | never committed; used only in CI |

- `android:allowBackup="false"` and a `dataExtractionRules` file prevent the
  encrypted store from being copied to another device.
- No analytics, telemetry, crash reporting, ads or third-party SDKs are
  included. The app has no network activity other than the VPN protocol itself
  (DNS/HTTP to the configured gateway, TLS/DTLS tunnel traffic, native
  OCSP/CRL checks performed by OpenSSL if the gateway requests them).
- VPN traffic is handled by the unmodified upstream OpenConnect library; the
  app only supplies profile data, the TUN file descriptor and user decisions
  for prompts (certificate trust, one-time codes).
- The VPN service is not exported and requires `BIND_VPN_SERVICE`.
- `VpnService.protect()` is applied to every socket the library creates so VPN
  traffic never loops back into the tunnel.

## TLS / certificate handling

- Certificates are validated against Android's system CA store (assembled into
  a PEM bundle) plus an optional per-profile CA certificate.
- A certificate that fails validation triggers a blocking dialog showing the
  reason and the SHA-256 pin. Nothing is accepted implicitly.
- "Trust & remember" stores the pin in the profile and accepts only that
  certificate for future connections. There is no global "trust everything"
  switch.
- Certificate pinning is trust-on-first-use; if the server rotates its
  certificate the user is prompted again.

## Known limitations (not vulnerabilities)

- SAML/SSO browser-based authentication is not implemented; gateways that
  require it will fail with an explicit error.
- Client certificates are supported by the native library but not yet exposed
  in the UI.
- The certificate trust prompt cannot be answered from the notification or
  without unlocking the device; an unattended reconnect to a gateway whose
  certificate changed will fail rather than silently trust it.

## Release signing and supply chain

- Release APKs are signed with a dedicated RSA-4096 keystore that lives outside
  this repository; only a base64 copy is passed to GitHub Actions through
  encrypted repository secrets.
- The release workflow builds, runs `apksigner verify`, and publishes the APK
  together with its SHA-256. Any change to the signing key invalidates updates.
- The native dependencies (OpenSSL, libxml2, LZ4) are pinned by SHA-256 in
  `native/build-openconnect-android.sh`; the OpenConnect source commit is pinned
  and documented in `THIRD_PARTY_NOTICES.md`.

## Hardening recommendations for users

- Install APKs only from this repository's Releases page and verify the
  published SHA-256.
- Prefer a gateway certificate from a real CA. If you pin a self-signed
  certificate, verify the fingerprint out of band before trusting it.
- Keep Android and the app updated; use a device with a locked bootloader if
  you rely on the VPN for sensitive work.
- If a signing keystore or its password is ever exposed, rotate the key (which
  forces a one-time reinstall for users) and publish a security advisory.
- Never share your `keystore.properties`, `*.jks` files or the
  `ANDROID_KEYSTORE_BASE64` secret; they allow signing malicious updates.
