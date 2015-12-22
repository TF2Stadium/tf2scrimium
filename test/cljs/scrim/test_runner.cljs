(ns scrim.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [scrim.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        'scrim.core-test))
    0
    1))
