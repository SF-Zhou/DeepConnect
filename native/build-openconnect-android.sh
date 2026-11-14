#!/usr/bin/env bash
#
# Cross-compile libopenconnect (with the official JNI bindings compiled in)
# for Android, statically linking OpenSSL / libxml2 / LZ4 into a single
# self-contained libopenconnect.so that can be dropped into
# app/src/main/jniLibs/<abi>/.
#
# Requirements:
#   * Android NDK (default: $ANDROID_SDK_ROOT/ndk/27.2.12479018)
#   * A JDK 17 (only jni.h is needed)
#   * autoconf / automake / libtool / pkg-config / gettext on the host
#   * An OpenConnect source tree (default: ~/Public/OpenConnect)
#
# Usage:
#   native/build-openconnect-android.sh                 # arm64-v8a
#   ABI=x86_64 native/build-openconnect-android.sh      # emulator
#   CLEAN=1 native/build-openconnect-android.sh         # rebuild from scratch
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Upstream source. Point OPENCONNECT_SRC at a checkout of
# https://gitlab.com/openconnect/openconnect at the commit recorded in
# THIRD_PARTY_NOTICES.md (70d1e79d1e55849dfc71dcc199b1edb535b547e4, "v9.21-25").
# OPENCONNECT_VERSION / OPENCONNECT_RELEASE only affect the version string that the
# shipped library reports (the source tree is copied without .git metadata).
OC_SRC="${OPENCONNECT_SRC:-$HOME/Public/OpenConnect}"
OC_VERSION="${OPENCONNECT_VERSION:-9.21}"
OC_RELEASE="${OPENCONNECT_RELEASE:-25-g70d1e79d}"

# version.sh in the OpenConnect tree reports "v<none>-unknown" when there is no .git;
# these make the shipped library report a meaningful, pinned version instead.
export RPM_PACKAGE_VERSION="$OC_VERSION"
export RPM_PACKAGE_RELEASE="$OC_RELEASE"
# Generic defaults so the script also works on a fresh checkout: set ANDROID_HOME /
# ANDROID_SDK_ROOT, or install the SDK via Android Studio in the default location.
ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
NDK="${ANDROID_NDK_HOME:-$ANDROID_HOME/ndk/27.2.12479018}"
JDK="${JAVA_HOME:-}"
API_LEVEL="${API_LEVEL:-24}"
ABI="${ABI:-arm64-v8a}"
JOBS="${JOBS:-$(getconf _NPROCESSORS_ONLN 2>/dev/null || sysctl -n hw.ncpu)}"
CLEAN="${CLEAN:-0}"

case "$ABI" in
  arm64-v8a)   TRIPLET=aarch64-linux-android;    OPENSSL_TARGET=android-arm64  ;;
  armeabi-v7a) TRIPLET=armv7a-linux-androideabi; OPENSSL_TARGET=android-arm    ;;
  x86_64)      TRIPLET=x86_64-linux-android;     OPENSSL_TARGET=android-x86_64 ;;
  x86)         TRIPLET=i686-linux-android;       OPENSSL_TARGET=android-x86    ;;
  *) echo "ERROR: unsupported ABI '$ABI'" >&2; exit 1 ;;
esac

BUILD_ROOT="$PROJECT_ROOT/native/build"
WORK="$BUILD_ROOT/$ABI"
SYSROOT="$WORK/sysroot"
DOWNLOADS="$BUILD_ROOT/downloads"
JNI_INC="$WORK/jni-include"
OUT_DIR="$PROJECT_ROOT/app/src/main/jniLibs/$ABI"

OPENSSL_VER=3.0.16
LIBXML2_VER=2.14.5
LZ4_VER=1.10.0

OPENSSL_URL="https://codeload.github.com/openssl/openssl/tar.gz/refs/tags/openssl-${OPENSSL_VER}"
OPENSSL_SHA=9642aa3d97ac37da45dadabb3b576f399016acdb1df5d0e2751733e1cdf9f328
LIBXML2_URL="https://codeload.github.com/GNOME/libxml2/tar.gz/refs/tags/v${LIBXML2_VER}"
LIBXML2_SHA=f52638e4d67135c49f676d1c8fcc4f9f35afb7ec9bfb4aee743e2e86d56e006b
LZ4_URL="https://codeload.github.com/lz4/lz4/tar.gz/refs/tags/v${LZ4_VER}"
LZ4_SHA=537512904744b35e232912055ccf8ec66d768639ff3abe5788d90d792ec5f48b

TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/$(uname -s | tr '[:upper:]' '[:lower:]')-x86_64"
[ -d "$TOOLCHAIN" ] || TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/darwin-x86_64"

log() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }

[ -d "$OC_SRC" ]        || { echo "ERROR: OpenConnect source not found at $OC_SRC" >&2; exit 1; }
[ -d "$NDK" ]           || { echo "ERROR: Android NDK not found at $NDK" >&2; exit 1; }
if [ -z "$JDK" ] || [ ! -x "$JDK/bin/javac" ]; then
  echo "ERROR: JDK not found. Set JAVA_HOME to a JDK 17 installation." >&2
  exit 1
fi
[ -d "$TOOLCHAIN" ]     || { echo "ERROR: NDK LLVM toolchain not found at $TOOLCHAIN" >&2; exit 1; }

if [ "$CLEAN" = "1" ]; then
  log "Cleaning $WORK"
  rm -rf "$WORK"
fi
mkdir -p "$WORK" "$SYSROOT" "$DOWNLOADS" "$JNI_INC" "$OUT_DIR"

export PATH="$TOOLCHAIN/bin:$PATH"
export CC="$TOOLCHAIN/bin/${TRIPLET}${API_LEVEL}-clang"
export CXX="$TOOLCHAIN/bin/${TRIPLET}${API_LEVEL}-clang++"
export AR="$TOOLCHAIN/bin/llvm-ar"
export RANLIB="$TOOLCHAIN/bin/llvm-ranlib"
export STRIP="$TOOLCHAIN/bin/llvm-strip"
export NM="$TOOLCHAIN/bin/llvm-nm"
export READELF="$TOOLCHAIN/bin/llvm-readelf"
[ -x "$CC" ] || { echo "ERROR: compiler wrapper $CC not found" >&2; exit 1; }

# The NDK sysroot already provides zlib, which is a stable Android platform API.
CFLAGS_COMMON="-O2 -fPIC -DNDEBUG"
LDFLAGS_COMMON="-Wl,-z,max-page-size=16384"

# jni.h + jni_md.h side by side in one include dir (configure only takes one -I)
ln -sf "$JDK/include/jni.h" "$JNI_INC/jni.h"
ln -sf "$JDK/include/$(uname -s | tr '[:upper:]' '[:lower:]')/jni_md.h" "$JNI_INC/jni_md.h"
[ -e "$JNI_INC/jni_md.h" ] || { echo "ERROR: jni_md.h not found in $JDK/include/darwin" >&2; exit 1; }

fetch() {
  local url="$1" out="$2" sha="$3"
  if [ -f "$DOWNLOADS/$out" ] && echo "${sha}  $DOWNLOADS/$out" | shasum -a 256 -c - >/dev/null 2>&1; then
    log "Using cached $out"
    return 0
  fi
  log "Downloading $url"
  curl -fSL --retry 5 --retry-delay 3 --connect-timeout 30 -o "$DOWNLOADS/$out.tmp" "$url"
  echo "${sha}  $DOWNLOADS/$out.tmp" | shasum -a 256 -c - >/dev/null
  mv "$DOWNLOADS/$out.tmp" "$DOWNLOADS/$out"
}

