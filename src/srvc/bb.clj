(ns srvc.bb
  (:refer-clojure :exclude [map])
  (:require [clojure.java.io :as io]
            [insilica.canonical-json :as json]
            [multihash.core :as multihash]
            [multihash.digest :as digest]
            [srvc.bb.json-schema :as bjs]))

(defn unix-time []
  (quot (System/currentTimeMillis) 1000))

(def unhashed-keys
  [(keyword "@context") (keyword "@id") ;; json-ld keys
   (keyword "$id") (keyword "$schema") ;; json-schema keys
   :hash :meta])

(defn json-hash [m]
  (-> (apply dissoc m unhashed-keys)
      json/write-str digest/sha2-256 multihash/base58))

(defn add-hash [m]
  (assoc m :hash (json-hash m)))

(defn write-event [writer event]
  (-> event add-hash (json/write writer))
  (.write writer "\n")
  (.flush writer))

(defn get-config [file]
  (some-> file io/reader
          (json/read :key-fn keyword)))

(defn generate [events]
  (let [out-file (System/getenv "SR_OUTPUT")]
    (with-open [writer (io/writer out-file)]
      (doseq [event events]
        (let [{:keys [errors valid?]} (-> (assoc event :hash "")
                                          json/write-str json/read-str
                                          bjs/validate)]
          (if valid?
            (write-event writer event)
            (throw (ex-info "Event failed validation"
                            {:errors errors :event event}))))))))

(defn map [f]
  (let [config-file (System/getenv "SR_CONFIG")
        in-file (System/getenv "SR_INPUT")
        out-file (System/getenv "SR_OUTPUT")
        config (get-config config-file)]
    (with-open [writer (io/writer out-file)]
      (doseq [line (-> in-file io/reader line-seq)
              :let [event (json/read-str line :key-fn keyword)
                    f-events (f config event)]]
        (doseq [f-event f-events]
          (if (= event f-event)
            (do
              (.write writer line)
              (.write writer "\n")
              (.flush writer))
            (let [{:keys [errors valid?]} (-> (assoc f-event :hash "")
                                              json/write-str json/read-str
                                              bjs/validate)]
              (if valid?
                (write-event writer f-event)
                (throw (ex-info "Event failed validation"
                                {:errors errors :event f-event}))))))))))
