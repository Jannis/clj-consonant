(ns clj-consonant.test.local-store
  (:require [clojure.test :refer [is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [com.stuartsierra.component :as component]
            [me.raynes.fs :as fs]
            [clj-consonant.store :as s]
            [clj-consonant.local-store :as ls]
            [clj-consonant.test.setup :as setup]))

(defspec new-local-store-not-connected-initially 10
  (prop/for-all [path setup/gen-git-repo]
    (try
      (let [store (ls/new-local-store path)]
        (is (and (satisfies? s/Store store)
                 (satisfies? component/Lifecycle store)
                 (nil? (:repo store)))))
      (finally (setup/delete-git-repo path)))))

(defspec new-local-store-connected-after-start 10
  (prop/for-all [path setup/gen-git-repo]
    (try
      (let [store (ls/new-local-store path)]
        (is (not (nil? (-> store component/start :repo)))))
      (finally (setup/delete-git-repo path)))))

(defspec new-local-store-disconnected-after-stop 10
  (prop/for-all [path setup/gen-git-repo]
    (try
      (let [store (ls/new-local-store path)]
        (is (nil? (-> store component/start component/stop :repo))))
      (finally (setup/delete-git-repo path)))))

(defspec local-store-already-connected 10
  (prop/for-all [path setup/gen-git-repo]
    (try
      (let [store (ls/local-store path)]
        (is (not (nil? (:repo store)))))
      (finally (setup/delete-git-repo path)))))

(defspec fresh-store-has-no-refs 10
  (prop/for-all [store setup/gen-store]
    (try
      (is (empty? (s/get-refs store)))
      (finally (setup/delete-store store)))))

(defspec fresh-store-has-an-empty-head-ref 10
  (prop/for-all [store setup/gen-store]
    (try
      (let [ref (s/get-ref store "HEAD")]
        (and (is (not (nil? ref)))
             (is (= "HEAD" (:name ref)))
             (is (= "branch" (:type ref)))
             (is (nil? (:tag ref)))
             (is (nil? (:head ref)))
             (is (= #{:name :type :tag :head}
                    (set (keys ref))))))
      (finally (setup/delete-store store)))))
