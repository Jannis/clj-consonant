(ns clj-consonant.service
  (:require [compojure.core :refer [context defroutes ANY OPTIONS POST]]
            [environ.core :refer [env]]
            [ring.middleware.format-params :refer [wrap-transit-json-params]]
            [ring.middleware.format-response :refer [wrap-transit-json-response]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.util.response :refer [response header]]
            [clj-consonant.debug :refer [print-and-return]]
            [clj-consonant.local-store :refer [local-store]]
            [clj-consonant.store :as store]))

;;;; Consonant store used by the service

(def data-store (atom nil))

(defn load-store [path]
  (reset! data-store (local-store path)))

;;;; Helpers

(defn class-for-uuid [ref-name uuid]
  (->> (store/get-classes @data-store ref-name)
       (vals)
       (filter #(some #{{:uuid uuid}} (:objects %)))
       (first)))

;;;; Route handlers

(defn handle-refs []
  (-> (store/get-refs @data-store)
      (response)))

(defn handle-ref [ref-name]
  (-> (store/get-ref @data-store ref-name)
      (response)))

(defn handle-classes [ref-name]
  (-> (store/get-classes @data-store ref-name)
      (response)))

(defn handle-class [ref-name class]
  (-> (store/get-class @data-store ref-name class)
      (response)))

(defn handle-objects
  ([ref-name]
   (->> (store/get-classes @data-store ref-name)
        (print-and-return "> classes")
        (keys)
        (print-and-return "> class names")
        (map #(vector % (store/get-objects @data-store ref-name %)))
        (print-and-return "> with objects")
        (into {})
        (response)))
  ([ref-name class]
   (-> (store/get-objects @data-store ref-name class)
       (response))))

(defn handle-object
  ([ref-name uuid]
   (let [class (class-for-uuid ref-name uuid)]
     (-> (store/get-object @data-store ref-name (:name class) uuid)
         (response))))
  ([ref-name class uuid]
   (-> (store/get-object @data-store ref-name class uuid)
       (response))))

(defn handle-object-class
  ([ref-name uuid]
   (let [class (class-for-uuid ref-name uuid)]
     (-> (store/get-object @data-store ref-name (:name class) uuid)
         :class
         (response))))
  ([ref-name class uuid]
   (-> (store/get-object @data-store ref-name class uuid)
       :class
       (response))))

(defn handle-object-properties
  ([ref-name uuid]
   (let [class (class-for-uuid ref-name uuid)]
     (-> (store/get-properties @data-store ref-name (:name class) uuid)
         (response))))
  ([ref-name class uuid]
   (-> (store/get-properties @data-store ref-name class uuid)
       (response))))

(defn handle-object-property
  ([ref-name uuid property]
   (let [class (class-for-uuid ref-name uuid)]
     (-> (when class
           (store/get-property @data-store ref-name (:name class) uuid property))
         (response))))
  ([ref-name class uuid property]
   (-> (store/get-property @data-store ref-name class uuid property)
       (response))))

(defn handle-transaction
  [actions]
  (when actions
    (-> (store/transact! @data-store actions)
        (response))))

;;;; Routes

(defroutes service-routes
  (context "/api"                                                              []
    (context "/1.0"                                                            []
      (ANY "/classes"                                                          []                            (handle-classes "HEAD"))
      (ANY "/classes/:class"                                                   [class]                       (handle-class "HEAD" class))
      (ANY "/classes/:class/objects"                                           [class]                       (handle-objects "HEAD" class))
      (ANY "/classes/:class/objects/:uuid"                                     [class uuid]                  (handle-object "HEAD" class uuid))
      (ANY "/classes/:class/objects/:uuid/class"                               [class uuid]                  (handle-object-class "HEAD" class uuid))
      (ANY "/classes/:class/objects/:uuid/properties"                          [class uuid]                  (handle-object-properties "HEAD" class uuid))
      (ANY "/classes/:class/objects/:uuid/properties/:property"                [class uuid property]         (handle-object-property "HEAD" class uuid property))
      (ANY "/objects"                                                          []                            (handle-objects "HEAD"))
      (ANY "/objects/:uuid"                                                    [uuid]                        (handle-object "HEAD" uuid))
      (ANY "/objects/:uuid/class"                                              [uuid]                        (handle-object-class "HEAD" uuid))
      (ANY "/objects/:uuid/properties"                                         [uuid]                        (handle-object-properties "HEAD" uuid))
      (ANY "/objects/:uuid/properties/:property"                               [uuid property]               (handle-object-property "HEAD" uuid property))
      (ANY "/refs"                                                             []                            (handle-refs))
      (ANY "/refs/:ref-name"                                                   [ref-name]                     (handle-ref ref-name))
      (ANY "/refs/:ref-name/classes"                                           [ref-name]                     (handle-classes ref-name))
      (ANY "/refs/:ref-name/classes/:class"                                    [ref-name class]               (handle-class ref-name class))
      (ANY "/refs/:ref-name/classes/:class/objects"                            [ref-name class]               (handle-objects ref-name class))
      (ANY "/refs/:ref-name/classes/:class/objects/:uuid"                      [ref-name class uuid]          (handle-object ref-name class uuid))
      (ANY "/refs/:ref-name/classes/:class/objects/:uuid/class"                [ref-name class uuid]          (handle-object-class ref-name class uuid))
      (ANY "/refs/:ref-name/classes/:class/objects/:uuid/properties"           [ref-name class uuid]          (handle-object-properties ref-name class uuid))
      (ANY "/refs/:ref-name/classes/:class/objects/:uuid/properties/:property" [ref-name class uuid property] (handle-object-property ref-name class uuid property))
      (ANY "/refs/:ref-name/objects"                                           [ref-name]                     (handle-objects ref-name))
      (ANY "/refs/:ref-name/objects/:uuid"                                     [ref-name uuid]                (handle-object ref-name uuid))
      (ANY "/refs/:ref-name/objects/:uuid/class"                               [ref-name uuid]                (handle-object-class ref-name uuid))
      (ANY "/refs/:ref-name/objects/:uuid/properties"                          [ref-name uuid]                (handle-object-properties ref-name uuid))
      (ANY "/refs/:ref-name/objects/:uuid/properties/:property"                [ref-name uuid property]       (handle-object-property ref-name uuid property))
      (ANY "/transactions"                                                     {params :body-params}         (handle-transaction params)))))

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
  (when-let [store-dir (System/getProperty "consonant-service-store")]
    (load-store store-dir)
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
