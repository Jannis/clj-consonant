(ns util.jgit
  (:require [clj-jgit.internal :refer [ref-database]]
            [clj-jgit.porcelain :refer :all])
  (:import [org.eclipse.jgit.lib
            CommitBuilder
            Constants
            FileMode
            PersonIdent
            TagBuilder
            TreeFormatter]
           [org.eclipse.jgit.revwalk
            RevWalk]))

(defn git-identity
  [name email]
  (PersonIdent. name email))

(defn git-ref
  [repo name]
  (.getRet (.getRepository repo) name))

(defn git-refs
  [repo]
  (.getAllRefs (.getRepository repo)))

(defn git-object-inserter
  [repo]
  (.newObjectInserter (.getRepository repo)))

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
  (let [builder (CommitBuilder.)]
    (.setMessage builder subject)
    (.setTreeId builder (:oid tree))
    (.setAuthor builder author)
    (.setCommitter builder author)
    (let [inserter (git-object-inserter repo)
          oid      (.insert inserter builder)]
      (.flush inserter)
      (.parseCommit (RevWalk. (.getRepository repo)) oid))))

(defn git-update-ref
  [repo refname commit]
  (let [update (.updateRef (.getRepository repo) refname)]
    (.setNewObjectId update (.getId commit))
    (.update update)))

(defn git-create-annotated-tag
  [repo name tagger subject commit]
  (.. repo
      (tag)
      (setName name)
      (setTagger tagger)
      (setMessage subject)
      (setAnnotated true)
      (setObjectId commit)
      (call)))
