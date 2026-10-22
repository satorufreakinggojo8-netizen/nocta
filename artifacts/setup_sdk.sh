#!/bin/bash
set -e
export JAVA_HOME=$HOME/toolchain/jdk-17.0.20.1+1
export PATH=$JAVA_HOME/bin:$PATH
SDK=$HOME/toolchain/android-sdk
mkdir -p $SDK/cmdline-tools
cd $HOME/toolchain
if [ ! -f cmdline-tools.zip ]; then
  curl -fsSL --retry 3 -o cmdline-tools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
fi
if [ ! -d "$SDK/cmdline-tools/latest" ]; then
  unzip -q cmdline-tools.zip -d $SDK/cmdline-tools
  mv $SDK/cmdline-tools/cmdline-tools $SDK/cmdline-tools/latest
fi
yes | $SDK/cmdline-tools/latest/bin/sdkmanager --sdk_root=$SDK --licenses > /workspace/chats/d06ff49ab42a3c69c05f5868/artifacts/sdk_licenses.log 2>&1 || true
$SDK/cmdline-tools/latest/bin/sdkmanager --sdk_root=$SDK "platforms;android-34" "build-tools;34.0.0.0" "platform-tools" > /workspace/chats/d06ff49ab42a3c69c05f5868/artifacts/sdk_install.log 2>&1
echo SDK_DONE
