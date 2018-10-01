FROM clojure:lein-alpine

WORKDIR /usr/src/app

RUN apk add --no-cache --update git nodejs-lts && \
    rm -rf /var/cache/apk

RUN npm install -g grunt-cli

RUN ln -s "/usr/bin/java" "/bin/kifshare"

COPY project.clj /usr/src/app/
RUN lein deps

COPY conf/main/logback.xml /usr/src/app/
COPY . /usr/src/app

RUN npm install
RUN grunt build-resources
RUN lein uberjar
RUN cp target/kifshare-standalone.jar .

ENTRYPOINT ["kifshare", "-Dlogback.configurationFile=/etc/iplant/de/logging/kifshare-logging.xml", "-cp", ".:resources:kifshare-standalone.jar", "kifshare.core"]
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