########################################################################
# OpenSSL (crypto + TLS + DTLS backend)
########################################################################
build_openssl() {
  local src="$WORK/openssl-${OPENSSL_VER}"
  [ -f "$SYSROOT/lib/libssl.a" ] && [ -f "$SYSROOT/lib/libcrypto.a" ] && { log "OpenSSL already built"; return 0; }
  log "Building OpenSSL ${OPENSSL_VER} for ${ABI}"
  rm -rf "$src"; mkdir -p "$src"
  tar xzf "$DOWNLOADS/openssl-${OPENSSL_VER}.tar.gz" -C "$src" --strip-components=1
  (
    cd "$src"
    export ANDROID_NDK_ROOT="$NDK"
    export ANDROID_API="$API_LEVEL"
    # Compile-time paths must not contain build-machine directories: they would be
    # compiled into libcrypto as OPENSSLDIR / ENGINESDIR / MODULESDIR. Install into a
    # staging DESTDIR with neutral paths and copy just the libraries, headers and
    # pkg-config files into our sysroot.
    PATH="$TOOLCHAIN/bin:$PATH" ./Configure "$OPENSSL_TARGET" \
      no-shared no-tests \
      --prefix=/usr/local --openssldir=/etc/ssl
    make -j"$JOBS" build_sw >/dev/null
    rm -rf "$WORK/openssl-stage"
    make install_sw DESTDIR="$WORK/openssl-stage" >/dev/null
    mkdir -p "$SYSROOT/lib/pkgconfig" "$SYSROOT/include"
    cp "$WORK/openssl-stage/usr/local/lib/libssl.a" \
       "$WORK/openssl-stage/usr/local/lib/libcrypto.a" "$SYSROOT/lib/"
    cp -R "$WORK/openssl-stage/usr/local/include/." "$SYSROOT/include/"
    cp "$WORK/openssl-stage/usr/local/lib/pkgconfig/"*.pc "$SYSROOT/lib/pkgconfig/"
    perl -pi -e "s|^prefix=.*|prefix=$SYSROOT|" "$SYSROOT/lib/pkgconfig/"*.pc
    rm -rf "$WORK/openssl-stage"
  )
  [ -f "$SYSROOT/lib/libssl.a" ] || { echo "ERROR: OpenSSL build did not produce libssl.a" >&2; exit 1; }
}

########################################################################
# libxml2 (required by openconnect)
########################################################################
build_libxml2() {
  local src="$WORK/libxml2-${LIBXML2_VER}"
  [ -f "$SYSROOT/lib/libxml2.a" ] && { log "libxml2 already built"; return 0; }
  log "Building libxml2 ${LIBXML2_VER} for ${ABI}"
  rm -rf "$src"; mkdir -p "$src"
  tar xzf "$DOWNLOADS/libxml2-${LIBXML2_VER}.tar.gz" -C "$src" --strip-components=1
  (
    cd "$src"
    # The GNOME source archive does not ship a generated ./configure
    if [ ! -x ./configure ]; then
      NOCONFIGURE=1 PATH="/opt/homebrew/bin:/usr/local/bin:$PATH" ./autogen.sh >/dev/null
    fi
    PATH="/opt/homebrew/bin:/usr/local/bin:$PATH" \
    CFLAGS="$CFLAGS_COMMON" \
    ./configure --host="$TRIPLET" --prefix="$SYSROOT" \
      --disable-shared --enable-static \
      --without-python --without-zlib --without-lzma --without-iconv \
      --without-http --without-catalog --without-debug \
      --without-schematron --without-schemas \
      --without-xpath --without-xinclude --without-valid --without-pattern \
      --without-push --without-regexps --without-sax1 --without-history \
      --without-iso8859x --without-legacy --without-threads \
      >/dev/null
    make -j"$JOBS" >/dev/null
    make install >/dev/null
  )
  [ -f "$SYSROOT/lib/libxml2.a" ] || { echo "ERROR: libxml2 build did not produce libxml2.a" >&2; exit 1; }
}

########################################################################
# LZ4 (CSTP LZ4 compression)
########################################################################
build_lz4() {
  local src="$WORK/lz4-${LZ4_VER}"
  [ -f "$SYSROOT/lib/liblz4.a" ] && { log "lz4 already built"; return 0; }
  log "Building lz4 ${LZ4_VER} for ${ABI}"
  rm -rf "$src"; mkdir -p "$src"
  tar xzf "$DOWNLOADS/lz4-${LZ4_VER}.tar.gz" -C "$src" --strip-components=1
  (
    cd "$src"
    make -C lib -j"$JOBS" liblz4.a BUILD_SHARED=no CFLAGS="$CFLAGS_COMMON" >/dev/null
    mkdir -p "$SYSROOT/lib" "$SYSROOT/include" "$SYSROOT/lib/pkgconfig"
    cp lib/liblz4.a "$SYSROOT/lib/"
    cp lib/lz4.h lib/lz4hc.h lib/lz4frame.h lib/lz4frame_static.h lib/xxhash.h "$SYSROOT/include/"
  )
  cat > "$SYSROOT/lib/pkgconfig/liblz4.pc" <<EOF
prefix=$SYSROOT
exec_prefix=\${prefix}
libdir=\${exec_prefix}/lib
includedir=\${prefix}/include

Name: liblz4
Description: LZ4 compression library
Version: $LZ4_VER
Libs: -L\${libdir} -llz4
Cflags: -I\${includedir}
EOF
}

