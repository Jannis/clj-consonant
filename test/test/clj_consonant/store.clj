(ns test.clj-consonant.store
  (:use clojure.test)
  (:require [environ.core :refer [env]]
            [clj-consonant.store :as store]))

(deftest store-load
  (is (not (nil? (store/load-store (:test-dir env))))))

(deftest store
  (store-load))
