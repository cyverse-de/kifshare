FROM clojure:alpine

RUN apk add --update git nodejs-lts && \
    rm -rf /var/cache/apk

VOLUME ["/etc/iplant/de"]

RUN npm install -g grunt-cli

WORKDIR /usr/src/app

COPY project.clj /usr/src/app/
RUN lein deps

COPY conf/main/logback.xml /usr/src/app/
COPY . /usr/src/app


RUN npm install
RUN grunt build-resources
RUN cp -r build/* resources/
RUN lein uberjar
RUN cp target/kifshare-standalone.jar .

COPY ui/ui.xml resources/ui.xml

RUN ln -s "/usr/bin/java" "/bin/kifshare"

ENTRYPOINT ["kifshare", "-Dlogback.configurationFile=/etc/iplant/de/logging/kifshare-logging.xml", "-cp", ".:resources:kifshare-standalone.jar", "kifshare.core"]
CMD ["--help"]

ARG git_commit=unknown
ARG version=unknown

LABEL org.cyverse.git-ref="$git_commit"
LABEL org.cyverse.version="$version"
