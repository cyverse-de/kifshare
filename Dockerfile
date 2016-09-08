FROM clojure:alpine

RUN apk add --update git nodejs-lts && \
    rm -rf /var/cache/apk

VOLUME ["/etc/iplant/de"]

RUN npm install -g grunt-cli

ARG git_commit=unknown
ARG version=unknown
LABEL org.iplantc.de.kifshare.git-ref="$git_commit" \
      org.iplantc.de.kifshare.version="$version"

COPY . /usr/src/app
COPY conf/main/logback.xml /usr/src/app/logback.xml

WORKDIR /usr/src/app

RUN npm install && \
    grunt build-resources && \
    lein uberjar && \
    cp -r build/* resources/ && \
    cp target/kifshare-standalone.jar .

RUN ln -s "/usr/bin/java" "/bin/kifshare"

ENTRYPOINT ["kifshare", "-Dlogback.configurationFile=/etc/iplant/de/logging/kifshare-logging.xml", "-cp", ".:resources:kifshare-standalone.jar", "kifshare.core"]
CMD ["--help"]
