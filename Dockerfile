FROM discoenv/clojure-base:master

ENV CONF_TEMPLATE=/usr/src/app/kifshare.properties.tmpl
ENV CONF_FILENAME=kifshare.properties
ENV PROGRAM=kifshare

RUN apk add --no-cache --update nodejs-lts && \
    rm -rf /var/cache/apk

VOLUME ["/etc/iplant/de"]

RUN npm install -g grunt-cli

COPY project.clj /usr/src/app/
RUN lein deps

COPY conf/main/logback.xml /usr/src/app/
COPY . /usr/src/app

RUN npm install
RUN grunt build-resources
RUN lein uberjar
RUN cp target/kifshare-standalone.jar .

RUN ln -s "/usr/bin/java" "/bin/kifshare"

ENTRYPOINT ["run-service", "kifshare", "-Dlogback.configurationFile=/etc/iplant/de/logging/kifshare-logging.xml", "-cp", ".:resources:kifshare-standalone.jar", "kifshare.core"]

ARG git_commit=unknown
ARG version=unknown
ARG descriptive_version=unknown

LABEL org.cyverse.git-ref="$git_commit"
LABEL org.cyverse.version="$version"
LABEL org.cyverse.descriptive-version="$descriptive_version"
LABEL org.label-schema.vcs-ref="$git_commit"
LABEL org.label-schema.vcs-url="https://github.com/cyverse-de/kifshare"
LABEL org.label-schema.version="$descriptive_version"
