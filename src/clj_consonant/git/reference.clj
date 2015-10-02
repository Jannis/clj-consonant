(ns clj-consonant.git.reference
  (:import [org.eclipse.jgit.lib Constants])
  (:refer-clojure :exclude [load])
  (:require [clojure.string :as str]
            [clj-consonant.git.coerce :refer [to-alias to-oid]]
            [clj-consonant.git.commit :as commit]
            [clj-consonant.git.repo :refer [object-type rev-walk]]
            [clj-consonant.git.tag :as tag]))

(defrecord Reference [name type url-aliases tag head])

(defn to-reference [repo jref]
  (when jref
    (let [name       (.getName jref)
          alias      (to-alias name)
          oid        (.getObjectId jref)
          tag?       (re-matches #"^refs/tags/" (.getName jref))
          annotated? (when oid (= Constants/OBJ_TAG (object-type repo oid)))
          tag        (when annotated? (tag/load repo (.getObjectId jref)))
          head-oid   (if tag
                       (->> (to-oid repo (:sha1 tag))
                            (.parseTag (rev-walk repo))
                            (.getObject)
                            (.getId))
                        (.getObjectId (.getLeaf jref)))
          head       (when head-oid (commit/load repo head-oid))]
      (->Reference name (if tag? "tag" "branch") [alias] tag head))))

(defn load [repo name]
  (->> (.getRef (.getRepository repo) name)
       (to-reference repo)))

(defn load-all [repo]
  (into {}
    (for [[name ref] (.getAllRefs (.getRepository repo))]
      [name (to-reference repo ref)])))
