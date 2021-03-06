(ns clj-consonant.client
  (:require [com.stuartsierra.component :as component]
            [clojure.string :as str]
            [ajax.core :refer [GET POST]]
            [clj-consonant.actions :refer [ITransaction actions]]))

(defrecord ServiceClient [url]
  component/Lifecycle
  (start [component]
    component)
  (stop [component]
    component))

(defn new-client [url]
  (ServiceClient. url))

(defn make-url [client & segments]
  (str/join "/" (cons (:url client) segments)))

(defn get-refs [client handler error-handler]
  (GET (make-url client "refs")
       {:handler handler :error-handler error-handler}))

(defn get-ref [client ref-name handler error-handler]
  (GET (make-url client "refs" ref-name)
       {:handler handler :error-handler error-handler}))

(defn get-objects
  ([client ref-name handler error-handler]
   (GET (make-url client "refs" ref-name "objects")
        {:handler handler :error-handler error-handler}))
  ([client ref-name class handler error-handler]
   (GET (make-url client "refs" ref-name "classes" class "objects")
        {:handler handler :error-handler error-handler})))

(defn transact! [client handler error-handler ta]
  {:pre [(satisfies? ITransaction ta)]}
  (POST (make-url client "transactions")
        {:params (actions ta)
         :handler handler
         :eror-handler error-handler}))
