(ns clj-consonant.debug)

(defn print-and-return
  ([x]   (println x) x)
  ([s x] (println s x) x))
