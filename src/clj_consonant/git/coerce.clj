(ns clj-consonant.git.coerce
  (:require [clojure.string :as str]))

(defn to-sha1 [oid]
  (.getName oid))

(defn to-oid [repo sha1]
  (.resolve (.getRepository repo) sha1))

(defn to-refname [alias]
  (str/replace alias #":" "/"))

(defn to-alias [refname]
  (str/replace refname #"/" ":"))
