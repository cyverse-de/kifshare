(ns kifshare.ui-template
  (:require [kifshare.config :as cfg]
            [clostache.parser :as prs]
            [clojure.tools.logging :as log]
            [cheshire.core :as json])
  (:import [org.apache.commons.io FileUtils]))

(def tmpl (ref ""))

(defn read-template
  []
  (dosync
    (ref-set tmpl (slurp "resources/ui.xml"))))

(defn ui-ticket-info
  [ticket-info]
  (assoc ticket-info
    :import_template (cfg/de-import-flags)
    :wget_template   (cfg/wget-flags)
    :curl_template   (cfg/curl-flags)
    :iget_template   (cfg/iget-flags)))

(defn landing-page
  [ticket-id metadata-promise ticket-info]
  (log/debug "entered kifshare.ui-template/landing-page")
  (prs/render @tmpl
              (assoc ticket-info
                     :metadata @metadata-promise
                     :filesize (FileUtils/byteCountToDisplaySize
                                (Long/parseLong (:filesize ticket-info)))
                     :irods-url (cfg/irods-url)
                     :de-url (cfg/de-url)
                     :ticket-info-json (json/generate-string
                                        (ui-ticket-info ticket-info)))))
