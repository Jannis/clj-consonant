(ns clj-consonant.store)

(defprotocol Store
  "Standard interface to access local and remote Consonant stores."
  (connect [this] "Connect to a store")
  (disconnect [this] "Disconnect from a store")
  (get-refs [this] "Return a map of all refs in the store")
  (get-ref [this ref-alias] "Return the ref for the given alias")
  (get-classes [this] [this ref-alias]
    "Return classes and their objects for the given ref alias (or
      HEAD if none is provided)")
  (get-class [this class-name] [this ref-alias class-name]
    "Return the class and its objects for the given class name
     and ref alias (or HEAD if none is provided)")
  (get-objects [this class-name] [this ref-alias class-name]
    "Return the objects of a class in the given ref (or HEAD if
     none is provided)")
  (get-object [this class-name uuid] [this ref-alias class-name uuid]
    "Return the object with the given UUID inside the given class and
     ref (or HEAD if none is provided)")
  (get-properties [this class-name uuid] [this ref-alias class-name uuid]
    "Return the properties of the object with the given UUID inside the
     given class and ref (or HEAD if none is provided)")
  (get-property [this class-name uuid name] [this ref-alias class-name uuid name]
    "Return the property with the given name of the object with the given UUID
     inside the given class and ref (or HEAD if none is provided)")
  (transact! [this transaction]
    "Applies a transaction"))
