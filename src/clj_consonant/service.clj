(ns clj-consonant.service
  (:require [compojure.core :refer [context defroutes ANY OPTIONS POST]]
            [environ.core :refer [env]]
            [ring.middleware.format-params :refer [wrap-transit-json-params]]
            [ring.middleware.format-response :refer [wrap-transit-json-response]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.util.response :refer [response header]]
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

(defn handle-transaction
  [actions]
  (when actions
    (-> (store/transact! @data-store actions)
        (response))))

;;;; Routes

(defroutes service-routes
  (context "/api"                                                             []
    (context "/1.0"                                                           []
      (ANY "/classes"                                                         []                            (handle-classes "HEAD"))
      (ANY "/classes/:class"                                                  [class]                       (handle-class "HEAD" class))
      (ANY "/classes/:class/objects"                                          [class]                       (handle-objects "HEAD" class))
      (ANY "/classes/:class/objects/:uuid"                                    [class uuid]                  (handle-object "HEAD" class uuid))
      (ANY "/classes/:class/objects/:uuid/class"                              [class uuid]                  (handle-object-class "HEAD" class uuid))
      (ANY "/classes/:class/objects/:uuid/properties"                         [class uuid]                  (handle-object-properties "HEAD" class uuid))
      (ANY "/classes/:class/objects/:uuid/properties/:property"               [class uuid property]         (handle-object-property "HEAD" class uuid property))
      (ANY "/objects"                                                         []                            (handle-objects "HEAD"))
      (ANY "/objects/:uuid"                                                   [uuid]                        (handle-object "HEAD" uuid))
      (ANY "/objects/:uuid/class"                                             [uuid]                        (handle-object-class "HEAD" uuid))
      (ANY "/objects/:uuid/properties"                                        [uuid]                        (handle-object-properties "HEAD" uuid))
      (ANY "/objects/:uuid/properties/:property"                              [uuid property]               (handle-object-property "HEAD" uuid property))
      (ANY "/refs"                                                            []                            (handle-refs))
      (ANY "/refs/:refname"                                                   [refname]                     (handle-ref refname))
      (ANY "/refs/:refname/classes"                                           [refname]                     (handle-classes refname))
      (ANY "/refs/:refname/classes/:class"                                    [refname class]               (handle-class refname class))
      (ANY "/refs/:refname/classes/:class/objects"                            [refname class]               (handle-objects refname class))
      (ANY "/refs/:refname/classes/:class/objects/:uuid"                      [refname class uuid]          (handle-object refname class uuid))
      (ANY "/refs/:refname/classes/:class/objects/:uuid/class"                [refname class uuid]          (handle-object-class refname class uuid))
      (ANY "/refs/:refname/classes/:class/objects/:uuid/properties"           [refname class uuid]          (handle-object-properties refname class uuid))
      (ANY "/refs/:refname/classes/:class/objects/:uuid/properties/:property" [refname class uuid property] (handle-object-property refname class uuid property))
      (ANY "/refs/:refname/objects"                                           [refname]                     (handle-objects refname))
      (ANY "/refs/:refname/objects/:uuid"                                     [refname uuid]                (handle-object refname uuid))
      (ANY "/refs/:refname/objects/:uuid/class"                               [refname uuid]                (handle-object-class refname uuid))
      (ANY "/refs/:refname/objects/:uuid/properties"                          [refname uuid]                (handle-object-properties refname uuid))
      (ANY "/refs/:refname/objects/:uuid/properties/:property"                [refname uuid property]       (handle-object-property refname uuid property))
      (ANY "/transactions"                                                    {params :body-params}         (handle-transaction params)))))

;;;; Middlewares

(defn wrap-404
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (not= :options (:request-method request))
        (case (:body response)
          nil   {:status 404 :body "Not found"}
                response)
        response))))

(defn wrap-access-headers
  [handler]
  (fn [request]
    (let [response (handler request)]
      (-> response
          (header "Access-Control-Allow-Origin" "*")
          (header "Access-Control-Allow-Headers" "Accept, Content-Type")
          (header "Access-Control-Allow-Methods"
                  "GET, HEAD, OPTIONS, POST, PUT")))))

;;;; Consonant service

(onelog.core/set-debug!)

(def service
  (do
    (load-store (System/getProperty "consonant-service-store"))
    (-> service-routes
        ; (wrap-with-logger
        ;   :info  #(println %)
        ;   :debug #(println %)
        ;   :error #(println %)
        ;   :warn  #(println %))
        (wrap-404)
        (wrap-access-headers)
        (wrap-transit-json-params)
        (wrap-transit-json-response :options {:verbose true}))))
