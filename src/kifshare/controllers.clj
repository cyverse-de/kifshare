(ns kifshare.controllers
  (:use [ring.util.response :only [redirect]]
        [kifshare.config :only [jargon-config]]
        [kifshare.ui-template :only [landing-page]]
        [slingshot.slingshot :only [try+]]
        [clojure-commons.error-codes])
  (:require [kifshare.tickets :as tickets]
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

(defn range-request?
  [ring-request]
  (and (contains? ring-request :headers)
       (contains? (:headers ring-request) "range")))

;; begins with bytes=; then a start-end range (where excluding either number works, but not both)
;; regex lookahead/lookbehind to ensure only one number is missing, not both
(def ^:private range-regex #"\s*(bytes)\s*=\s*([0-9]+|(?=-\d))\-([0-9]+|(?<=\d-))\s*")

(defn valid-range?
  [ring-request]
  (re-matches range-regex (get-in ring-request [:headers "range"])))

(defn- longify
  "Turn the argument to a long if it's relatively easy to do so (integral numbers and strings)"
  [str-long]
  (if (integer? str-long)
    (long str-long)
    (Long/parseLong str-long)))

(defn- extract-range
  "Extract start and end bytes from the Range header"
  [ring-request filesize]
  (let [range-header  (get-in ring-request [:headers "range"])
        range-matches (re-matches range-regex range-header)
        last-byte     (- (longify filesize) 1)
        start?        (seq (nth range-matches 2))
        end?          (seq (nth range-matches 3))
        ;; "N-" means N-EOF; "-M" means (EOF-M)-EOF, so:
        ;; Start is as specified, or specified number of bytes returned (i.e. filesize - specified)
        ;; End is as specified only if both are specified, otherwise last byte
        start         (if start?
                        (longify (nth range-matches 2))
                        (- (longify filesize) (longify (nth range-matches 3))))
        end           (if (and end? start?)
                        (longify (nth range-matches 3))
                        last-byte)]
    [(max 0 start) (min end last-byte)]))

(defn download-range
  [cm ticket-id ring-request]
  ; check-ticket is called by ticket-info
  (let [ticket-info (tickets/ticket-info cm ticket-id)
        [start-byte end-byte] (extract-range ring-request (:filesize ticket-info))]
    (tickets/download-byte-range cm ticket-info start-byte end-byte)))

(defn download-file
  "Allows the caller to download a file associated with a ticket."
  [ticket-id filename ring-request]
  (log/debug "entered page kifshare.controllers/download-file")

  (try+
    (log/info "Downloading " ticket-id " as " filename)
    (if (and (range-request? ring-request) (valid-range? ring-request))
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
      {:status  200
       :headers {"Content-Length"      (str (:filesize ticket-info))
                 "Content-Disposition" (str "filename=\"" (:filename ticket-info) "\"")
                 "Accept-Ranges"       "bytes"}})))
