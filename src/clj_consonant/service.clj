(ns clj-consonant.service
  (:require [compojure.core :refer [context defroutes GET]]
            [clj-consonant.local-store :refer [local-store]]
            [clj-consonant.store :as store]))

(def data-store (atom nil))

(defn start-service []
  (reset! data-store (local-store "/tmp/clj-consonant-test")))

(defn stop-service [])

(defroutes service-routes
  (context "/api"                                                             []
    (context "/1.0"                                                           []
      (GET "/classes"                                                         []                            "TODO")
      (GET "/classes/:class"                                                  [class]                       "TODO")
      (GET "/classes/:class/objects"                                          [class]                       "TODO")
      (GET "/classes/:class/objects/:uuid"                                    [class uuid]                  "TODO")
      (GET "/classes/:class/objects/:uuid/class"                              [class uuid]                  "TODO")
      (GET "/classes/:class/objects/:uuid/properties"                         [class uuid]                  "TODO")
      (GET "/classes/:class/objects/:uuid/properties/:property"               [class uuid property]         "TODO")
      (GET "/objects"                                                         []                            "TODO")
      (GET "/objects/:uuid"                                                   [uuid]                        "TODO")
      (GET "/objects/:uuid/class"                                             [uuid]                        "TODO")
      (GET "/objects/:uuid/properties"                                        [uuid]                        "TODO")
      (GET "/objects/:uuid/properties/:property"                              [uuid property]               "TODO")
      (GET "/refs"                                                            []                            "TODO")
      (GET "/refs/:refname"                                                   [refname]                     "TODO")
      (GET "/refs/:refname/classes/:class"                                    [refname class]               "TODO")
      (GET "/refs/:refname/classes/:class/objects"                            [refname class]               "TODO")
      (GET "/refs/:refname/classes/:class/objects/:uuid"                      [refname class uuid]          "TODO")
      (GET "/refs/:refname/classes/:class/objects/:uuid/class"                [refname class uuid]          "TODO")
      (GET "/refs/:refname/classes/:class/objects/:uuid/properties"           [refname class uuid]          "TODO")
      (GET "/refs/:refname/classes/:class/objects/:uuid/properties/:property" [refname class uuid property] "TODO")
      (GET "/refs/:refname/objects"                                           [refname]                     "TODO")
      (GET "/refs/:refname/objects/:uuid"                                     [refname uuid]                "TODO")
      (GET "/refs/:refname/objects/:uuid/class"                               [refname uuid]                "TODO")
      (GET "/refs/:refname/objects/:uuid/properties"                          [refname uuid]                "TODO")
      (GET "/refs/:refname/objects/:uuid/properties/:property"                [refname uuid property]       "TODO"))))

(def service
  (-> service-routes))
