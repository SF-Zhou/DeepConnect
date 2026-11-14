# Third-party notices

DeepConnect is an independent app. It links the upstream OpenConnect client
library and bundles several permissively licensed native libraries inside it.
The files below are the authoritative notices; full license texts are in
[LICENSES/](LICENSES/).

## OpenConnect — LGPL-2.1-or-later

- Upstream: <https://gitlab.com/openconnect/openconnect>
- Source snapshot used for the shipped binaries: commit
  [`70d1e79d1e55849dfc71dcc199b1edb535b547e4`](https://gitlab.com/openconnect/openconnect/-/commit/70d1e79d1e55849dfc71dcc199b1edb535b547e4)
  (25 commits after the `v9.21` tag).
- Files in this repository:
  - `app/src/main/java/org/infradead/libopenconnect/LibOpenConnect.java` —
    copied unmodified from `java/src/org/infradead/libopenconnect/` of that tree.
  - `app/src/main/jniLibs/<abi>/libopenconnect.so` — built from that tree with
    `--enable-jni-standalone --with-openssl`, statically linking OpenSSL,
    libxml2 and LZ4; only Android system libraries remain as dynamic
    dependencies (`libc`, `libdl`, `liblog`, `libm`, `libz`).
- License: GNU Lesser General Public License, version 2.1 or (at your option)
  any later version. Full text: [LICENSES/OpenConnect-LGPL-2.1.txt](LICENSES/OpenConnect-LGPL-2.1.txt)
  (<https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html>).
- The library reports its version as `v9.21-25-g70d1e79d`; because the build
  copies the source tree without its `.git` directory, an unmodified build of
  this snapshot may report `v9.21-unknown` instead. Both are the same source.

### Relinking / corresponding source

The shipped `libopenconnect.so` is statically linked. To exercise your LGPL
right to modify and relink:

1. Obtain the OpenConnect source at the commit above.
2. Run `native/build-openconnect-android.sh` for the desired ABI (this
   repository's script pins and verifies the OpenSSL/libxml2/LZ4 downloads by
   SHA-256 and documents every build flag).
3. Replace `app/src/main/jniLibs/<abi>/libopenconnect.so` with the rebuilt
   library and rebuild the APK (`./gradlew :app:assembleRelease`).

The script is self-contained and can also be pointed at a modified tree with
`OPENCONNECT_SRC=/path/to/openconnect`.

## OpenSSL 3.0.16 — Apache License 2.0

Statically linked into `libopenconnect.so`. Copyright © 1998–2024 The OpenSSL
Project Authors. Full text: [LICENSES/OpenSSL-Apache-2.0.txt](LICENSES/OpenSSL-Apache-2.0.txt)
(<https://www.openssl.org/source/license.html>, <https://www.apache.org/licenses/LICENSE-2.0>).

## libxml2 2.14.5 — MIT

Statically linked into `libopenconnect.so`. Copyright © 1998–2012 Daniel
Veillard and contributors. Full text: [LICENSES/libxml2-Copyright.txt](LICENSES/libxml2-Copyright.txt)
(<https://gitlab.gnome.org/GNOME/libxml2>).

## LZ4 1.10.0 — BSD 2-Clause

Statically linked into `libopenconnect.so`. Copyright © 2011–2020 Yann Collet.
Full text: [LICENSES/LZ4-BSD-2-Clause.txt](LICENSES/LZ4-BSD-2-Clause.txt)
(<https://github.com/lz4/lz4>).

## AndroidX / Jetpack Compose / Kotlin

Distributed as Maven dependencies under the Apache License 2.0 by the Android
Open Source Project and JetBrains:
- androidx.core:core-ktx, androidx.activity:activity-compose,
  androidx.lifecycle:lifecycle-viewmodel-compose, androidx.compose.*,
  androidx.navigation:navigation-compose
- org.jetbrains.kotlinx:kotlinx-coroutines-android

Their license texts are shipped inside the respective artifacts.

## Build-time only dependencies

The Android NDK, Gradle, the Kotlin compiler toolchain and the autotools used by
`native/build-openconnect-android.sh` are build-time tools; they are not
distributed as part of the APK.
