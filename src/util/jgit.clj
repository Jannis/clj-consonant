(ns util.jgit
  (:require [clj-jgit.internal :refer [ref-database]]
            [clj-jgit.porcelain :refer :all])
  (:import [org.eclipse.jgit.lib
            CommitBuilder
            Constants
            FileMode
            PersonIdent
            TreeFormatter]))

(defn git-identity
  [name email]
  (PersonIdent. name email))

(defn git-ref
  [repo name]
  (.. repo getRepository (getRef name)))

(defn git-object-inserter
  [repo]
  (.. repo getRepository newObjectInserter))

(defn git-create-blob
  [repo data]
  (let [bytes    (-> data .toString (.getBytes "utf-8"))
        inserter (git-object-inserter repo)
        oid      (.insert inserter (Constants/OBJ_BLOB) bytes)]
    (.flush inserter)
    {:oid oid :type FileMode/REGULAR_FILE}))

(defn git-create-tree
  [repo objects]
  (let [formatter (TreeFormatter.)]
    (doseq [object objects]
      (.append formatter (:name object) (:type object) (:oid object)))
    (let [inserter  (git-object-inserter repo)
          oid       (.insert inserter formatter)]
      (.flush inserter)
      {:oid oid :type FileMode/TREE})))

(defn git-create-commit
  [repo author subject tree]
  (let [builder  (CommitBuilder.)]
    (.setMessage builder subject)
    (.setTreeId builder (:oid tree))
    (.setAuthor builder author)
    (.setCommitter builder author)
    (let [inserter (git-object-inserter repo)
          oid      (.insert inserter builder)]
      (.flush inserter)
      oid)))

(defn git-update-ref
  [repo refname commit-id]
  (let [refdb  (ref-database repo)
        update (.newUpdate refdb refname false)]
    (.setNewObjectId update commit-id)
    (.update update)))
