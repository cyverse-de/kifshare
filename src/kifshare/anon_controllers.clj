(ns kifshare.anon-controllers
  (:use [kifshare.config :only [jargon-config irods-anonymous-user]]
        [clojure-commons.error-codes])
  (:require [kifshare.ranges :as ranges]
            [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [clj-jargon.init :as jinit]
            [clj-jargon.item-info :as info]
            [clj-jargon.permissions :as perms]
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

(defn handle-options
  [filepath]
  (jinit/with-jargon (jargon-config) [cm]
    (case (validation-info cm filepath)
      :not-exists   {:status 404 :body (cheshire/encode {:error_code ERR_NOT_FOUND :message "Path not found."})}
      :not-file     {:status 403 :body (cheshire/encode {:error_code ERR_NOT_A_FILE :message "Path not a file."})}
      :not-readable {:status 403 :body (cheshire/encode {:error_code ERR_NOT_READABLE :message "Path not readable."})}
      :ok           (ranges/options-resp)
      {:status 500 :body (cheshire/encode (unchecked {:message "Unknown file status."}))})))
