(ns util.create-test-store
  (:require [clj-jgit.porcelain :refer :all]
            [clj-uuid :as uuid]
            [me.raynes.fs :refer [delete-dir exists? mkdirs temp-dir]]
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
  [test-dir]
  (let [dirname test-dir]
    (println dirname)
    (when (exists? dirname)
      (delete-dir dirname))
    (mkdirs dirname)
    (git-init dirname)
    (with-repo dirname
      (let [users-tree (create-users repo)
            root-tree  (git-create-tree repo [users-tree])
            author     (git-identity "Jannis Pohlmann" "jannis@xfce.org")
            commit     (git-create-commit repo author "First commit" root-tree)
            head       (git-update-ref repo "HEAD" commit)
            atag       (git-create-annotated-tag repo
                                                 "annotated-tag"
                                                 author
                                                 "Tag the first commit"
                                                 commit)]
            ; atag       (git-update-ref real-repo "refs/tags/annotated-tag" atag-obj)]
        (println "commit  " commit)
        (println "head    " head)
        ; (println "atag obj" atag-obj)
        (println "atag    " atag)))))
