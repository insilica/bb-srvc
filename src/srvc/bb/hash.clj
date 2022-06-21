(ns srvc.bb.hash
  (:refer-clojure :exclude [hash])
  (:require [insilica.canonical-json :as json]
            [multihash.core :as multihash]
            [multihash.digest :as digest]))

(defn hash [m]
  (-> (dissoc m :hash :meta) json/write-str digest/sha2-256 multihash/base58))

(defn add-hash [m]
  (assoc m :hash (hash m)))
