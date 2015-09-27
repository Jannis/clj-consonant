(ns test.clj-consonant.local-store
  (:use clojure.test)
  (:require [clojure.pprint :refer [pprint]]
            [environ.core :refer [env]]
            [clj-consonant.store :refer :all]
            [clj-consonant.local-store :refer [local-store]]))

(def test-dir   (:test-dir env))
(def mock-cache {})

(deftest local-store-location-only
  (let [s (local-store test-dir)]
    (is (satisfies? Store s))
    (is (= test-dir (:location s))
    (is (= nil (:cache s))))))

(deftest local-store-with-cache
  (let [s (local-store test-dir mock-cache)]
    (is (satisfies? Store s))
    (is (= test-dir (:location s))
    (is (= mock-cache (:cache s))))))

(deftest local-store-has-repo
  (let [s (local-store test-dir)]
    (is (not (nil? (:repo s))))))

(deftest local-store-has-refs
  (let [s  (local-store test-dir)
        rs (refs s)]
    (pprint rs)
    (is (map? rs))
    (and (is (contains? rs "HEAD"))
         (let [ref (get rs "HEAD")]
           (is (= "branch" (:type ref)))))
    (and (is (contains? rs "refs/heads/master"))
         (let [ref (get rs "refs/heads/master")]
           (is (= "branch" (:type ref)))))))
