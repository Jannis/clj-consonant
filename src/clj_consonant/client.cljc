(ns clj-consonant.client
  (:require [com.stuartsierra.component :as component]
            [clojure.string :as str]
            [ajax.core :refer [GET POST]]))

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

(defn get-ref [client refname handler error-handler]
  (GET (make-url client "refs" refname)
       {:handler handler :error-handler error-handler}))

(defn get-objects
  ([client refname handler error-handler]
   (GET (make-url client "refs" refname "objects")
        {:handler handler :error-handler error-handler}))
  ([client refname class handler error-handler]
   (GET (make-url client "refs" refname "classes" class "objects")
        {:handler handler :error-handler error-handler})))

(defn transact! [client handler error-handler actions]
  (POST (make-url client "transactions")
        {:params actions
         :handler handler
         :eror-handler error-handler}))
