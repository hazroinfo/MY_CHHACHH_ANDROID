FROM eclipse-temurin:17-jdk-jammy

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=$PATH:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools

RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends curl unzip python3 ca-certificates \
 && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt/android-sdk/cmdline-tools \
 && curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip -o /tmp/cmdtools.zip \
 && unzip -q /tmp/cmdtools.zip -d /opt/android-sdk/cmdline-tools \
 && mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest \
 && rm -f /tmp/cmdtools.zip

RUN yes | sdkmanager --licenses >/dev/null || true
RUN sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"

WORKDIR /workspace
COPY . .

RUN chmod +x gradlew cloud-start.sh \
 && ./gradlew --no-daemon clean assembleDebug bundleRelease \
 && mkdir -p /public \
 && cp app/build/outputs/apk/debug/app-debug.apk /public/MY_CHHACHH_LATEST_DEBUG.apk \
 && cp app/build/outputs/bundle/release/app-release.aab /public/MY_CHHACHH_PLAYSTORE_UNSIGNED.aab

EXPOSE 8080
CMD ["./cloud-start.sh"]
