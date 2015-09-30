(ns clj-consonant.git.reference
  (:import [org.eclipse.jgit.lib Constants])
  (:refer-clojure :exclude [load])
  (:require [clojure.string :as str]
            [clj-consonant.git.coerce :refer [to-alias to-oid]]
            [clj-consonant.git.commit :as commit]
            [clj-consonant.git.repo :refer [object-type rev-walk]]
            [clj-consonant.git.tag :as tag]))

(defrecord Reference [type url-aliases tag head])

(defn to-reference [repo jref]
  (let [tag?       (re-matches #"^refs/tags/" (.getName jref))
        annotated? (= Constants/OBJ_TAG (object-type repo (.getObjectId jref)))
        alias      (to-alias (.getName jref))
        tag        (when annotated? (tag/load repo (.getObjectId jref)))
        head-oid   (if tag
                     (->> (to-oid repo (:sha1 tag))
                          (.parseTag (rev-walk repo))
                          (.getObject)
                          (.getId))
                      (.getObjectId (.getLeaf jref)))
        head       (commit/load repo head-oid)]
    (->Reference (if tag? "tag" "branch") [alias] tag head)))

(defn load [repo name]
  (->> (.getRef (.getRepository repo) name)
       (to-reference repo)))

(defn load-all [repo]
  (into {}
    (for [[name ref] (.getAllRefs (.getRepository repo))]
      [name (to-reference repo ref)])))
