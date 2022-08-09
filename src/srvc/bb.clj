(ns srvc.bb
  (:refer-clojure :exclude [map])
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [srvc.bb.json-schema :as bjs]))

(defn unix-time []
  (quot (System/currentTimeMillis) 1000))

(defn write-event [writer event]
  (json/write event writer)
  (.write writer "\n")
  (.flush writer))

(defn get-config []
  (some-> (System/getenv "SR_CONFIG")
          io/reader
          (json/read :key-fn keyword)))

(defn socket [addr]
  (let [[host port] (str/split addr #"\:")]
    (java.net.Socket. host (parse-long port))))

(defn get-input-reader []
  (io/reader (socket (System/getenv "SR_INPUT"))))

(defn get-input-lines []
  (line-seq (get-input-reader)))

(defn get-output-writer []
  (io/writer (socket (System/getenv "SR_OUTPUT"))))

(defn generate [events]
  (with-open [writer (get-output-writer)]
    (doseq [event events]
      (let [{:keys [errors valid?]} (-> (assoc event :hash "")
                                        json/write-str json/read-str
                                        bjs/validate)]
        (if valid?
          (write-event writer event)
          (throw (ex-info "Event failed validation"
                          {:errors errors :event event})))))))

(defn map [f]
  (let [config (get-config)]
    (with-open [writer (get-output-writer)]
      (doseq [line (get-input-lines)
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