########################################################################
# OpenConnect itself (libopenconnect.so + JNI bindings)
########################################################################
build_openconnect() {
  local src="$WORK/openconnect"
  if [ -f "$WORK/openconnect-built/libopenconnect.so" ]; then
    log "libopenconnect already built"
    return 0
  fi
  log "Building libopenconnect (JNI) for ${ABI}"
  rm -rf "$src"; mkdir -p "$src"
  ( cd "$OC_SRC" && tar cf - --exclude .git . ) | ( cd "$src" && tar xf - )
  (
    cd "$src"
    [ -x ./configure ] || PATH="/opt/homebrew/bin:/usr/local/bin:$PATH" ./autogen.sh >/dev/null
    export PKG_CONFIG_LIBDIR="$SYSROOT/lib/pkgconfig"
    export PKG_CONFIG_PATH=""
    export LD_LIBRARY_PATH=""
    CFLAGS="$CFLAGS_COMMON -fvisibility=default" \
    LDFLAGS="$LDFLAGS_COMMON" \
    ./configure --host="$TRIPLET" \
      --prefix="$WORK/install" \
      --with-openssl=yes \
      --without-gnutls \
      --with-java="$JNI_INC" \
      --enable-jni-standalone \
      --enable-shared --disable-static \
      --disable-symvers --disable-nls \
      --disable-vhost-net \
      --without-libproxy --without-libpskc --without-libpcsclite \
      --without-stoken --without-gssapi \
      --with-vpnc-script=/etc/vpnc/vpnc-script \
      --without-openssl-version-check \
      > "$WORK/openconnect-configure.log" 2>&1 || {
        echo "ERROR: configure failed; see $WORK/openconnect-configure.log" >&2
        tail -40 "$WORK/openconnect-configure.log" >&2
        exit 1
      }
    make -j"$JOBS" libopenconnect.la > "$WORK/openconnect-make.log" 2>&1 || {
      echo "ERROR: make failed; see $WORK/openconnect-make.log" >&2
      tail -60 "$WORK/openconnect-make.log" >&2
      exit 1
    }
    grep -E "SSL library|Java bindings|DTLS support|LZ4 compression|HPKE" "$WORK/openconnect-configure.log" || true
    mkdir -p "$WORK/openconnect-built"
    cp .libs/libopenconnect.so* "$WORK/openconnect-built/" 2>/dev/null || cp .libs/libopenconnect.so "$WORK/openconnect-built/"
  )
}

########################################################################
# Package
########################################################################
package() {
  log "Packaging libopenconnect.so for $ABI"
  local so="$WORK/openconnect-built/libopenconnect.so"
  if [ ! -f "$so" ]; then
    # libtool may have produced libopenconnect.so.5.8.0 instead of the symlink
    so="$(ls "$WORK"/openconnect-built/libopenconnect.so.* 2>/dev/null | head -1)"
  fi
  [ -f "$so" ] || { echo "ERROR: libopenconnect.so not found in build output" >&2; exit 1; }
  mkdir -p "$OUT_DIR"
  cp "$so" "$OUT_DIR/libopenconnect.so"
  "$STRIP" --strip-unneeded "$OUT_DIR/libopenconnect.so"
  log "Checking exported JNI symbols"
  local n
  n=$("$NM" -D --defined-only "$OUT_DIR/libopenconnect.so" | grep -c 'Java_org_infradead_libopenconnect_' || true)
  echo "    exported JNI functions: $n"
  [ "$n" -gt 50 ] || { echo "ERROR: JNI symbols missing from libopenconnect.so" >&2; exit 1; }
  echo "    DT_NEEDED:"
  "$READELF" -d "$OUT_DIR/libopenconnect.so" | grep NEEDED | sed 's/^/      /'
  ls -lh "$OUT_DIR/libopenconnect.so"
}

fetch "$OPENSSL_URL" "openssl-${OPENSSL_VER}.tar.gz" "$OPENSSL_SHA"
fetch "$LIBXML2_URL" "libxml2-${LIBXML2_VER}.tar.gz" "$LIBXML2_SHA"
fetch "$LZ4_URL" "lz4-${LZ4_VER}.tar.gz" "$LZ4_SHA"

build_openssl
build_libxml2
build_lz4
build_openconnect
package

log "Done: $OUT_DIR/libopenconnect.so"
