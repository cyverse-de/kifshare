(ns kifshare.ui-template
  (:require [kifshare.config :as cfg]
            [clostache.parser :as prs]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [ring.util.io :as ring-io])
  (:import [org.apache.commons.io FileUtils]))

(def headtmpl (ref ""))
(defn read-head-template []
  (dosync (ref-set headtmpl (slurp "resources/head.xml"))))

(def pre-avustmpl (ref ""))
(defn read-pre-avus-template []
  (dosync (ref-set pre-avustmpl (slurp "resources/pre-avus.xml"))))

(def avus-and-footertmpl (ref ""))
(defn read-avus-and-footer-template []
  (dosync (ref-set avus-and-footertmpl (slurp "resources/avus-and-footer.xml"))))

(defn ui-ticket-info
  [ticket-info]
  (assoc ticket-info
    :import_template (cfg/de-import-flags)
    :wget_template   (cfg/wget-flags)
    :curl_template   (cfg/curl-flags)
    :iget_template   (cfg/iget-flags)))

(defn landing-page
  [ticket-id metadata-promise ticket-info-promise]
  (log/debug "entered kifshare.ui-template/landing-page")
  (ring-io/piped-input-stream
    (fn [ostream] (with-open [writer (io/make-writer ostream {})]
      (.write writer (prs/render @headtmpl))
      (.flush writer)
      (let [base-info @ticket-info-promise
            ticket-info (assoc @ticket-info-promise
                               :filesize (FileUtils/byteCountToDisplaySize
                                          (Long/parseLong (:filesize base-info)))
                               :irods-url (cfg/irods-url)
                               :de-url (cfg/de-url)
                               :ticket-info-json (json/generate-string
                                                  (ui-ticket-info base-info)))]
        (.write writer (prs/render @pre-avustmpl ticket-info))
        (.flush writer)
        (.write writer (prs/render @avus-and-footertmpl (assoc ticket-info
                                                                 :metadata @metadata-promise)))
        (.flush writer))))))
