(ns kifshare.tickets-controllers
  (:use [ring.util.response :only [redirect]]
        [kifshare.config :only [jargon-config]]
        [kifshare.ui-template :only [landing-page]]
        [slingshot.slingshot :only [try+]]
        [clojure-commons.error-codes])
  (:require [kifshare.tickets :as tickets]
            [kifshare.ranges :as ranges]
            [kifshare.common :as common]
            [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [clj-jargon.metadata :as jmeta]
            [clj-jargon.init :as jinit]
            [kifshare.errors :as errors]))

(defn- object-metadata
  [cm abspath]
  (log/debug "kifshare.controllers/object-metadata")

  (future (filterv
   #(not= (:unit %1) "ipc-system-avu")
   (jmeta/get-metadata cm abspath))))

(defn show-landing-page
  "Handles error checking and decides whether to show the
   landing page or an error page."
  [cm ticket-id ticket-info]
  (log/debug "entered kifshare.controllers/show-landing-page")
  (landing-page
   ticket-id
   (object-metadata cm (:abspath ticket-info))
   ticket-info))

(defn error-map-response
  [request err-map]
  (if (common/show-html? request)
    (errors/error-html err-map)
    (errors/error-response err-map)))

(defn get-ticket
  "Determines whether to redirect to a download or show the landing page."
  [ticket-id ring-request]
  (log/debug "entered page kifshare.controllers/get-ticket")

  (jinit/with-jargon (jargon-config) [cm]
    (try+
     ; check-ticket is called by ticket-info
     (let [ticket-info (tickets/ticket-info cm ticket-id)]
       (log/debug "Ticket Info:\n" ticket-info)
       {:status 200 :body (show-landing-page cm ticket-id ticket-info)})

     (catch error? err
       (log/error (format-exception (:throwable &throw-context)))
       (error-map-response ring-request err))

     (catch Exception _
       (log/error (format-exception (:throwable &throw-context)))
       (errors/error-response (unchecked &throw-context))))))

(defn download-range
  [cm ticket-id ring-request]
  ; check-ticket is called by ticket-info
  (let [ticket-info (tickets/ticket-info cm ticket-id)
        [start-byte end-byte] (ranges/extract-range ring-request (:filesize ticket-info))]
    (tickets/download-byte-range cm ticket-info start-byte end-byte)))

(defn download-file
  "Allows the caller to download a file associated with a ticket."
  [ticket-id filename ring-request]
  (log/debug "entered page kifshare.controllers/download-file")

  (try+
    (log/info "Downloading " ticket-id " as " filename)
    (if (and (ranges/range-request? ring-request) (ranges/valid-range? ring-request))
      (jinit/with-jargon (jargon-config) :auto-close false [cm]
        (download-range cm ticket-id ring-request))
      (jinit/with-jargon (jargon-config) :auto-close false [cm]
        (tickets/download cm ticket-id)))

    (catch error? err
      (log/error (format-exception (:throwable &throw-context)))
      (error-map-response ring-request err))

    (catch Exception _
      (log/error (format-exception (:throwable &throw-context)))
      {:status 500 :body (cheshire/encode (unchecked &throw-context))})))

(defn download-ticket
  "Redirects the caller to the endpoint that allows them to download a ticket."
  [ticket-id ring-request]
  (log/debug "entered page kifshare.controllers/download-ticket")

  (try+
   (jinit/with-jargon (jargon-config) [cm]
     (let [ticket-info (tickets/ticket-info cm ticket-id)]
       (log/warn "Redirecting download for " ticket-id " to the /d/:ticket-id/:filename page.")
       (redirect (str "../d/" ticket-id "/" (:filename ticket-info)))))

   (catch error? err
     (log/error (format-exception (:throwable &throw-context)))
     (error-map-response ring-request err))

   (catch Exception _
     (log/error (format-exception (:throwable &throw-context)))
     {:status 500 :body (cheshire/encode (unchecked &throw-context))})))

(defn file-info
  [ticket-id]
  (jinit/with-jargon (jargon-config) [cm]
    (let [ticket-info (tickets/ticket-info cm ticket-id)]
      (ranges/head-resp (:filename ticket-info) (:filesize ticket-info)))))

(defn file-options
  [ticket-id]
  (jinit/with-jargon (jargon-config) [cm]
    (let [ticket-info (tickets/ticket-info cm ticket-id)]
      (ranges/options-resp))))
