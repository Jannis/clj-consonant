(ns util.create-test-store
  (:require [clj-jgit.porcelain :refer :all]
            [clj-uuid :as uuid]
            [me.raynes.fs :refer [temp-dir]]
            [util.jgit :refer :all]
            [util.transit :refer :all]))

(def USERS [{:name "Jannis Pohlmann" :email "jannis@xfce.org"}
            {:name "Simon Steinbeiß" :email "simon@xfce.org"}])

(defn create-object
  [repo data]
  (merge {:name (str (uuid/v4))} (git-create-blob repo (transit-write data))))

(defn create-class
  [repo name objects]
  (merge {:name name} (git-create-tree repo objects)))

(defn create-users
  [repo]
  (println "Create users")
  (->> (doall (map (partial create-object repo) USERS))
       (create-class repo "users")))

(defn run
  []
  (let [dir (temp-dir "clj-consonant")]
    (println dir)
    (git-init dir)
    (with-repo dir
      (let [users-tree (create-users repo)
            root-tree  (git-create-tree repo [users-tree])
            author     (git-identity "Jannis Pohlmann" "jannis@xfce.org")]
        (->> (git-create-commit repo author "First commit" root-tree)
             (git-update-ref repo "HEAD")
             (println))))))
