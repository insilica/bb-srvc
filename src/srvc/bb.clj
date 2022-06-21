(ns srvc.bb
  (:require [clojure.java.io :as io]
            [insilica.canonical-json :as json]
            [multihash.core :as multihash]
            [multihash.digest :as digest]))

(defn json-hash [m]
  (-> (dissoc m :hash :meta) json/write-str digest/sha2-256 multihash/base58))

(defn add-hash [m]
  (assoc m :hash (json-hash m)))

(defn write-event [writer event]
  (-> event add-hash (json/write writer))
  (.write writer "\n")
  (.flush writer))

(defn generate [documents]
  (let [[_ out-file] *command-line-args*]
    (with-open [writer (io/writer out-file)]
      (doseq [doc documents]
        (write-event writer doc)))))
