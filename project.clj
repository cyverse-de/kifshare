(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.cyverse/kifshare "2.12.0-SNAPSHOT"
  :description "CyVerse Quickshare for iRODS"
  :url "https://github.com/cyverse-de/kifshare"

  :license {:name "BSD"
            :url "http://cyverse.org/sites/default/files/iPLANT-LICENSE.txt"}

  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "kifshare-standalone.jar"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [hawk "0.2.11"]
                 [medley "0.5.5"]
                 [org.cyverse/clj-jargon "3.0.0"
                   :exclusions [[org.slf4j/slf4j-log4j12]
                                [log4j]]]
                 [org.cyverse/debug-utils "2.8.1"]
                 [org.cyverse/service-logging "2.8.2"]
                 [net.logstash.logback/logstash-logback-encoder "4.11"]
                 [org.cyverse/clojure-commons "2.8.0"]
                 [org.cyverse/common-cli "2.8.1"]
                 [org.cyverse/event-messages "0.0.1"]
                 [com.novemberain/langohr "3.5.1"]
                 [me.raynes/fs "1.4.6"]
                 [cheshire "5.5.0"
                   :exclusions [[com.fasterxml.jackson.dataformat/jackson-dataformat-cbor]
                                [com.fasterxml.jackson.dataformat/jackson-dataformat-smile]
                                [com.fasterxml.jackson.core/jackson-annotations]
                                [com.fasterxml.jackson.core/jackson-databind]
                                [com.fasterxml.jackson.core/jackson-core]]]
                 [slingshot "0.12.2"]
                 [compojure "1.5.0"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 [com.cemerick/url "0.1.1" :exclusions [com.cemerick/clojurescript.test]]
                 [ring-logger "1.0.1"]]

  :eastwood {:exclude-namespaces [:test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}

  :ring {:init kifshare.config/init
         :handler kifshare.core/app}

  :profiles {:dev     {:resource-paths ["build" "conf" "dev-resources"]}
             :uberjar {:aot :all}}

  :plugins [[jonase/eastwood "0.2.3"]
            [test2junit "1.2.2"]
            [lein-ring "0.7.5"]]

  :main ^:skip-aot kifshare.core
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/kifshare-logging.xml"])
