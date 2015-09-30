(ns clj-consonant.git.commit
  (:refer-clojure :exclude [load])
  (:require [clj-consonant.git.coerce :refer [to-oid]]
            [clj-consonant.git.ident :as ident]
            [clj-consonant.git.repo :refer [rev-walk]]
            [clj-consonant.git.tree :as git-tree]))

(defrecord Commit [sha1
                   author
                   committer
                   subject
                   message
                   parents])

(defn to-commit [repo jcommit]
  (let [sha1      (.getName jcommit)
        author    (ident/load (.getAuthorIdent jcommit))
        committer (ident/load (.getCommitterIdent jcommit))
        subject   (.getShortMessage jcommit)
        message   (.getFullMessage jcommit)
        parents   (mapv #(.getId %) (.getParents jcommit))]
    (->Commit sha1 author committer subject message parents)))

(defn load [repo oid]
  (->> (.parseCommit (rev-walk repo) oid)
       (to-commit repo)))

(defn to-jcommit [repo commit]
  (->> (:sha1 commit)
       (to-oid repo)
       (.parseCommit (rev-walk repo))))

(defn tree [repo commit]
  (->> (to-jcommit repo commit)
       (.getTree)
       (.getId)
       (git-tree/load repo)))
