(ns kifshare.ui-template
  (:require [kifshare.config :as cfg]
            [kifshare.ranges :as ranges]
            [clojure.tools.logging :as log]
            [stencil.core :as stencil]
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

(defn template-values
  "Builds the substitution map for the landing page template. download-path is pre-URL-encoded
   for safe use in an href; ticket-info-json keeps the raw filename and the client-side Mustache
   templates, which the page's JavaScript renders after parsing the JSON."
  [ticket-id metadata ticket-info de-url irods-url]
  (assoc ticket-info
         :metadata         metadata
         :filesize         (FileUtils/byteCountToDisplaySize (Long/parseLong (:filesize ticket-info)))
         :irods-url        irods-url
         :de-url           de-url
         :download-path    (ranges/url-encode-path (str "d/" ticket-id "/" (:filename ticket-info)))
         :ticket-info-json (json/generate-string (ui-ticket-info ticket-info))))

(defn render-page
  "Renders the landing page template with Mustache, HTML-escaping interpolated values."
  [template ticket-id metadata ticket-info de-url irods-url]
  (stencil/render-string template (template-values ticket-id metadata ticket-info de-url irods-url)))

(defn landing-page
  [ticket-id metadata-promise ticket-info]
  (log/debug "entered kifshare.ui-template/landing-page")
  (render-page @tmpl ticket-id @metadata-promise ticket-info (cfg/de-url) (cfg/irods-url)))
