(ns scrim.core
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [ajax.core :refer [GET POST]]
            [cljs.core.async :as async :refer [<! >! put! chan]]
            [taoensso.sente  :as sente :refer [cb-success?]])
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer [go go-loop]]))

(enable-console-print!)

(defonce app-state (atom {:user {} :chat {}}))

(def login-button
  (dom/form {:method "post" :action "/api/auth/login"}
            (dom/input {:type "image"
                        :alt "Sign in through Steam"
                        :src "images/sits_large_noborder.png"
                        :width "114"
                        :height "43"})))

(defcomponent user-widget [data owner]
  (will-mount [_]
    (GET "/api/auth/state"
        {:handler
         (fn [resp]
           (println resp)
           (om/update! data resp))}))
  (render [_]
    (if (empty? data)
      (dom/h1 nil "Loading...")
      (if (:logged-in data)
        (dom/div
          (dom/h1 (:personaname (:info data)))
          (dom/img {:src (:avatar (:info data))})
          (dom/p (pr-str data)))
        login-button))))

(defcomponent chat-box [data owner]
  (will-mount [_]
    (let [{:keys [chsk ch-recv send-fn state]}
          (sente/make-channel-socket! "/api/chat/channel-socket"
                                      {:type :auto
                                       :wrap-recv-evs? false})]

      (om/update! data {:log []
                        :players []
                        :state state})

      (go-loop []
        (let [{:keys [id ?data]} (<! ch-recv)]
          (case id
            :chsk/state
            (om/update! data :state ?data)

            :scrim.chat/message
            (om/transact! data :log #(conj % ?data))

            :scrim.chat/players
            (om/transact! data :players #(conj % ?data))

          (println "Unhandled message type" ctrl-id ?data)))
        (recur))

      (go-loop [i 0]
        (<! (async/timeout 5000))
        (send-fn [:scrim.chat/message {:message (str "hi " i)}])
        (recur (inc i)))))

  (render [_]
    (if (empty? data)
      (dom/h1 nil "Loading...")
      (dom/div {:class "chatbox"}
        (dom/h1 "Chat")
        (dom/ul
          (map (fn [{:keys [steamid message]}]
                 (dom/li steamid message))
               (:log data)))
        (dom/input {:type "text"})
        (dom/p (pr-str data))))))

(defcomponent main-page [data owner]
  (render [_]
    (dom/div
      (om/build user-widget (:user data))
      (om/build chat-box (:chat data)))))

(defn main []
  (om/root main-page
           app-state
           {:target (. js/document (getElementById "app"))}))
