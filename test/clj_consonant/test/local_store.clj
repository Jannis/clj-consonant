(ns clj-consonant.test.local-store
  (:require [clojure.test.check.clojure-test :refer [defspec]]
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
        (and (satisfies? s/Store store)
             (satisfies? component/Lifecycle store)
             (nil? (:repo store))))
      (finally (setup/delete-git-repo path)))))

(defspec new-local-store-connected-after-start 10
  (prop/for-all [path setup/gen-git-repo]
    (try
      (let [store (ls/new-local-store path)]
        (not (nil? (-> store component/start :repo))))
      (finally (setup/delete-git-repo path)))))

(defspec new-local-store-disconnected-after-stop 10
  (prop/for-all [path setup/gen-git-repo]
    (try
      (let [store (ls/new-local-store path)]
        (nil? (-> store component/start component/stop :repo)))
      (finally (setup/delete-git-repo path)))))

(defspec local-store-already-connected 10
  (prop/for-all [path setup/gen-git-repo]
    (try
      (let [store (ls/local-store path)]
        (not (nil? (:repo store))))
      (finally (setup/delete-git-repo path)))))
