(ns kifshare.config
  (:require [clojure.string :as string]
            [clj-jargon.init :as jinit]
            [clojure-commons.props :as prps]
            [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]
            [clojure.tools.logging :as log]))

(def default-css-files
  "resources/css/reset.css,
   resources/css/960.css,
   resources/css/kif.css")

(def default-javascript-files
  "https://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js,
   https://cdnjs.cloudflare.com/ajax/libs/mustache.js/0.7.0/mustache.min.js,
   resources/js/json2.js,
   resources/js/kif.js")

(def props (atom nil))

(def robots-txt (atom ""))

(defn robots-txt-path
  []
  (or (get @props "kifshare.app.robots-txt")
      "robots.txt"))

(defn client-cache-scope
  []
  (or (get @props "kifshare.app.client-cache-scope")
      "public"))

(defn client-cache-max-age
  []
  (or (get @props "kifshare.app.client-cache-max-age")
      "604800"))

(defn robots-txt-content
  []
  @robots-txt)

(defn local-init
  [local-config-path]
  (let [main-props (prps/read-properties local-config-path)]
    (reset! props main-props)))

(defn resources-root
  []
  (or (get @props "kifshare.app.resources-root")
      "/resources"))

(defn js-dir
  []
  (or (get @props "kifshare.app.js-dir")
      "js"))

(defn img-dir
  []
  (or (get @props "kifshare.app.images-dir")
      "img"))

(defn css-dir
  []
  (or (get @props "kifshare.app.css-dir")
      "css"))

(defn flash-dir
  []
  (or (get @props "kifshare.app.flash-dir")
      "flash"))

(defn de-url
  []
  (get @props "kifshare.app.de-url"))

(defn irods-url
  []
  (get @props "kifshare.app.irods-url"))

(defn logo-path
  []
  (or (get @props "kifshare.app.logo-path")
      "resources/img/logo.png"))

(defn favicon-path
  []
  (or (get @props "kifshare.app.favicon-path")
      "resources/img/logo.ico"))

(defn de-import-flags
  []
  (or (get @props "kifshare.app.de-import-flags")
      "{{url}}/d/{{ticket-id}}/{{filename}}"))

(defn footer-text
  []
  (get @props "kifshare.app.footer-text"))

(defn curl-flags
  []
  (or (get @props "kifshare.app.curl-flags")
      "curl -o '{{filename}}' '{{url}}/d/{{ticket-id}}/{{filename}}'"))

(defn wget-flags
  []
  (or (get @props "kifshare.app.wget-flags")
      "wget '{{url}}/d/{{ticket-id}}/{{filename}}'"))

(defn iget-flags
  []
  (or (get @props "kifshare.app.iget-flags")
      "iget -t {{ticket-id}} '{{abspath}}'"))

(defn username
  []
  (or (get @props "kifshare.irods.user")
      "public"))

(defn irods-host
  []
  (or (get @props "kifshare.irods.host") "irods"))

(defn irods-port
  []
  (or (get @props "kifshare.irods.port") "1247"))

(defn irods-password
  []
  (or (get @props "kifshare.irods.password") "notprod"))

(defn irods-home
  []
  (or (get @props "kifshare.irods.home") "/iplant/home"))

(defn irods-zone
  []
  (or (get @props "kifshare.irods.zone") "iplant"))

(defn irods-default-resource
  []
  (or (get @props "kifshare.irods.defaultResource") ""))

(defn irods-anonymous-user
  []
  (or (get @props "kifshare.irods.anon-user") "anonymous"))

(def jgcfg (atom nil))

(defn jargon-config [] @jgcfg)

(defn jargon-init
  []
  (reset! jgcfg
          (jinit/init
           (irods-host)
           (irods-port)
           (username)
           (irods-password)
           (irods-home)
           (irods-zone)
           (irods-default-resource))))

(defn css-files
  []
  (mapv
    string/trim
    (string/split
      (or (get @props "kifshare.app.css-files") default-css-files)
      #",")))

(defn javascript-files
  []
  (mapv
    string/trim
    (string/split
      (or (get @props "kifshare.app.javascript-files") default-javascript-files)
      #",")))

(defn- exception-filters
  []
  (mapv #(re-pattern (str %))
        [(username) (irods-password)]))

(defn register-exception-filters
  []
  (ce/register-filters (exception-filters)))

(defn log-config
  []
  (log/warn "Configuration:")
  (cc/log-config props :filters [#"irods\.user"]))
