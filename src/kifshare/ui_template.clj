(ns kifshare.ui-template
  (:use [hiccup.core]
        [hiccup.page :only [include-css include-js html5]]
        [kifshare.common :only [layout]])
  (:require [kifshare.config :as cfg]
            [clostache.parser :as prs]
            [clojure.tools.logging :as log]
            [cheshire.core :as json])
  (:import [org.apache.commons.io FileUtils]))

(defn read-template
  []
  (slurp "resources/ui.xml"))

(def memo-read-template
  (memoize read-template))

(defn ui-ticket-info
  [ticket-info]
  (assoc ticket-info
    :import_template (cfg/de-import-flags)
    :wget_template   (cfg/wget-flags)
    :curl_template   (cfg/curl-flags)
    :iget_template   (cfg/iget-flags)))

(defn landing-page
  [ticket-id metadata ticket-info]
  (log/debug "entered kifshare.ui-template/landing-page")
  (prs/render (memo-read-template)
              (assoc ticket-info
                     :metadata metadata
                     :filesize (FileUtils/byteCountToDisplaySize
                                (Long/parseLong (:filesize ticket-info)))
                     :ticket-info-json (json/generate-string
                                        (ui-ticket-info ticket-info)))))
