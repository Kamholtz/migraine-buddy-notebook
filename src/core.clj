(ns core
  (:require [nextjournal.clerk :as clerk]
            [dk.ative.docjure.spreadsheet :as ss]
            [meta-csv.core :as csv]
            [java-time :as jt]
            [clojure.string :as str]
            [clojure.set :as cset]))


(def fields  [{:field :index, :type :long}
              {:field :date, 
               :type :string 
               ; :preprocess-fn 
               }
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
  (print last-date)
  ; Learning java-time
  (jt/local-date-time "d/MM/yy HH:mm" "4/04/22 06:55")
  (jt/local-date-time "d/MM/yy HH:mm" "31/01/22 07:42")
  (jt/local-date-time "d/MM/yy HH:mm" "26/12/21 06:26")

  (jt/local-date-time "d/MM/yy HH:mm" "2/03/22 06:54")
  (jt/local-date-time "d/MM/yy HH:mm" "28/02/22 16:29")
  (jt/local-date-time "d/MM/yy HH:mm" "26/02/22 07:49")


  (str (jt/local-date-time "d/MM/yy HH:mm" "29/04/22 07:03")) ; "2022-04-29T07:03"
  (jt/get-minute (jt/local-date-time "d/MM/yy HH:mm" "29/04/22 07:03"))

  (.getMinute (jt/local-date-time "d/MM/yy HH:mm" "29/04/22 07:03"))

  (jt/as-map (jt/local-date-time "d/MM/yy HH:mm" "29/04/22 07:03"))
; {:proleptic-month 24267,
;  :julian-day 2459699,
;  :week-based-year 2022,
;  :hour-of-am-pm 7,
;  :nano-of-second 0,
;  :era 1,
;  :rata-die 738274,
;  :micro-of-day 25380000000,
;  :day-of-week 5,
;  :hour-of-day 7,
;  :month-of-year 4,
;  :milli-of-second 0,
;  :micro-of-second 0,
;  :day-of-month 29,
;  :year 2022,
;  :day-of-year 119,
;  :day-of-quarter 29,
;  :year-of-era 2022,
;  :nano-of-day 25380000000000,
;  :second-of-minute 0,
;  :second-of-day 25380,
;  :modified-julian-day 59698,
;  :week-of-week-based-year 17,
;  :milli-of-day 25380000,
;  :quarter-of-year 2}

  (jt/as-map (jt/local-date-time "d/MM/yy HH:mm" "29/04/22 07:03"))


  (-> (jt/local-date-time "dd/MM/yy HH:mm" "21/12/21 22:05")
      (jt/as :minutes-of-hour))
  
  )


(def mb-path "./datasets/MigraineBuddy_20211216_20220430_1651315369524_-555206987.csv")

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

(comment 
  
  (str->vl-datetime "21/12/21 22:15")

  )

(defn csv->col [csv-str]
  (str/split csv-str #","))

(comment 
  (csv->col "Right Front Head, Left Front Head, Left Eye, Right Eye")
  (csv->col nil))

(defn get-column-parser [col-key parse-fn]
  (fn [row]
    (let [str-in (col-key row)]
      (if (nil? str-in)
        row ; Do nothing if nil, there's no reasonable way to parse
        (assoc row col-key (parse-fn str-in))))))

(def parsed-mb-data
  (->> mb-data
       (map (get-column-parser :date str->iso-datetime))
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

(clerk/table parsed-mb-data)

(clerk/vl {
           :width 675
           :height 400
           :data {:values parsed-mb-data}
           :mark "line"
           :encoding {:x {:field :date :type :temporal}
                      :y {:field :pain-level :type :quantitative}}
           })


