(ns kifshare.core
  (:gen-class)
  (:use compojure.core
        [ring.middleware
         params
         keyword-params
         nested-params
         multipart-params])
  (:require [clojure-commons.file-utils :as ft]
            [clojure.tools.logging :as log]
            [compojure.route :as route]
            [ring.util.response :as resp]
            [ring.util.http-response :as http-resp]
            [kifshare.config :as cfg]
            [kifshare.tickets-controllers :as t-c]
            [kifshare.anon-controllers :as a-c]
            [kifshare.amqp :as amqp]
            [kifshare.events :as events]
            [kifshare.ui-template :as ui]
            [clojure.string :as string]
            [common-cli.core :as ccli]
            [me.raynes.fs :as fs]
            [hawk.core :as hawk]))

(defn- keep-alive
  [resp]
  (assoc-in resp [:headers "Connection"] "Keep-Alive"))

(defn- caching
  [resp]
  (assoc-in resp [:headers "Cache-Control"]
            (str (cfg/client-cache-scope) ", max-age=" (cfg/client-cache-max-age))))

(defn- static-resp
  [file-path & {:keys [root] :or {root (cfg/resources-root)}}]
  (-> (resp/file-response file-path {:root root})
      caching
      keep-alive))

(defn- well-known-routes []
  (routes
    (GET "/favicon.ico" []
         (static-resp (cfg/favicon-path)))

    (GET "/robots.txt" []
         (static-resp (cfg/robots-txt-path)))))

(defn- resources-routes []
  (context "/resources" []
    (GET "/:rsc-name"
         [rsc-name]
         (static-resp rsc-name))

    (GET "/fa/css/:rsc-name"
         [rsc-name]
         (let [resource-root (ft/path-join (cfg/resources-root) "fa/css")]
           (resp/content-type (static-resp rsc-name :root resource-root) "text/css")))

    (GET "/fa/fonts/:rsc-name"
         [rsc-name]
         (let [resource-root (ft/path-join (cfg/resources-root) "fa/fonts")]
           (static-resp rsc-name :root resource-root)))

    (GET "/css/:rsc-name"
         [rsc-name]
         (let [resource-root (ft/path-join (cfg/resources-root) (cfg/css-dir))]
           (resp/content-type (static-resp rsc-name :root resource-root) "text/css")))

    (GET "/js/:rsc-name"
         [rsc-name]
         (let [resource-root (ft/path-join (cfg/resources-root) (cfg/js-dir))]
           (resp/content-type (static-resp rsc-name :root resource-root) "application/json")))

    (GET "/img/:rsc-name"
         [rsc-name]
         (let [resource-root (ft/path-join (cfg/resources-root) (cfg/img-dir))]
           (static-resp rsc-name :root resource-root)))))

(defn- ticket-routes []
  (routes
    ;; ticket download
    (context "/d/:ticket-id" [ticket-id]
      (HEAD "/:filename" [ticket-id]
            (t-c/file-info ticket-id))

      (OPTIONS "/:filename" [ticket-id]
            (t-c/file-options ticket-id))

      (GET "/:filename" [ticket-id filename :as request]
           (t-c/download-file ticket-id filename request))

      (HEAD "/" [ticket-id]
            (t-c/file-info ticket-id))

      (OPTIONS "/" [ticket-id]
            (t-c/file-options ticket-id))

      (GET "/" [ticket-id :as request]
           (t-c/download-ticket ticket-id request)))

    ;; ticket info
    (GET "/:ticket-id" [ticket-id :as request]
         (resp/content-type (t-c/get-ticket ticket-id request) "text/html; charset=utf-8"))))

(defn- collapse-slashes
  [filepath]
  (string/replace filepath #"/+" "/"))

(defn- anon-files-routes []
  (context "/anon-files" []
    (HEAD ":filepath{.*}" [filepath] (a-c/handle-head (collapse-slashes filepath)))
    (GET ":filepath{.*}" [filepath :as req] (a-c/handle-get (collapse-slashes filepath) req))
    (OPTIONS ":filepath{.*}" [filepath] (a-c/handle-options (collapse-slashes filepath)))))

(defroutes kifshare-routes
  (GET "/" [:as {{expecting :expecting} :params :as req}]
       (if (and expecting (not= expecting "kifshare"))
         (http-resp/internal-server-error (str "Hello from kifshare. Error: expecting " expecting "."))
         "Hello from kifshare."))

  ;; static well-known location files
  (well-known-routes)

  ;; resources
  (resources-routes)

  ;; tickets
  (ticket-routes)

  ;; anonymous access
  (anon-files-routes)

  (route/not-found "Not found!"))

(def svc-info
  {:desc "Service that serves up public files from iRODS."
   :app-name "kifshare"
   :group-id "org.cyverse"
   :art-id "kifshare"
   :service "kifshare"})

(defn wrap-log-request
  [handler]
  (fn [request]
    (log/info request)
    (handler request)))

(defn site-handler [routes]
  (-> routes
      wrap-log-request
      wrap-multipart-params
      wrap-keyword-params
      wrap-nested-params
      wrap-params))

(def app
  (site-handler kifshare-routes))

(defn cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/kifshare.properties"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

(defn- override-buffer-size
  [opts]
  (or (:buffer-size opts)
      (* 1024 (Integer/parseInt (get @cfg/props "kifshare.app.download-buffer-size")))))

(defn listen-for-events
  []
  (let [exchange-cfg (events/exchange-config)
        queue-cfg    (events/queue-config)]
    (amqp/connect exchange-cfg queue-cfg {"events.kifshare.ping" events/ping-handler})))

(defn -main
  [& args]

  (let [{:keys [options]} (ccli/handle-args svc-info args cli-options)]
    (when-not (fs/exists? (:config options))
      (ccli/exit 1 (str "The config file does not exist.")))
    (when-not (fs/readable? (:config options))
      (ccli/exit 1 "The config file is not readable."))
    (cfg/local-init (:config options))
    (cfg/jargon-init)
    (cfg/log-config)
    (.start (Thread. listen-for-events))
    (with-redefs [clojure.java.io/buffer-size override-buffer-size]
      (let [port (Integer/parseInt (string/trim (get @cfg/props "kifshare.app.port")))]
        (ui/read-template)
        (hawk/watch! {:watcher :polling}
                     [{:paths ["resources/ui.xml"]
                       :handler (fn [ctx e]
                                  (when (= (:kind e) :modify)
                                    (ui/read-template)))}])
        (log/warn "Configured listen port is: " port)
        (require 'ring.adapter.jetty)
        ((eval 'ring.adapter.jetty/run-jetty) app {:port port})))))
