(ns kifshare.ranges
  (:require [clojure.tools.logging :as log]
            [kifshare.inputs :refer [chunk-stream]]))

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

(defn extract-range
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

(defn head-resp
  [filename filesize]
  {:status  200
   :headers {"Content-Length"      (str filesize)
             "Content-Disposition" (str "filename=\"" filename "\"")
             "Accept-Ranges"       "bytes"}})

(defn options-resp
  []
  {:status  200
   :headers {"Accept-Ranges"       "bytes"
             "Allow"               "GET, HEAD"}})

(defn unsatisfiable-resp
  [filesize]
  {:status 416
   :body "The requested range is not satisfiable."
   :headers {"Content-Range" (str "bytes */" filesize)
             "Accept-Ranges" "bytes"}})

(defn range-resp
  [body filesize start-byte end-byte]
  {:status 206
   :body   body
   :headers
   {"Content-Range" (str "bytes " start-byte "-" end-byte "/" filesize)
    "Accept-Ranges" "bytes"}})

(defn download-byte-range
  "Returns a response map containing a byte range from a file. Assumes validation has already been performed."
  [cm filename filesize start-byte end-byte]
  (log/debug "entered kifshare.ranges/download-byte-range")

  (if (or (> start-byte end-byte)
          (>= start-byte filesize))
    (unsatisfiable-resp filesize)
    (do
      (log/warn "Download file range:" start-byte "-" end-byte "for file" filename)
      (range-resp (chunk-stream cm filename start-byte end-byte) filesize start-byte end-byte))))
