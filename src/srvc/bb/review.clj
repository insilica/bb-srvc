(ns srvc.bb.review
  (:require [babashka.curl :as curl]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [insilica.canonical-json :as json]
            [srvc.bb :as sb]
            [srvc.bb.sr-yaml :as sr-yaml]))

(def default-opts
  {:inherit true
   :shutdown p/destroy-tree})

(def default-sink-step {:run "sink/sink.clj"})
(def remote-sink-step {:run "sink/sink-remote.clj"})

(defn process [args & [opts]]
  (p/process args (merge default-opts opts)))

(defn make-fifo [path]
  @(p/process ["mkfifo" (str path)])
  path)

(defn step-labels [{:keys [labels]} step]
  (->> step :labels
       (map #(sb/add-hash {:data (labels (keyword %)) :type "label"}))))

(defn step-config [config step]
  (cond-> (assoc config :current_step step)
    (seq (:labels step)) (assoc :current_labels (step-labels config step))))

(defn write-step-config [config dir step]
  (let [config-json (str (fs/path dir (str (random-uuid) ".json")))]
    (-> (step-config config step)
        json/write-str
        (->> (spit config-json)))
    config-json))

(defn remote-target? [s]
  (let [lc (str/lower-case s)]
    (or (str/starts-with? lc "http://")
        (str/starts-with? lc "https://"))))

(defn api-route [target & path-parts]
  (str target (when-not (str/ends-with? target "/") "/")
       "api/v1/" (str/join "/" path-parts)))

(defn make-remote-in-file [in-source dir]
  (let [in-file (-> (fs/path dir (str (random-uuid) ".fifo")) make-fifo str)
        server-hashes (->> (curl/get (api-route in-source "hashes") {:as :stream})
                           :body io/reader line-seq
                           (map json/read-str))]
    (future
      (with-open [writer (io/writer in-file)]
        (doseq [hash server-hashes]
          (->> (curl/get (api-route in-source "hash" hash))
               :body
               (.write writer))
          (.write writer "\n")
          (.flush writer))))
    in-file))

(defn docker-volume [filename]
  (if (fs/absolute? filename)
    (str "type=bind,src=" filename ",dst=" filename)
    (str "type=bind,src=" (fs/absolutize filename)
         ",dst=" (str "/" filename))))

(defn docker-volumes [filenames]
  (mapcat #(do ["--mount" (docker-volume %)]) filenames))

(defn step-process [dir
                    {:keys [image run]}
                    {:keys [config-json in-file out-file]}]
  (let [args (filter identity [config-json in-file out-file])
        opts {:extra-env {"SR_CONFIG" config-json
                          "SR_INPUT" in-file
                          "SR_OUTPUT" out-file}}]
    (if-not image
      (process (into ["perl" run] args) opts)
      (let [docker-run-args ["docker" "run" "--rm"
                             "--network=host"
                             "-v" (str dir ":" dir)
                             "-e" (str "SR_CONFIG=" config-json)
                             "-e" (str "SR_INPUT=" in-file)
                             "-e" (str "SR_OUTPUT=" out-file)
                             "-it" image]
            run-file (-> (fs/path dir (str (random-uuid))))]
        (if-not run
          (process docker-run-args opts)
          (do
            (fs/copy run run-file)
            (process (concat docker-run-args ["perl" run-file] args) opts)))))))

(defn push-to-target [in-source out-file]
  (when-not (or (remote-target? out-file) (fs/exists? out-file))
    (-> out-file fs/absolutize fs/parent fs/create-dirs)
    (fs/create-file out-file))
  (fs/with-temp-dir [dir {:prefix "srvc"}]
    (let [config (-> (sr-yaml/load-from-file "sr.yaml")
                     (assoc :db out-file))
          step (if (remote-target? out-file)
                 remote-sink-step
                 default-sink-step)
          config-json (write-step-config config dir step)
          in-file (if (remote-target? in-source)
                    (make-remote-in-file in-source dir)
                    in-source)]
      @(step-process dir step [config-json in-file]))))

(defn pull [target]
  (let [{:keys [db]} (sr-yaml/load-from-file "sr.yaml")]
    (push-to-target target db)))

(defn push [target]
  (let [{:keys [db]} (sr-yaml/load-from-file "sr.yaml")]
    (push-to-target db target)))

(defn sync [target]
  (pull target)
  (push target))

(defn review [flow-name]
  (fs/with-temp-dir [dir {:prefix "srvc"}]
    (let [{:keys [db] :as config} (sr-yaml/load-from-file "sr.yaml")
          {:keys [steps]} (get-in config [:flows (keyword flow-name)])
          steps (concat steps [(if (remote-target? db)
                                 remote-sink-step
                                 default-sink-step)])]
      (loop [[step & more] steps
             in-file nil]
        (let [config-json (write-step-config config dir step)]
          (if more
            (let [out-file (-> (fs/path dir (str (random-uuid) ".fifo")) make-fifo str)
                  add-hashes-step {:run (-> *file*
                                            fs/real-path ; Resolve symlinks
                                            fs/parent
                                            (fs/path "map" "add-hashes.clj")
                                            str)}
                  add-hashes-config-json (write-step-config config dir add-hashes-step)
                  add-hashes-out-file (-> (fs/path dir (str (random-uuid) ".fifo")) make-fifo str)]
              (step-process dir step {:config-json config-json
                                      :in-file in-file
                                      :out-file out-file})
              (step-process dir
                            add-hashes-step
                            {:config-json add-hashes-config-json
                             :in-file out-file
                             :out-file add-hashes-out-file})
              (recur more add-hashes-out-file))
            @(step-process dir step {:config-json config-json
                                     :in-file in-file})))))))
