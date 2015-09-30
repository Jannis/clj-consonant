(ns clj-consonant.git.ident
  (:refer-clojure :exclude [load]))

(defrecord Identity [name email date utc-offset])

(defn load [ident]
  (->Identity (.getName ident)
              (.getEmailAddress ident)
              (.getWhen ident)
              (.getTimeZoneOffset ident)))
