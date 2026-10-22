#!/bin/bash
set -e
export JAVA_HOME=$HOME/toolchain/jdk-17.0.20.1+1
export PATH=$JAVA_HOME/bin:$PATH
SDK=$HOME/toolchain/android-sdk
$SDK/cmdline-tools/latest/bin/sdkmanager --sdk_root=$SDK "platforms;android-34" "build-tools;34.0.0" "platform-tools" > /workspace/chats/d06ff49ab42a3c69c05f5868/artifacts/sdk_install2.log 2>&1
echo SDK_DONE
