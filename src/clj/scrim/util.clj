(ns scrim.util
  (:require [ring.util.response :refer [response content-type]]))

(defn forbidden []
  {:status 403
   :body "Forbidden"})
