(ns core
  (:require [nextjournal.clerk :as clerk]
            [dk.ative.docjure.spreadsheet :as ss]
            [meta-csv.core :as csv]
            [java-time :as jt]
            [clojure.string :as str]
            [clojure.set :as cset]
            [clojure.java.io :as jio]))


(def fields  [{:field :index, :type :long}
              {:field :date, :type :string}
              {:field :lasted, :type :string}
              {:field :pain-level, :type :long}
              {:field :affected-activities, :type :string}
              {:field :potential-triggers, :type :string}
              {:field :symptoms, :type :string}
              {:field :most-bothersome-symptom, :type :string}
              {:field :auras, :type :string}
              {:field :pain-positions, :type :string}
              {:field :helpful-medication, :type :string}
              {:field :somewhat-helpful-medication, :type :string}
              {:field :unhelpful-medication, :type :string}
              {:field :unsure-medication, :type :string}
              {:field :helpful-non-drug-relief-methods, :type :string}
              {:field :somewhat-helpful-non-drug-relief-methods, :type :string}
              {:field :unhelpful-non-drug-relief-methods, :type :string}
              {:field :unsure-non-drug-relief-methods, :type :string}
              {:field :notes, :type :string}])

(comment 
  ; Learning java-time
  (jt/local-date-time "d/MM/yy HH:mm" "4/04/22 06:55")
  (jt/local-date-time "d/MM/yy HH:mm" "31/01/22 07:42")

  (str (jt/local-date-time "d/MM/yy HH:mm" "29/04/22 07:03")) ; "2022-04-29T07:03"
  (.getMinute (jt/local-date-time "d/MM/yy HH:mm" "29/04/22 07:03"))
  (jt/as-map (jt/local-date-time "d/MM/yy HH:mm" "29/04/22 07:03"))

  (-> (jt/local-date-time "dd/MM/yy HH:mm" "21/12/21 22:05")
      (jt/as :minutes-of-hour))
  )


(def mb-path "./datasets/MigraineBuddy_20211216_20220430_1651315369524_-555206987.csv")

(defn get-health-event-line-count 
  "Return a count of lines in CSV that are health events, including header"
  [path]
  (with-open [rdr (jio/reader path)]
    (let [lseq (line-seq rdr) ]
      (count 
        (->> lseq
             (take-while #(not (str/blank? %)))
             doall)))))

(def num-health-event-lines 
  (get-health-event-line-count mb-path))

; (def health-event-data 
;   (csv/read-csv mb-path {:header? true }))

(def mb-data
  (csv/read-csv mb-path {:header? false :skip 6 :fields fields}))

(defn str->datetime 
  "Get a datetime object"
  [date-str]
  (jt/local-date-time "d/MM/yy HH:mm" date-str))

(defn str->iso-datetime 
  "Parses to an iso datetime str... I think?"
  [date-str]
  (jt/format (str->datetime date-str) ))

(comment 
  (str->iso-datetime "29/04/22 07:03")
  )

(defn str->date-as-map [date-str] 
  (jt/as-map (str->datetime date-str)))

(defn str->vl-datetime [date-str]
  (let [dt (str->datetime date-str)]
    (-> (jt/as-map dt)
        (select-keys [:year :month-of-year :day-of-month 
                      :hour-of-day :second-of-minute])
        (cset/rename-keys  {; Year as is
                            :month-of-year :month
                            :day-of-month :date

                            :hour-of-day :hours
                            :second-of-minute :seconds })
        (assoc :minutes (.getMinute dt)))))

(defn csv->col [csv-str]
  (str/split csv-str #","))

(comment 
  (csv->col "Right Front Head, Left Front Head, Left Eye, Right Eye")
  (csv->col nil))

(defn get-column-parser 
  ([col-from col-to parse-fn]
   (fn [row]
     (let [str-in (col-from row)]
       (if (nil? str-in)
         row ; Do nothing if nil, there's no reasonable way to parse
         (assoc row col-to (parse-fn str-in))))))

  ([col-key parse-fn]
   (get-column-parser col-key col-key parse-fn)))

(def parsed-mb-data
  (->> mb-data
       (map (get-column-parser :date :date-formatted str->iso-datetime))
       (map (get-column-parser :date :date-as-map str->date-as-map))
       (map (get-column-parser :affected-activities csv->col))
       (map (get-column-parser :potential-triggers csv->col))
       (map (get-column-parser :symptoms csv->col))
       (map (get-column-parser :auras csv->col))
       (map (get-column-parser :helpful-medication csv->col))
       (map (get-column-parser :somewhat-helpful-medication csv->col))
       (map (get-column-parser :unhelpful-medication csv->col))
       (map (get-column-parser :unsure-medication csv->col))
       (map (get-column-parser :helpful-non-drug-relief-methods csv->col))
       (map (get-column-parser :somewhat-helpful-non-drug-relief-methods csv->col))
       (map (get-column-parser :unhelpful-non-drug-relief-methods csv->col))
       (map (get-column-parser :unsure-non-drug-relief-methods csv->col))
       (map (get-column-parser :pain-positions csv->col))

       ))

; (clerk/table parsed-mb-data)

(clerk/html [:h1 "Migraine intensity/day of year"])

(clerk/vl {:width 675
           :height 400
           :data {:values parsed-mb-data}
           :mark "bar"
           :encoding {:x {:field :date-formatted :type :temporal :title "Date"}
                      :y {:field :pain-level :type :quantitative :title "Pain Level (0 - 10)"}}})


(def grouped-mb-data
  (->> parsed-mb-data
       ; Group by year/week of year
       (group-by #(vector (get-in % [:date-as-map :week-based-year])
                          (get-in % [:date-as-map :week-of-week-based-year])))
       ; Count items in group
       (map (fn [[group-key group]] {:year (first group-key) 
                                     :week (second group-key)
                                     :year-and-week group-key
                                     :migraines-in-week (count group)}))

       (sort-by (juxt :year :week))))

(clerk/html [:h1 "Migraine count/week of year"])

(clerk/vl {:width 675
           :height 400
           :data {:values grouped-mb-data}
           :mark "bar"
           :encoding {:x {:field :year-and-week :type :nominal :title "Year and Week"}
                      :y {:field :migraines-in-week :type :quantitative :title "# Migraines/Week" :scale {:domain [0 10]}}} })


; (clerk/vl {:width 675
;            :height 400
;            :data {:values grouped-mb-data}
;            :mark "bar"
;            :encoding {:x {:field :year-and-week :type :nominal :title "Year and Week"}
;                       :y {:field :migraines-in-week :type :quantitative :title "# Migraines/Week" :scale {:domain [0 10]}}} })
