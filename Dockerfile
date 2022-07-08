# Based on https://github.com/babashka/babashka/blob/00ea105d4a1efa1f84660364fa599463a2a3b047/Dockerfile
FROM clojure:openjdk-11-tools-deps-1.11.1.1149-bullseye AS BASE

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update
RUN apt-get install --no-install-recommends -yy build-essential zlib1g-dev
RUN mkdir -p /opt/target
WORKDIR "/opt"

ENV GRAALVM_VERSION="22.1.0"
ARG TARGETARCH
ENV GRAALVM_ARCH=${TARGETARCH}
RUN if [ "${TARGETARCH}" = "" ] || [ "${TARGETARCH}" = "amd64" ]; then \
      export GRAALVM_ARCH=amd64; \
    elif [ "${TARGETARCH}" = "arm64" ]; then \
      export GRAALVM_ARCH=aarch64; \
    fi && \
    echo "Installing GraalVM for ${GRAALVM_ARCH}" && \
    curl -sLO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-${GRAALVM_VERSION}/graalvm-ce-java11-linux-${GRAALVM_ARCH}-${GRAALVM_VERSION}.tar.gz && \
    tar -xzf graalvm-ce-java11-linux-${GRAALVM_ARCH}-${GRAALVM_VERSION}.tar.gz && \
    rm graalvm-ce-java11-linux-${GRAALVM_ARCH}-${GRAALVM_VERSION}.tar.gz

ENV GRAALVM_HOME="/opt/graalvm-ce-java11-${GRAALVM_VERSION}"
ENV JAVA_HOME="/opt/graalvm-ce-java11-${GRAALVM_VERSION}/bin"
ENV PATH="$JAVA_HOME:$PATH"

RUN gu install native-image

COPY script/setup-musl script/setup-musl
RUN ./script/setup-musl

COPY deps.edn .
RUN clj -X:deps prep

COPY resources/ resources/
COPY src/ src/
RUN clj -M:native-image


FROM ubuntu:latest
RUN mkdir -p /usr/local/bin
COPY --from=BASE /opt/target/sr /usr/local/bin/sr
CMD ["sr"]
