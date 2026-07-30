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

  :dependencies [[org.clojure/clojure "1.12.5"]
                 [org.clojure/tools.logging "1.3.1"]
                 [ch.qos.logback/logback-classic "1.5.34"]
                 [net.logstash.logback/logstash-logback-encoder "9.0"]
                 [hawk "0.2.11"]
                 [hiccup "2.0.0"]
                 [medley "1.4.0"]
                 [org.cyverse/clj-jargon "3.1.5"
                  :exclusions [[org.slf4j/slf4j-log4j12]
                               [log4j]]]
                 [org.cyverse/debug-utils "2.9.0"]
                 [org.cyverse/clojure-commons "3.0.12"]
                 [org.cyverse/common-cli "2.8.2"]
                 [me.raynes/fs "1.4.6"]
                 [cheshire "6.2.0"]
                 [slingshot "0.12.2"]
                 [compojure "1.7.2" :exclusions [ring/ring-codec]]
                 [stencil "0.5.0"]
                 [com.cemerick/url "0.1.1" :exclusions [com.cemerick/clojurescript.test]]
                 [ring/ring-core "1.15.4"]
                 [ring/ring-jetty-adapter "1.15.4"]]

  :eastwood {:exclude-namespaces [:test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}

  :ring {:init kifshare.config/init
         :handler kifshare.core/app}

  :profiles {:dev     {:resource-paths ["build" "conf" "dev-resources"]}
             :uberjar {:aot :all}}

  :plugins [[jonase/eastwood "1.4.3"]
            [lein-ancient "0.7.0"]
            [lein-ring "0.12.6"]
            [test2junit "1.4.4"]]

  :main ^:skip-aot kifshare.core
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/kifshare-logging.xml"])
