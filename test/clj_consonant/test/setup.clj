(ns clj-consonant.test.setup
  (:require [clojure.java.io :as io]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [me.raynes.fs :as fs]
            [clj-consonant.git.repo :as repo]))

(def gen-temp-dir
  (gen/fmap fs/temp-dir gen/string-alphanumeric))

(defspec temp-dir-generator-works 10
  (prop/for-all [dir gen-temp-dir]
    (try
      (fs/directory? dir)
      (finally (fs/delete-dir dir)))))

(defn init-git-repo [dir]
  (let [path (.getAbsolutePath dir)]
    (repo/init path)
    path))

(defn delete-git-repo [path]
  (fs/delete-dir (io/as-file path)))

(def gen-git-repo
  (gen/fmap init-git-repo gen-temp-dir))
