(ns build
  (:refer-clojure :exclude [test])
  (:require [org.corfield.build :as bb]))

(def lib 'co.insilica/bb-srvc)
(def version "0.7.0")
(defn get-version [opts]
  (str version (when (:snapshot opts) "-SNAPSHOT")))

(defn test [opts]
  (-> opts
      bb/clean
      (assoc :main-args ["-m" "kaocha.runner"])
      (bb/run-task [:test]))
  opts)

(defn jar [opts]
  (-> opts
      (assoc :lib lib :version (get-version opts))
      bb/clean
      (assoc :src-pom "template/pom.xml")
      bb/jar))

(defn ci "Run the CI pipeline of tests (and build the JAR)." [opts]
  (-> opts test jar))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (-> opts
      (assoc :lib lib :version (get-version opts))
      bb/deploy))
