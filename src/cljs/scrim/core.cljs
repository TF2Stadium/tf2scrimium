(ns scrim.core
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [ajax.core :refer [GET POST]]))

(enable-console-print!)

(defonce app-state
  (atom {:text "Hello Chestnut!"
         :user {}}))

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

(defn main []
  (om/root
    (fn [app owner]
      (reify
        om/IRender
        (render [_]
          (dom/div
            (om/build user-widget (:user app))
            (dom/h1 (:text app))))))
    app-state
    {:target (. js/document (getElementById "app"))}))
