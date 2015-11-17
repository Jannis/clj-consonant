(ns clj-consonant.actions
  (:refer-clojure :exclude [update]))

(defn begin
  [& {:keys [source]}]
  {:action :begin
   :source source})

(defn commit
  [& {:keys [target author committer message]}]
  {:action    :commit
   :target    target
   :author    author
   :committer committer
   :message   message})

(defn create
  [& {:keys [uuid class properties]}]
  {:action     :create
   :uuid       uuid
   :class      class
   :properties properties})

(defn delete
  [& {:keys [uuid]}]
  {:action :delete
   :uuid   uuid})

(defn update
  [& {:keys [uuid properties]}]
  {:action     :update
   :uuid       uuid
   :properties properties})
