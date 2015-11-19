(ns clj-consonant.test.setup
  (:require [clojure.java.io :as io]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [me.raynes.fs :as fs]
            [clj-consonant.git.repo :as repo]
            [clj-consonant.local-store :as ls]))

;;;; Create temporary directories

(def gen-temp-dir
  (gen/fmap fs/temp-dir gen/string-alphanumeric))

(defspec temp-dir-generator-works 10
  (prop/for-all [dir gen-temp-dir]
    (try
      (fs/directory? dir)
      (finally (fs/delete-dir dir)))))

;;;; Create temporary git repositories

(defn init-git-repo [dir]
  (let [path (.getAbsolutePath dir)]
    (repo/init path)
    path))

(defn delete-git-repo [path]
  (fs/delete-dir (io/as-file path)))

(def gen-git-repo
  (gen/fmap init-git-repo gen-temp-dir))

;;;; Create temporary stores

(defn init-store [repo]
  (ls/local-store repo))

(defn delete-store [store]
  (delete-git-repo (:location store)))

(def gen-store
  (gen/fmap init-store
    (gen/fmap init-git-repo gen-temp-dir)))

(defn make-store []
  (-> (fs/temp-dir "clj-consonant")
      init-git-repo
      init-store))

;;;; Use stores in specs

(defmacro with-store
  [store & body]
  `(let [~(symbol "store") ~store]
     (try
       ~@body
       (finally
         (delete-store ~(symbol "store"))))))

;;;; Apply transactions
