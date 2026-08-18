(require '[clojure.java.shell :refer (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.cyverse/kifshare "3.0.1-SNAPSHOT"
  :description "CyVerse Quickshare for iRODS"
  :url "https://github.com/cyverse-de/kifshare"

  :license {:name "BSD"
            :url "http://cyverse.org/sites/default/files/iPLANT-LICENSE.txt"}

  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "kifshare-standalone.jar"

  ;; Fail the build on a new dependency conflict rather than printing a
  ;; warning nobody reads.
  :pedantic? :abort
  ;; Records versions Leiningen already resolves, read off the resolved
  ;; classpath rather than copied from lein's "Consider using these
  ;; :managed-dependencies" hint -- that hint names the version that LOST the
  ;; conflict, so pasting it would be a silent upgrade.
  ;;
  ;; The jackson-* entries hold the family where clj-jargon puts it. jargon-core
  ;; is pinned :upgrade false at 4.3.7.0-RELEASE for iRODS compatibility and
  ;; brings jackson 2.14.1. jackson-core sits higher because cheshire needs it
  ;; there -- cheshire 6 throws at runtime against jackson-core 2.14.1. This
  ;; asymmetry is deliberate and matches what main already resolved; do not
  ;; "tidy" it by dropping core to match the rest. Unifying means moving jargon.
  :managed-dependencies [[com.fasterxml.jackson.core/jackson-databind "2.14.1"]
                         [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.14.1"]
                         [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.14.1"]
                         [commons-codec "1.15"]
                         [org.apache.commons/commons-compress "1.8"]
                         [org.clojure/core.cache "0.6.3"]
                         [org.clojure/data.priority-map "0.0.2"]
                         [prismatic/schema "1.1.12"]]
  :dependencies [[org.clojure/clojure "1.12.5"]
                 [org.clojure/tools.logging "1.3.1"]
                 [ch.qos.logback/logback-classic "1.5.34"]
                 [net.logstash.logback/logstash-logback-encoder "9.0"]
                 [hawk "0.2.11"]
                 [hiccup "2.0.0"]
                 [medley "1.4.0"]
                 [org.cyverse/clj-jargon "3.1.6"
                  :exclusions [[org.slf4j/slf4j-log4j12]
                               [log4j]]]
                 [org.cyverse/debug-utils "2.9.1"]
                 [org.cyverse/clojure-commons "3.0.13"]
                 [org.cyverse/common-cli "2.8.3"]
                 [me.raynes/fs "1.4.6"]
                 [cheshire "6.2.0"]
                 [slingshot "0.12.2"]
                 [compojure "1.7.2" :exclusions [ring/ring-codec]]
                 [stencil "0.5.0"]
                 [com.cemerick/url "0.1.1" :exclusions [com.cemerick/clojurescript.test]]
                 [ring/ring-core "1.15.5"]
                 [ring/ring-jetty-adapter "1.15.5"]]

  :eastwood {:exclude-namespaces [:test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}

  :ring {:init kifshare.config/init
         :handler kifshare.core/app}

  :profiles {:dev     {:resource-paths ["build" "conf" "dev-resources"]}
             :uberjar {:aot :all}}

  :plugins [[jonase/eastwood "1.4.3"]
            [lein-ancient "1.0.0"]
            [lein-ring "0.12.6"]
            [test2junit "1.4.4"]]

  :main ^:skip-aot kifshare.core
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/kifshare-logging.xml"])
