(ns clj-consonant.actions)

(defn begin
  [& {:keys [source]}]
  {:action :begin
   :source source})

(defn commit
  [& {:keys [target author committer message]}]
  {:action         :commit
   :target         target
   :author         author
   :committer      committer
   :message        message})

(defn create
  [& {:keys [id class properties]}]
  {:action     :create
   :id         id
   :class      class
   :properties properties})
