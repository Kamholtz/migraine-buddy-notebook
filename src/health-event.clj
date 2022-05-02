(ns health-event
  (:require [java-time :as jt]
            [clojure.data.csv :as ccsv]
            [clojure.string :as str]
            [clojure.java.io :as jio]
            [csv-parse :as csvp]))

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

(defn get-with-end-formated-end-date
  "Return map `m` with :end-date-formatted based on description text"
  [{:keys [:date :description] :as m}]
  (let [start-date (-> date 
                       csvp/str->start-date-str
                       csvp/str->date)
        is-emgality (str/includes? (str/lower-case description) "emgality")
        days-to-add (if is-emgality 28 1)
        end-date (jt/plus start-date (jt/days days-to-add))]

    (assoc m :end-date-formatted (csvp/dt->iso-datetime end-date))))


(defn get-parsed-health-event-data [data]
  (->> data
       (map (csvp/get-column-parser :date :start-date-formatted
                                    #(-> % 
                                         (csvp/str->start-date-str)
                                         (csvp/str->date)
                                         (csvp/dt->iso-datetime))))
       (map get-with-end-formated-end-date)))
