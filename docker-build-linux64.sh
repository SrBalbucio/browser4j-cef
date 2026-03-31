#!/usr/bin/env bash
set -euo pipefail

echo "Building browser4j-cef native Linux x64 inside Docker..."

docker build -t browser4j-cef-linux64 -f Dockerfile.linux64 .

docker run --rm -v "$(pwd)":/workspace -w /workspace browser4j-cef-linux64 \
  bash -lc "./gradlew cmakeConfigure nativeRelease makeDistrib --no-daemon"

echo "Build finished. Artifacts should be in jcef_build/ and binary_distrib/ (if you run makeDistrib)."
