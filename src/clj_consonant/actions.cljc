(ns clj-consonant.actions)

(defn begin
  [& {:keys [source]}]
  {:action :begin
   :source source})

(defn end
  [& {:keys [target author author-date committer committer-date message]}]
  {:action         :end
   :target         target
   :author         author
   :author-date    author-date
   :committer      committer
   :committer-date committer-date
   :message        message})

(defn create
  [& {:keys [id class properties]}]
  {:action     :create
   :id         id
   :class      class
   :properties properties})
