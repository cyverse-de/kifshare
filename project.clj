(use '[clojure.java.shell :only (sh)])
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

  :dependencies [[org.clojure/clojure "1.11.4"]
                 [org.clojure/tools.logging "1.3.0"]
                 [ch.qos.logback/logback-classic "1.5.6"]
                 [net.logstash.logback/logstash-logback-encoder "8.0"]
                 [hawk "0.2.11"]
                 [hiccup "1.0.2"]
                 [medley "1.4.0"]
                 [org.cyverse/clj-jargon "3.1.1"
                   :exclusions [[org.slf4j/slf4j-log4j12]
                                [log4j]]]
                 [org.cyverse/debug-utils "2.9.0"]
                 [org.cyverse/clojure-commons "3.0.9"]
                 [org.cyverse/common-cli "2.8.2"]
                 [org.cyverse/event-messages "0.0.1"]
                 [com.novemberain/langohr "3.5.1"]
                 [me.raynes/fs "1.4.6"]
                 [cheshire "5.13.0"
                   :exclusions [[com.fasterxml.jackson.dataformat/jackson-dataformat-cbor]
                                [com.fasterxml.jackson.dataformat/jackson-dataformat-smile]
                                [com.fasterxml.jackson.core/jackson-annotations]
                                [com.fasterxml.jackson.core/jackson-databind]
                                [com.fasterxml.jackson.core/jackson-core]]]
                 [slingshot "0.12.2"]
                 [compojure "1.7.1"]
                 [com.cemerick/url "0.1.1" :exclusions [com.cemerick/clojurescript.test]]
                 [ring/ring-core "1.12.2"]]

  :eastwood {:exclude-namespaces [:test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}

  :ring {:init kifshare.config/init
         :handler kifshare.core/app}

  :profiles {:dev     {:resource-paths ["build" "conf" "dev-resources"]
                       :jvm-opts ["-Dotel.javaagent.enabled=false"]}
             :uberjar {:aot :all}}

  :plugins [[jonase/eastwood "1.4.3"]
            [lein-ancient "0.7.0"]
            [lein-ring "0.12.6"]
            [test2junit "1.4.4"]]

  :main ^:skip-aot kifshare.core
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/kifshare-logging.xml"
             "-javaagent:./opentelemetry-javaagent.jar"
             "-Dotel.resource.attributes=service.name=kifshare"])
