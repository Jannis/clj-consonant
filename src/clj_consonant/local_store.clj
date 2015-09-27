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

(defn- is-annotated-tag
  [store ref]
  (->> (.getObjectId ref)
       (object-loader store)
       (.getType)
       (= Constants/OBJ_TAG)))

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
    {:head (parse-commit store (.getId (.getObject tag)))
     :tag  {:sha1        (.getName oid)
            :tagger      (parse-ident (.getTaggerIdent tag))
            :tagger-date (parse-ident-time (.getTaggerIdent tag))
            :subject     (.getShortMessage tag)}}))

(defmulti parse-ref (fn [store ref] (->> (str/split (.getName ref) #"/")
                                         (take 2)
                                         (str/join "/"))))

(defmethod parse-ref "HEAD"
  [store ref]
  {:type "branch"
   :url-aliases ["HEAD"]
   :head (parse-commit store (.. ref (getLeaf) (getObjectId)))})

(defmethod parse-ref "refs/heads"
  [store ref]
  {:type "branch"
   :url-aliases [(str/replace (.getName ref) "/" ":")]
   :head (parse-commit store (.. ref (getLeaf) (getObjectId)))})

(defmethod parse-ref "refs/tags"
  [store ref]
  (merge {:type "tag"
          :url-aliases [(str/replace (.getName ref) "/" ":")]}
         (if (is-annotated-tag store ref)
           (parse-tag store (.getObjectId ref))
           {:head (parse-commit store (.getObjectId ref))})))

(defn- parse-refs
  [store refs]
  (into {} (for [[name ref] refs] [name (parse-ref store ref)])))

; Local store implementation

(defrecord LocalStore [location cache repo]
  Store

  (connect [this]
    (assoc this :repo (load-repo (:location this))))

  (disconnect [this]
    (assoc this :repo nil))

  (get-refs [this]
    (if (:repo this)
      (->> (git-refs (:repo this))
           (parse-refs this))
      {}))

  (get-ref [this alias]
    (if (:repo this)
      (->> (git-ref (:repo this) alias)
           (parse-ref this)))))

(defn local-store
  ([location]
    (-> (LocalStore. location nil nil)
        (connect)))
  ([location cache]
    (-> (LocalStore. location cache nil)
        (connect))))
