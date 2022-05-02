(ns health-event
  (:require [clojure.data.csv :as ccsv]
            [clojure.string :as str]
            [clojure.java.io :as jio]))

(defn get-health-event-line-count 
  "Return a count of lines in CSV that are health events, including header"
  [path]
  (with-open [rdr (jio/reader path)]
    (let [lseq (line-seq rdr) ]
      (count 
        (->> lseq
             (take-while #(not (str/blank? %)))
             doall)))))

(defn- is-empty-csv-col 
  "Returns true if `col` contains one blank string"
  [col]
  (and (= (count col) 1)
       (str/blank? (first col))))


(defn- get-mb-health-event-csv 
  "MB health data as a collection of vectors until the first empty line in csv"
  [path]
  (with-open [rdr (jio/reader path)]
    (let [data (ccsv/read-csv rdr)]
      (->> data 
           (take-while #(not (is-empty-csv-col %)))
           doall))))


(defn get-mb-health-event-data 
  "MB health data as a collection of maps"
  [path]
  (let [csv-data (get-mb-health-event-csv path)]
    (map zipmap 
         (repeat [:date :time-period :description :notes])
         (rest csv-data))))
