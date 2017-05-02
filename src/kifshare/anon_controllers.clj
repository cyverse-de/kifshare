(ns kifshare.anon-controllers
  (:use [kifshare.config :only [jargon-config irods-anonymous-user]]
        [clojure-commons.error-codes])
  (:require [kifshare.ranges :as ranges]
            [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [clj-jargon.init :as jinit]
            [clj-jargon.item-info :as info]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.permissions :as perms]
            [clojure-commons.file-utils :as ft]
            [kifshare.errors :as errors]))

(defn- validation-info
  [cm filepath]
  (log/info "Validating anonymous access to" filepath)
  (cond
    (not (info/exists? cm filepath))
    :not-exists

    (not (info/is-file? cm filepath))
    :not-file

    (not (perms/is-readable? cm (irods-anonymous-user) filepath))
    :not-readable

    :else :ok))

(defmacro validated-with-jargon
  [filepath & params]
  (let [[opts [[cm-sym] & body]] (split-with #(not (vector? %)) params)]
    `(jinit/with-jargon (jargon-config) ~@opts [~cm-sym]
       (case (validation-info ~cm-sym ~filepath)
               :not-exists   {:status 404 :body (cheshire/encode {:error_code ERR_NOT_FOUND :message "Path not found."})}
               :not-file     {:status 403 :body (cheshire/encode {:error_code ERR_NOT_A_FILE :message "Path not a file."})}
               :not-readable {:status 403 :body (cheshire/encode {:error_code ERR_NOT_READABLE :message "Path not readable."})}
               :ok           (do ~@body)
               {:status 500 :body (cheshire/encode (unchecked {:message "Unknown file status."}))}))))

(defn handle-get
  [filepath req]
  (validated-with-jargon filepath :auto-close false [cm]
    (let [filesize (info/file-size cm filepath)]
    (if (and (ranges/range-request? req) (ranges/valid-range? req))
      (let [[start-byte end-byte] (ranges/extract-range req filesize)]
        (ranges/download-byte-range cm filepath filesize start-byte end-byte))
      (ranges/non-range-resp (ops/input-stream cm filepath) (ft/basename filepath) filesize)))))

(defn handle-head
  [filepath]
  (validated-with-jargon filepath [cm]
    (ranges/head-resp (ft/basename filepath) (info/file-size cm filepath))))

(defn handle-options
  [filepath]
  (validated-with-jargon filepath [cm]
    (ranges/options-resp)))
