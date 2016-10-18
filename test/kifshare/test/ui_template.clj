(ns kifshare.test.ui-template
  (:use [kifshare.ui-template]
        [hiccup.core]
        [clojure.test])
  (:require [kifshare.config :as cfg]))

(deftest clear-test
    (is (= (clear) "<div class=\"clear\"></div>") "clear div"))

(deftest section-spacer-test
    (is (= (section-spacer) "<div class=\"section-spacer\"></div>") "Section spacer"))

(deftest irods-avu-row-test
    (is (=
      (irods-avu-row {:attr "attr" :value "value" :unit "unit"})
      "<tr><td title=\"attr\">attr</td><td title=\"value\">value</td><td title=\"unit\">unit</td></tr>")))

(deftest irods-avu-table-test
    (is (=
      (irods-avu-table [{:attr "attr" :value "value" :unit "unit"}])
      (str "<div id=\"irods-avus\">"
             "<div id=\"irods-avus-header\">"
               "<h2>Metadata</h2>"
             "</div>"
             "<table id=\"irods-avus-data\">"
               "<thead>"
                 "<tr>"
                   "<th>Attribute</th>"
                   "<th>Value</th>"
                   "<th>Unit</th>"
                 "</tr>"
               "</thead>"
               "<tbody>"
                 (irods-avu-row {:attr "attr" :value "value" :unit "unit"}) ;; tested above, no need to double-test
               "</tbody>"
             "</table>"
             "<div class=\"section-spacer\"></div>"
           "</div>"))))

(deftest lastmod-test
    (is (=
      (lastmod {:lastmod "foo-blippy-bar"})
      (str "<div id=\"lastmod-detail\">"
             "<div id=\"lastmod-label\">"
               "<p>Last Modified:</p>"
             "</div>"
             "<div id=\"lastmod\">"
               "<p>foo-blippy-bar</p>"
             "</div>"
           "</div>"))))

(deftest filesize-test
    (is (=
      (filesize {:filesize "1024"})
      (str "<div id=\"size-detail\">"
             "<div id=\"size-label\">"
               "<p>File Size:</p>"
             "</div>"
             "<div id=\"size\">"
               "<p>1 KB</p>"
             "</div>"
             "</div>"))))

