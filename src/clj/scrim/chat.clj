(ns scrim.chat
  (:require [clojure.string :refer [split]]
            [clojure.pprint :refer [pprint]]
            [environ.core :refer [env]]
            [ring.util.response :refer
             [response content-type redirect]]
            [compojure.core :refer [defroutes POST GET ANY context routes]]
            [clojure.core.async :as async :refer [<! >! put! chan go-loop]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit
             :refer [sente-web-server-adapter]]
            [scrim.util :refer [forbidden]]))

(let [{:keys [ch-recv
              send-fn
              ajax-post-fn
              ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente/make-channel-socket!
       sente-web-server-adapter
       {:user-id-fn (fn [ring-req] (get-in ring-req [:session :steamid]))})

      broadcast
      (fn [event]
        (dorun (map #(send-fn % event) (:any @connected-uids))))]

  (defroutes chat-routes
    (GET  "/channel-socket" req (ajax-get-or-ws-handshake-fn req))
    (POST "/channel-socket" req (ajax-post-fn req)))

  (go-loop []
    (let [{:keys [event id ?data client-id ring-req]} (<! ch-recv)
          {:keys [steamid] :as session} (:session ring-req)]
      (case id
        ::message
        (when steamid
          (broadcast [::message
                      {:steamid steamid :message (:message ?data)}]))

        (println "Unhandled message type" id ?data)))
    (recur)))
