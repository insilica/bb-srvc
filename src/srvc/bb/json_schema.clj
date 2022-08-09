(ns srvc.bb.json-schema
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [insilica.jinx :as jinx]
            [insilica.jinx.resolve :as resolve]))

(def event-schema-filename "event-v1.json")

(def schema-filenames
  [event-schema-filename
   "document-v1.json"
   "label-answer-v1.json"
   "label-v1.json"])

(defn load-schema [filename]
  (-> (str "srvc/bb/json-schema/" filename)
      io/resource io/reader json/read
      jinx/schema))

(def event-schema (delay (load-schema event-schema-filename)))

(defn get-uri-map []
  (->> schema-filenames
       (map (fn [filename]
              [(str "https://insilica.github.io/bb-srvc/json-schema/" filename)
               (load-schema filename)]))
       (into {})))

(def uri-map (delay (get-uri-map)))

(defmethod resolve/resolve-uri ::resolver [_ ^String uri]
  (@uri-map uri))

(defn validate [m]
  (jinx/validate @event-schema m {:resolvers [::resolve/built-in ::resolver]}))
