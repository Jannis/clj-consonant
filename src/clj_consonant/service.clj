(ns clj-consonant.service
  (:require [compojure.core :refer [context defroutes GET rfn]]
            [ring.middleware.format-params :refer [wrap-transit-json-params]]
            [ring.middleware.format-response :refer [wrap-transit-json-response]]
            [ring.util.response :refer [response]]
            [clj-consonant.local-store :refer [local-store]]
            [clj-consonant.store :as store]))

;;;; Consonant store used by the service

(def data-store (atom nil))

(defn load-store [path]
  (reset! data-store (local-store path)))

;;;; Helpers

(defn class-for-uuid [refname uuid]
  (->> (store/get-classes @data-store refname)
       (vals)
       (filter #(some #{{:uuid uuid}} (:objects %)))
       (first)))

;;;; Route handlers

(defn handle-refs []
  (-> (store/get-refs @data-store)
      (response)))

(defn handle-ref [refname]
  (-> (store/get-ref @data-store refname)
      (response)))

(defn handle-classes [refname]
  (-> (store/get-classes @data-store refname)
      (response)))

(defn handle-class [refname class]
  (-> (store/get-class @data-store refname class)
      (response)))

(defn handle-objects
  ([refname]
   (->> (store/get-classes @data-store refname)
        (keys)
        (map #(vector % (store/get-objects @data-store refname %)))
        (into {})
        (response)))
  ([refname class]
   (-> (store/get-objects @data-store refname class)
       (response))))

(defn handle-object
  ([refname uuid]
   (let [class (class-for-uuid refname uuid)]
     (-> (store/get-object @data-store refname (:name class) uuid)
         (response))))
  ([refname class uuid]
   (-> (store/get-object @data-store refname class uuid)
       (response))))

(defn handle-object-class
  ([refname uuid]
   (let [class (class-for-uuid refname uuid)]
     (-> (store/get-object @data-store refname (:name class) uuid)
         :class
         (response))))
  ([refname class uuid]
   (-> (store/get-object @data-store refname class uuid)
       :class
       (response))))

(defn handle-object-properties
  ([refname uuid]
   (let [class (class-for-uuid refname uuid)]
     (-> (store/get-properties @data-store refname (:name class) uuid)
         (response))))
  ([refname class uuid]
   (-> (store/get-properties @data-store refname class uuid)
       (response))))

(defn handle-object-property
  ([refname uuid property]
   (let [class (class-for-uuid refname uuid)]
     (-> (when class
           (store/get-property @data-store refname (:name class) uuid property))
         (response))))
  ([refname class uuid property]
   (-> (store/get-property @data-store refname class uuid property)
       (response))))

;;;; Routes

(defroutes service-routes
  (context "/api"                                                             []
    (context "/1.0"                                                           []
      (GET "/classes"                                                         []                            (handle-classes "HEAD"))
      (GET "/classes/:class"                                                  [class]                       (handle-class "HEAD" class))
      (GET "/classes/:class/objects"                                          [class]                       (handle-objects "HEAD" class))
      (GET "/classes/:class/objects/:uuid"                                    [class uuid]                  (handle-object "HEAD" class uuid))
      (GET "/classes/:class/objects/:uuid/class"                              [class uuid]                  (handle-object-class "HEAD" class uuid))
      (GET "/classes/:class/objects/:uuid/properties"                         [class uuid]                  (handle-object-properties "HEAD" class uuid))
      (GET "/classes/:class/objects/:uuid/properties/:property"               [class uuid property]         (handle-object-property "HEAD" class uuid property))
      (GET "/objects"                                                         []                            (handle-objects "HEAD"))
      (GET "/objects/:uuid"                                                   [uuid]                        (handle-object "HEAD" uuid))
      (GET "/objects/:uuid/class"                                             [uuid]                        (handle-object-class "HEAD" uuid))
      (GET "/objects/:uuid/properties"                                        [uuid]                        (handle-object-properties "HEAD" uuid))
      (GET "/objects/:uuid/properties/:property"                              [uuid property]               (handle-object-property "HEAD" uuid property))
      (GET "/refs"                                                            []                            (handle-refs))
      (GET "/refs/:refname"                                                   [refname]                     (handle-ref refname))
      (GET "/refs/:refname/classes"                                           [refname]                     (handle-classes refname))
      (GET "/refs/:refname/classes/:class"                                    [refname class]               (handle-class refname class))
      (GET "/refs/:refname/classes/:class/objects"                            [refname class]               (handle-objects refname class))
      (GET "/refs/:refname/classes/:class/objects/:uuid"                      [refname class uuid]          (handle-object refname class uuid))
      (GET "/refs/:refname/classes/:class/objects/:uuid/class"                [refname class uuid]          (handle-object-class refname class uuid))
      (GET "/refs/:refname/classes/:class/objects/:uuid/properties"           [refname class uuid]          (handle-object-properties refname class uuid))
      (GET "/refs/:refname/classes/:class/objects/:uuid/properties/:property" [refname class uuid property] (handle-object-property refname class uuid property))
      (GET "/refs/:refname/objects"                                           [refname]                     (handle-objects refname))
      (GET "/refs/:refname/objects/:uuid"                                     [refname uuid]                (handle-object refname uuid))
      (GET "/refs/:refname/objects/:uuid/class"                               [refname uuid]                (handle-object-class refname uuid))
      (GET "/refs/:refname/objects/:uuid/properties"                          [refname uuid]                (handle-object-properties refname uuid))
      (GET "/refs/:refname/objects/:uuid/properties/:property"                [refname uuid property]       (handle-object-property refname uuid property)))))

;;;; Middlewares

(defn wrap-404
  [handler]
  (fn [request]
    (let [resp (handler request)]
      (case (:body resp)
        nil   {:status 404 :body "Not found"}
              resp))))

;;;; Consonant service

(def service
  (do
    (load-store (System/getProperty "consonant-service-store"))
    (-> service-routes
        (wrap-404)
        (wrap-transit-json-params)
        (wrap-transit-json-response :options {:verbose true}))))
