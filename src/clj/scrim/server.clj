(ns scrim.server
  (:require [clojure.java.io :as io]
            [scrim.dev :refer [is-dev? inject-devmode-html
                               browser-repl start-figwheel start-less]]
            [compojure.core :refer [GET defroutes context]]
            [compojure.route :refer [resources not-found]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [net.cgrand.reload :refer [auto-reload]]
            [ring.middleware.reload :as reload]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.browser-caching :refer [wrap-browser-caching]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.transit
             :refer [wrap-transit-response wrap-transit-body]]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [scrim.auth :refer [auth-routes]])
  (:gen-class))

(deftemplate page (io/resource "index.html") []
  [:body] (if is-dev? inject-devmode-html identity))

(defroutes routes
  (resources "/")
  (resources "/react" {:root "react"})
  (context "/api" {}
      (context "/auth" {} auth-routes))
  (GET "/" {} (page))
  (not-found "404"))

(defn- wrap-browser-caching-opts [handler]
  (wrap-browser-caching handler (or (env :browser-caching) {})))

(defn wrap-debug [handler]
  (fn [request]
    (let [response (handler request)]
      (println "request:" request)
      (println "response:" response)
      response)))

(def http-handler
  (cond-> routes
    is-dev? wrap-debug
    true wrap-params
    true wrap-transit-response
    true wrap-transit-body
    true wrap-session
    true (wrap-defaults api-defaults)
    is-dev? reload/wrap-reload
    true wrap-gzip))

(defn run-web-server [& [port]]
  (let [port (Integer. (or port (env :port) 10555))]
    (println (format "Starting web server on port %d." port))
    (run-jetty #'http-handler {:port port :join? false})))

(defn run-auto-reload [& [port]]
  (auto-reload *ns*)
  (start-figwheel)
  (start-less))

(defn run [& [port]]
  (when is-dev?
    (println "Running in devloper mode")
    (run-auto-reload))
  (run-web-server port))

(defn -main [& [port]]
  (run port))
