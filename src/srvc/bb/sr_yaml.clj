(ns srvc.bb.sr-yaml
  (:require [clj-yaml.core :as yaml]
            [clojure.string :as str]
            [srvc.bb :as sb]))

(defn canonical-label [id label]
  (-> label
      (assoc :id (str/lower-case (name id)))
      (update :required boolean)
      (update :type str/lower-case)
      sb/add-hash))

(defn parse-labels [labels]
  (->> labels
       (map (fn [[k v]] [k (canonical-label k v)]))
       (into {})))

(defn load-from-file [filename]
  (-> filename slurp yaml/parse-string
      (update :labels parse-labels)))
