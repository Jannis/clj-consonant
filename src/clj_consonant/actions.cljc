(ns clj-consonant.actions
  (:refer-clojure :exclude [update]))

;;;; Representing Consonant actions

(defprotocol IAction
  (action-type [this]))

(defrecord BeginAction [source]
  IAction
  (action-type [this] :begin))

(defrecord CommitAction [target author committer message]
  IAction
  (action-type [this] :commit))

(defrecord CreateAction [uuid class properties]
  IAction
  (action-type [this] :create))

(defrecord DeleteAction [uuid]
  IAction
  (action-type [this] :delete))

(defrecord UpdateAction [uuid properties]
  IAction
  (action-type [this] :update))

;;;; Representing Consonant transactions

(defprotocol ITransaction
  (actions [this]))

(defrecord Transaction [actions]
  ITransaction
  (actions [this]
    (:actions this)))

(defn begin [options]
  {:pre [(map? options)]}
  (->Transaction [(map->BeginAction options)]))

(defn commit [ta options]
  {:pre [(map? options)
         (:target options)
         (:author options)
         (:committer options)
         (:message options)]}
  (clojure.core/update ta :actions conj (map->CommitAction options)))

(defn create [ta options]
  {:pre [(map? options)]}
  (clojure.core/update ta :actions conj (map->CreateAction options)))

(defn delete [ta options]
  {:pre [(map? options)]}
  (clojure.core/update ta :actions conj (map->DeleteAction options)))

(defn update [ta options]
  {:pre [(map? options)]}
  (clojure.core/update ta :actions conj (map->UpdateAction options)))
