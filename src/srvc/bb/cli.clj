(ns srvc.bb.cli
  (:gen-class)
  (:require [clojure.string :as str]
            [srvc.bb.review :as review]))

(defn usage []
  (println "Usage: sr review flow-id"))

(defn run-command [& [command & args]]
  (let [command (some-> command str/lower-case)]
    (case command
      nil (usage)
      "review" (apply review/review args)
      "pull" (apply review/pull args)
      "push" (apply review/push args)
      "sync" (apply review/sync args)
      (do (println "Unknown command" (pr-str command))
          (System/exit 1)))))

(defn -main [& args]
  (apply run-command args))
