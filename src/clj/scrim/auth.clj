(ns scrim.auth
  (:require [clojure.string :refer [split]]
            [clojure.pprint :refer [pprint]]
            [environ.core :refer [env]]
            [ring.util.response :refer
             [response content-type redirect]]
            [compojure.core :refer [defroutes POST GET ANY context routes]]
            [scrim.dev :refer [is-dev?]]
            [scrim.util :refer [forbidden]]
            [scrim.steamapi :refer [get-player fetch-player]])
  (:import (org.openid4java.consumer ConsumerManager VerificationResult
                                     InMemoryConsumerAssociationStore
                                     InMemoryNonceVerifier)))

(def ^:private consumer-manager
  ;; Steam needs us to use stateless mode
  (doto (ConsumerManager.)
    (.setMaxAssocAttempts 0)
    (.setNonceVerifier (InMemoryNonceVerifier. 60))))

(def ^:private steam-discoveries
  (.discover consumer-manager "http://steamcommunity.com/openid"))

(def ^:private steam-association
  (.associate consumer-manager steam-discoveries))

(defn ^:private post-login [steamid session]
  (future (get-player steamid) :force-fetch? true)
  (-> (redirect "/")
      (assoc :session session)
      (assoc-in [:session :steamid] steamid)))

(defroutes auth-routes
  (GET "/state" {session :session}
    (response
     (if-let [steamid (:steamid session)]
       {:logged-in true
        :steamid steamid
        :info (get-player steamid)}
       {:logged-in false})))

  (POST "/login" {uri :uri
                  scheme :scheme
                  {host :host} :headers
                  session :session
                  :as req}
    (let [return-url (str (name scheme) "://"
                          (env :public-url)
                          uri "-verify")
          auth-req (.authenticate consumer-manager
                                  steam-association
                                  return-url)]
      (redirect (.getDestinationUrl auth-req true))))

  (GET "/login-mock" [steamid :as {session :session :as req}]
    (if is-dev?
      (post-login steamid session)
      (forbidden)))

  (GET "/login-verify" {params :params
                        session :session
                        :as req}
    (println "calling login-verify" params "changed" (into {} (for [[k v] (:params req)] [(name k) v])) "req" req)
    (let [openid-req (into {} (for [[k v] (:params req)] [(name k) v]))
          resp-params (org.openid4java.message.ParameterList. openid-req)
          receiving-url (str (name (:scheme req)) "://"
                            ((:headers req) "host")
                            (:uri req)
                            "?" (:query-string req))
          verification (.verify consumer-manager
                                receiving-url
                                resp-params
                                steam-association)
          steamid (-> verification
                      .getVerifiedId
                      .getIdentifier
                      (split #"/")
                      last)]
      (post-login steamid session))))