(deftest ui-ticket-info-test
    (is (=
      (with-redefs [cfg/de-import-flags #(str "import flags!")
                    cfg/wget-flags #(str "wget flags!")
                    cfg/curl-flags #(str "curl flags!")
                    cfg/iget-flags #(str "iget flags!")]
        (ui-ticket-info {}))
      {:import_template "import flags!"
       :wget_template "wget flags!"
       :curl_template "curl flags!"
       :iget_template "iget flags!"})))

(deftest template-map-test
    (with-redefs [cfg/de-import-flags #(str "import flags!")
                  cfg/wget-flags #(str "wget flags!")
                  cfg/curl-flags #(str "curl flags!")
                  cfg/iget-flags #(str "iget flags!")]
      (is (re-matches
        (re-pattern (str
                     "<span id=\"ticket-info\" style=\"display: none;\">"
                     "<div id=\"ticket-info-map\">"
                     ".*"
                     "</div>"
                     "</span>"))
        (template-map {})))))

(deftest input-display-test
    (is (=
      (input-display "foo")
      "<input id=\"foo\" type=\"text\" value=\"\" />")))

(deftest irods-instr-test
  (with-redefs [cfg/irods-url #(str "foo")]
    (is (=
      (irods-instr {})
      (str
       "<div id=\"irods-instructions\">"
         "<div id=\"irods-instructions-label\">"
           "<h2 title=\"iRODS icommands\"><a href=\"foo\">iRODS icommands</a>:</h2>"
         "</div>"
         "<div id=\"clippy-irods-instructions\">"
           "<input id=\"irods-command-line\" type=\"text\" value=\"\" />"
           "<span title=\"copy to clipboard\">"
             "<button class=\"clippy-irods\" id=\"clippy-irods-wrapper\" title=\"Copy\">Copy</button>"
           "</span>"
         "</div>"
       "</div>")))))

(deftest de-import-instr-test
  (with-redefs [cfg/de-url #(str "foo")]
    (is (=
      (de-import-instr {})
      (str
       "<div id=\"de-import-instructions\">"
         "<div id=\"de-import-instructions-label\">"
           "<h2 title=\"Discovery Environment Import URL\"><a href=\"foo\">DE Import URL</a>:</h2>"
         "</div>"
         "<div id=\"clippy-import-instructions\">"
           "<input id=\"de-import-url\" type=\"text\" value=\"\" />"
           "<span title=\"copy to clipboard\">"
             "<button class=\"clippy-import\" id=\"clippy-import-wrapper\" title=\"Copy\">Copy</button>"
           "</span>"
         "</div>"
       "</div>")))))

(deftest downloader-instr-test
    (is (=
      (downloader-instr "lol-id" {})
      (str
       "<div id=\"wget-instructions\">"
         "<div id=\"wget-instructions-label\">"
           "<p>Wget:</p>"
         "</div>"
         "<div id=\"clippy-wget-instructions\">"
           "<input id=\"wget-command-line\" type=\"text\" value=\"\" />"
           "<span title=\"copy to clipboard\">"
             "<button class=\"clippy-wget\" id=\"clippy-wget-wrapper\" title=\"Copy\">Copy</button>"
           "</span>"
         "</div>"
       "</div>"
       "<div id=\"curl-instructions\">"
         "<div id=\"curl-instructions-label\">"
           "<p>cURL:</p>"
         "</div>"
         "<div id=\"clippy-curl-instructions\">"
           "<input id=\"curl-command-line\" type=\"text\" value=\"\" />"
           "<span title=\"copy to clipboard\">"
             "<button class=\"clippy-curl\" id=\"clippy-curl-wrapper\" title=\"Copy\">Copy</button>"
           "</span>"
         "</div>"
         "</div>"))))

(deftest menu-test
  (with-redefs [cfg/logo-path (fn [] "/tmp/logo-path")]
    (is (=
      (menu {:filename "foo" :ticket-id "a-ticket"}))
      (str
       "<div id=\"menu\">"
         "<ul>"
           "<li>"
             "<div id=\"logo-container\">"
               "<img id=\"logo\" src=\"/tmp/logo-path\" />"
             "</div>"
           "</li>"
           "<li>"
             "<div>"
               "<h1 id=\"filename\" title=\"foo\">foo</h1>"
             "</div>"
           "</li>"
           "<li>"
             "<div id=\"download-container\">"
               "<a href=\"d/a-ticket/foo\" id=\"download-link\">"
                 "<div id=\"download-link-area\">Download!</div>"
               "</a>"
             "</div>"
           "</li>"
         "</ul>"
         "</div>"))))

(deftest details-test
  (is (re-matches
    (re-pattern
     (str
      "<div id=\"details\">"
        "<a name=\"details-section\"></a>"
        "<div id=\"details-header\">"
          "<h2>File Details</h2>"
          ".*"
        "</div>"
        "<div class=\"section-spacer\"></div>"
      "</div>"))
    (html (details {:filesize "1024" :lastmod "1024"})))))

(deftest alt-downloads-test
  (is (re-matches
    (re-pattern
     (str
      "<div id=\"alt-downloads-header\">"
        "<h2>Alternative Download Methods</h2>"
      "</div>"
      "<div id=\"alt-downloads\">"
        "<div id=\"de-import-instructions\">"
          ".*"
        "</div>"
        "<div id=\"irods-instructions\">"
          ".*"
        "</div>"
        "<div id=\"wget-instructions\">"
          ".*"
        "</div>"
        "<div id=\"curl-instructions\">"
          ".*"
        "</div>"
      "</div>"))
    (alt-downloads {}))))

(deftest footer-test
  (with-redefs [cfg/footer-text #(str "footer!")]
    (is (=
    (footer)
    (str "<div id=\"footer\">"
           "<p>footer!</p>"
         "</div>")))))

#_(fact "twitter title"
      (twitter-title {:filename "filename"} {}) => "filename")

#_(fact "twitter description"
      (twitter-description {:filesize "1024" :lastmod "last!"} {}) =>
      "File Size: 1 KB\nLast Modified: last!")


#_(fact "twitter card"
  (twitter-card {:filesize "1024" :lastmod "last!" :filename "filename"} {}) =>
  (str "<meta content=\"summary\" name=\"twitter:card\" />"
       "<meta content=\"filename\" name=\"twitter:title\" />"
       "<meta content=\"File Size: 1 KB\nLast Modified: last!\" name=\"twitter:description\" />"))
