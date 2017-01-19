(ns kifshare.inputs
  (:use [clj-jargon.init]
        [clj-jargon.item-ops]
        [clj-jargon.item-info]
        [clj-jargon.paging])
  (:import [java.io InputStream]))

(defn raw-chunk-stream
  "Produce a proxy InputStream for a given file path, which reads only from start-byte to end-byte. For use with range requests."
  [cm ^String filepath start-byte end-byte]
  (let [raf (random-access-file cm filepath)
        location (atom start-byte)]
    (.seek raf start-byte SEEK-CURRENT)
    (proxy [java.io.InputStream] []
      (available [] (.length raf))
      (mark [readlimit] nil)
      (markSupported [] nil)
      (read
        ([]
           (let [new-loc (inc @location)]
             (if (<= new-loc end-byte)
               (let [bufsize 1
                     buf     (byte-array bufsize)]
                 (.read raf buf 0 bufsize)
                 (reset! location new-loc)
                 (first buf))
               -1)))
        ([b]
           (if (<= @location end-byte)
             (let [diff       (inc (- end-byte @location))
                   len        (if (> (count b) diff)
                                diff
                                (count b))
                   bytes-read (.read raf b 0 len)]
               (reset! location (+ @location bytes-read))
               bytes-read)
             -1))
        ([b off len]
           (if (<= @location end-byte)
             (let [diff (inc (- end-byte @location))
                   len (if (> len diff)
                         diff
                         len)
                   bytes-read (.read raf b off len)]
               (reset! location (+ @location bytes-read))
               bytes-read)
             -1)))
      (reset [] (.seek raf 0 SEEK-START))
      (skip [n] (.skipBytes raf n))
      (close []
        (.close raf)))))

(defn chunk-stream
  "Same as raw-chunk-stream, but uses proxy-input-stream to automatically close the context-manager when it's done"
  [cm ^String filepath start-byte end-byte]
  (proxy-input-stream cm (raw-chunk-stream cm filepath start-byte end-byte)))
