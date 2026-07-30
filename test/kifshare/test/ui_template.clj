(ns kifshare.test.ui-template
  (:use [clojure.test])
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [kifshare.config :as config]
            [kifshare.ui-template :as ui]))

(use-fixtures :once
  (fn [f]
    (require 'kifshare.config :reload)
    (config/local-init "dev-resources/mostly-defaults.properties")
    (f)))

(def ^:private ticket-info
  {:ticket-id "tkt1"
   :abspath   "/iplant/home/user/a<x>ä.txt"
   :filename  "a<x>ä.txt"
   :filesize  "1024"
   :lastmod   "12345"
   :useslimit "0"
   :remaining "0"})

(defn- values []
  (ui/template-values "tkt1" [] ticket-info "https://de.example.org" "https://irods.example.org"))

(deftest test-download-path-is-url-encoded
  (testing "the download path percent-encodes the filename (and other URL-significant characters)"
    (is (= "d/tkt1/a%3Cx%3E%C3%A4.txt" (:download-path (values))))))

(deftest test-ticket-info-json-carries-raw-filename-and-templates
  (testing "the embedded JSON keeps the raw filename and the client-side Mustache templates"
    (let [parsed (json/parse-string (:ticket-info-json (values)) true)]
      (is (= "a<x>ä.txt" (:filename parsed)))
      (is (string/includes? (:wget_template parsed) "{{filename}}")))))

(deftest test-filesize-is-human-readable
  (testing "filesize is converted to a display string"
    (is (= "1 KB" (:filesize (values))))))

(deftest test-render-page-escapes-filename-for-display
  (testing "the rendered title HTML-escapes the filename rather than emitting raw markup"
    (let [out (ui/render-page (slurp "ui/ui.xml") "tkt1" [] ticket-info
                              "https://de.example.org" "https://irods.example.org")]
      (is (string/includes? out "<title>a&lt;x&gt;ä.txt</title>"))
      (is (not (string/includes? out "a<x>ä.txt"))))))

(deftest test-render-page-encodes-download-link
  (testing "the rendered download link uses the percent-encoded path"
    (let [out (ui/render-page (slurp "ui/ui.xml") "tkt1" [] ticket-info
                              "https://de.example.org" "https://irods.example.org")]
      (is (string/includes? out "href=\"d/tkt1/a%3Cx%3E%C3%A4.txt\"")))))

(deftest test-render-page-renders-metadata-section
  (testing "the metadata Mustache section iterates and escapes values"
    (let [out (ui/render-page (slurp "ui/ui.xml") "tkt1"
                              [{:attr "creator" :value "v<1" :unit "u"}] ticket-info
                              "https://de.example.org" "https://irods.example.org")]
      (is (string/includes? out "<td title=\"creator\">creator</td>"))
      (is (string/includes? out "v&lt;1")))))
