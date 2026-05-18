# --- Builder stage: Clojure + Node toolchain ---
FROM clojure:temurin-25-lein-trixie AS builder

WORKDIR /usr/src/app

RUN apt-get update && \
    apt-get install -y --no-install-recommends nodejs npm git && \
    rm -rf /var/lib/apt/lists/*

COPY project.clj package.json package-lock.json ./
RUN lein deps && npm ci

COPY . .

RUN npm run build && lein uberjar

# --- Runtime stage: distroless Java 25 ---
FROM gcr.io/distroless/java25-debian13:nonroot

WORKDIR /app

COPY --from=builder /usr/src/app/target/kifshare-standalone.jar /app/kifshare-standalone.jar
COPY --from=builder /usr/src/app/resources /app/resources
COPY --from=builder /usr/src/app/conf/main/logback.xml /app/logback.xml

ENTRYPOINT ["java", \
            "-Dlogback.configurationFile=/app/logback.xml", \
            "-cp", "/app:/app/resources:/app/kifshare-standalone.jar", \
            "kifshare.core"]
CMD ["--help"]

ARG git_commit=unknown
ARG version=unknown
ARG descriptive_version=unknown

LABEL org.cyverse.git-ref="$git_commit"
LABEL org.cyverse.version="$version"
LABEL org.cyverse.descriptive-version="$descriptive_version"
LABEL org.label-schema.vcs-ref="$git_commit"
LABEL org.label-schema.vcs-url="https://github.com/cyverse-de/kifshare"
LABEL org.label-schema.version="$descriptive_version"
