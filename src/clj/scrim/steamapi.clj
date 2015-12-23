;;; This files provides a wrapper around the "steamweb" library to
;;; provide an API that better fits this project's use case.

(ns scrim.steamapi
  (:require [clojure.string :refer [split]]
            [clojure.pprint :refer [pprint]]
            [environ.core :refer [env]]
            [steamweb.core :as steam]))

(def ^:private player-cache (atom {}))

(defn fetch-player [steamid]
  (-> (steam/player-summaries (env :steam-api-key) [steamid])
      (get-in [:response :players 0])))

(defn get-player
  "Returns information about a player, identified by the steamid
  parameter. May block to make an API request if the player's
  information is not yet cached."
  [steamid & {:keys [force-fetch?] :or {force-fetch? false}}]
  (if (or force-fetch? (not (contains? @player-cache steamid)))
    (swap! player-cache assoc steamid
           (future (fetch-player steamid))))
  @(get @player-cache steamid))
