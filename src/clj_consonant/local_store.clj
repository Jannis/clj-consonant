(ns clj-consonant.local-store
  (:require [clojure.string :as str]
            [clj-jgit.porcelain :refer [load-repo]]
            [clj-time.coerce :refer [from-date to-epoch]]
            [clj-time.core :refer [time-zone-for-offset to-time-zone]]
            [clj-time.format :refer [formatter unparse]]
            [clj-consonant.git :refer :all]
            [clj-consonant.store :refer :all]
            [clj-consonant.transit :refer [transit-read]])
  (:import [org.eclipse.jgit.lib Constants]
           [org.eclipse.jgit.revwalk RevCommit RevWalk]
           [org.eclipse.jgit.treewalk TreeWalk]))

; JGit helpers

(defn- rev-walk
  [store]
  (RevWalk. (.getRepository (:repo store))))

(defn- tree-walk
  [store trees]
  (let [tw (TreeWalk. (.getRepository (:repo store)))]
    (doseq [t trees] (.addTree tw t))
    tw))

(defn- tree-walk-for-path
  [store path tree]
  (TreeWalk/forPath (.getRepository (:repo store)) path tree))

(defn- object-loader
  [store oid]
  (.open (.getRepository (:repo store)) oid))

(defn- is-annotated-tag
  [store ref]
  (->> (.getObjectId ref)
       (object-loader store)
       (.getType)
       (= Constants/OBJ_TAG)))

(defn- to-oid
  [store sha1]
  (.. (:repo store) (getRepository) (resolve sha1)))

(defn- ref-commit
  [store ref]
  (->> (get-in ref [:head :sha1])
       (to-oid store)
       (.parseCommit (rev-walk store))))

(defn- subtree-by-name
  [store tree name]
  (let [tw (tree-walk-for-path store name tree)]
    (.parseTree (rev-walk store) (.getObjectId tw 0))))

(defn- class-tree
  [store commit class-name]
  (subtree-by-name store (.getTree commit) class-name))

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
  (let [commit (.parseCommit (rev-walk store) oid)]
    {:sha1           (.getName oid)
     :author         (parse-ident (.getAuthorIdent commit))
     :author-date    (parse-ident-time (.getAuthorIdent commit))
     :committer      (parse-ident (.getCommitterIdent commit))
     :committer-date (parse-ident-time (.getCommitterIdent commit))
     :subject        (.getShortMessage commit)
     :parents        (mapv #(.getId %) (.getParents commit))}))

(defn parse-tag
  [store oid]
  (let [tag    (.parseTag (rev-walk store) oid)
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

(defn load-class
  [store commit class-name]
  (let [class-tree (class-tree store commit class-name)
        class-walk (tree-walk store [class-tree])
        objects    (transient [])]
    (while (.next class-walk)
      (conj! objects {:uuid (.getPathString class-walk)}))
    {:name class-name
     :objects (persistent! objects)}))

(defn- load-classes
  [store commit]
  (let [walk    (tree-walk store [(.getTree commit)])
        classes (transient {})]
    (while (.next walk)
      (when (.isSubtree walk)
        (let [class-name (.getPathString walk)]
          (assoc! classes class-name (load-class store commit class-name)))))
    (persistent! classes)))

(defn- load-object
  [store commit class uuid]
  (let [class-tree (class-tree store commit (:name class))
        class-walk (tree-walk-for-path store uuid class-tree)
        oid        (.getObjectId class-walk 0)
        loader     (object-loader store oid)]
    {:uuid       uuid
     :class      (:name class)
     :properties (transit-read (.getBytes loader))}))

(defn- load-objects
  [store commit class]
  (let [class-tree (class-tree store commit (:name class))
        ctw        (tree-walk store [class-tree])
        objects    (transient [])]
    (while (.next ctw)
      (conj! objects (load-object store commit class (.getPathString ctw))))
    (persistent! objects)))

(defrecord LocalStore [location cache repo]
  Store

  (connect [this]
    (assoc this :repo (load-repo (:location this))))

  (disconnect [this]
    (assoc this :repo nil))

  (get-refs [this]
    (when (:repo this)
      (->> (git-refs (:repo this))
           (parse-refs this))))

  (get-ref [this ref-alias]
    (when (:repo this)
      (->> (git-ref (:repo this) ref-alias)
           (parse-ref this))))

  (get-classes [this]
    (get-classes this "HEAD"))

  (get-classes [this ref-alias]
    (when (:repo this)
      (->> ref-alias
           (get-ref this)
           (ref-commit this)
           (load-classes this))))

  (get-class [this class-name]
    (get-class this "HEAD" class-name))

  (get-class [this ref-alias class-name]
    (when (:repo this)
      (load-class this
                  (->> ref-alias
                       (get-ref this)
                       (ref-commit this))
                  class-name)))

  (get-objects [this class-name]
    (get-objects this "HEAD" class-name))

  (get-objects [this ref-alias class-name]
    (when (:repo this)
      (load-objects this
                    (->> ref-alias
                         (get-ref this)
                         (ref-commit this))
                    (get-class this ref-alias class-name))))

  (get-object [this class-name uuid]
    (get-object this "HEAD" class-name uuid))

  (get-object [this ref-alias class-name uuid]
    (when (:repo this)
      (load-object this
                   (->> ref-alias
                        (get-ref this)
                        (ref-commit this))
                    (get-class this ref-alias class-name)
                    uuid)))

  (get-properties [this class-name uuid]
    (get-properties this "HEAD" class-name uuid))

  (get-properties [this ref-alias class-name uuid]
    (when (:repo this)
      (:properties (get-object this ref-alias class-name uuid))))

  (get-property [this class-name uuid name]
    (get-property this "HEAD" class-name uuid name))

  (get-property [this ref-alias class-name uuid name]
    (when (:repo this)
      (->> (get-object this ref-alias class-name uuid)
           :properties
           name))))

(defn local-store
  ([location]
    (-> (LocalStore. location nil nil)
        (connect)))
  ([location cache]
    (-> (LocalStore. location cache nil)
        (connect))))
