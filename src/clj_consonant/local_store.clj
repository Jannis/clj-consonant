(ns clj-consonant.local-store
  (:require [clojure.string :as str]
            [clj-jgit.porcelain :refer [load-repo]]
            [clj-time.coerce :refer [from-date to-epoch]]
            [clj-time.core :refer [time-zone-for-offset to-time-zone]]
            [clj-time.format :refer [formatter unparse]]
            [util.jgit :refer :all]
            [clj-consonant.store :refer :all])
  (:import [org.eclipse.jgit.lib Constants]
           [org.eclipse.jgit.revwalk RevCommit RevWalk]))

; JGit helpers

(defn- revwalk
  [store]
  (RevWalk. (.getRepository (:repo store))))

(defn- object-loader
  [store oid]
  (.open (.getRepository (:repo store)) oid))

; Git object parsing

(defn- parse-ident
  [ident]
  (format "%s <%s>" (.getName ident) (.getEmailAddress ident)))

(defn- parse-ident-time
  [ident]
  (let [offset  (.getTimeZoneOffset ident)
        hours   (quot offset 60)
        minutes (rem offset 60)
        tz      (time-zone-for-offset hours minutes)
        date    (to-time-zone (from-date (.getWhen ident)) tz)]
    (format "%s %s" (to-epoch date) (unparse (formatter "Z" tz) date))))

(defn- parse-commit
  [store oid]
  (let [commit (.parseCommit (revwalk store) oid)]
    {:sha1           (.getName oid)
     :author         (parse-ident (.getAuthorIdent commit))
     :author-date    (parse-ident-time (.getAuthorIdent commit))
     :committer      (parse-ident (.getCommitterIdent commit))
     :committer-date (parse-ident-time (.getCommitterIdent commit))
     :subject        (.getShortMessage commit)
     :parents        (mapv #(.getId %) (.getParents commit))}))

(defn parse-tag
  [store oid]
  (let [tag    (.parseTag (revwalk store) oid)
        commit (.getObject tag)]
    {:sha1        (.getName oid)
     :tagger      (parse-ident (.getTaggerIdent tag))
     :tagger-date (parse-ident-time (.getTaggerIdent tag))
     :subject     (.getShortMessage tag)
     :parents     [(.getName (.getId commit))]}))

(defmulti parse-ref (fn [store name ref] (->> (str/split name #"/")
                                              (take 2)
                                              (str/join "/"))))

(defmethod parse-ref "HEAD"
  [store name ref]
  {:type "branch"
   :url-aliases ["HEAD"]
   :head (parse-commit store (.. ref (getLeaf) (getObjectId)))})

(defmethod parse-ref "refs/heads"
  [store name ref]
  {:type "branch"
   :url-aliases [(str/replace name "/" ":")]
   :head (parse-commit store (.. ref (getLeaf) (getObjectId)))})

(defmethod parse-ref "refs/tags"
  [store name ref]
  (println "IN" (.. ref getObjectId toString)
                (.. ref getLeaf getObjectId toString))
  {:type "tag"
   :url-aliases [(str/replace name "/" ":")]
   :head (condp = (.getType (object-loader store (.getObjectId ref)))
           Constants/OBJ_TAG    (parse-tag store (.getObjectId ref))
           Constants/OBJ_COMMIT (parse-commit store (.getObjectId ref)))})

(defn- parse-refs
  [store refs]
  (into {} (for [[k v] refs] [k (parse-ref store k v)])))

; Local store implementation

(defrecord LocalStore [location cache repo]
  Store

  (connect [this]
    (assoc this :repo (load-repo (:location this))))

  (disconnect [this]
    (assoc this :repo nil))

  (refs [this]
    (if (:repo this)
      (->> (git-refs (:repo this))
           (parse-refs this))
      {})))

(defn local-store
  ([location]
    (-> (LocalStore. location nil nil)
        (connect)))
  ([location cache]
    (-> (LocalStore. location cache nil)
        (connect))))
