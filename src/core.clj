(ns core
  (:require [nextjournal.clerk :as clerk]
            [dk.ative.docjure.spreadsheet :as ss]
            [meta-csv.core :as csv]
            [java-time :as jt]
            [clojure.string :as str]
            [clojure.set :as cset]
            [clojure.java.io :as jio]
            
            [migraine :as m]
            [health-event :as he]))

(use 'debux.core)

(comment 
  ; Learning java-time
  (jt/local-date-time "d/MM/yy HH:mm" "4/04/22 06:55")
  (jt/local-date-time "d/MM/yy HH:mm" "31/01/22 07:42")

  (str (jt/local-date-time "d/MM/yy HH:mm" "29/04/22 07:03")) ; "2022-04-29T07:03"
  (.getMinute (jt/local-date-time "d/MM/yy HH:mm" "29/04/22 07:03"))
  (jt/as-map (jt/local-date-time "d/MM/yy HH:mm" "29/04/22 07:03"))

  (-> (jt/local-date-time "dd/MM/yy HH:mm" "21/12/21 22:05")
      (jt/as :minutes-of-hour))) 

(def mb-path "./datasets/MigraineBuddy_20211216_20220430_1651315369524_-555206987.csv")
(defonce mb-data (m/get-migraine-data mb-path))
(defonce mb-health-event-data (he/get-mb-health-event-data mb-path))


(defn str->datetime 
  "Get a datetime object"
  [date-str]
  (jt/local-date-time "d/MM/yy HH:mm" date-str))

(defn str->iso-datetime 
  "Parses to an iso datetime str... I think?"
  [date-str]
  (jt/format (str->datetime date-str) ))

(defn dt->iso-datetime 
  "Datetime to iso datetime string"
  [dt]
  (jt/format dt))

(comment 
  (str->iso-datetime "29/04/22 07:03")
  )

(defn str->date-as-map [date-str] 
  (jt/as-map (str->datetime date-str)))

(defn dt->date-as-map [dt] 
  (jt/as-map dt))

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

(defn csv->col 
  "Split a string separated by commas"
  [csv-str]
  (str/split csv-str #","))

(comment 
  (csv->col "Right Front Head, Left Front Head, Left Eye, Right Eye")
  (csv->col nil))

(defn get-column-parser 
  "Returns a fn taking a map that calls `parse-fn` on the data assoc'd with `col-from` key and returns the map with the result assoc'd with `col-to` key"
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
       (map (get-column-parser :pain-positions csv->col))))

(defn str->date 
  "Get a date object from health event date string"
  [date-str]
  (jt/local-date "dd/MM/yyyy" date-str))


(defn str->start-date-str 
 "Parse out the start date from the form" 
  [date-range-str]
  (first (str/split date-range-str #" - ")))


(defn get-with-end-formated-end-date
  "Return map `m` with :end-date-formatted based on description text"
  [{:keys [:date :description] :as m}]
  (let [start-date (-> date 
                       str->start-date-str
                       str->date)
        is-emgality (str/includes? (str/lower-case description) "emgality")
        days-to-add (if is-emgality 28 1)
        end-date (jt/plus start-date (jt/days days-to-add))]

    (assoc m :end-date-formatted (dt->iso-datetime end-date))))

(def parsed-mb-health-event-data
  (->> mb-health-event-data
       (map (get-column-parser :date :start-date-formatted
                               #(-> % 
                                    (str->start-date-str)
                                    (str->date)
                                    (dt->iso-datetime))))
       (map get-with-end-formated-end-date)))

(comment 
  
  (-> (str->date "09/04/2022")
      (.toString))
  )


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

#_ (clerk/vl 
  {:width 675
   :height 400
   :data {:values grouped-mb-data}
   :layer [{:mark "bar"
            :encoding {:x {:field :year-and-week :type :nominal :title "Year and Week"}
                       :y {:field :migraines-in-week :type :quantitative :title "# Migraines/Week" :scale {:domain [0 10]}}
                       :color {:value "red"}}}]})

(clerk/table parsed-mb-health-event-data)

(clerk/vl 
  {:width 675
   :height 400
   :data {:values parsed-mb-data}
   ; https://vega.github.io/vega-lite/docs/layer.html
   ; https://vega.github.io/vega-lite/examples/layer_falkensee.html
   :layer [{:mark "rect"
            :data {:values (take 2 parsed-mb-health-event-data)}
            :encoding {:x {:field :start-date-formatted
                           ; https://vega.github.io/vega-lite/docs/datetime.html
                           ; https://vega.github.io/vega-lite/docs/timeunit.html
                           :timeUnit :yearmonthdate}
                       :x2 {:field :end-date-formatted
                            :timeUnit :yearmonthdate}
                       :color {:field :description 
                               :type :nominal
                               :legend {:orient :bottom}}}}

           {:mark "bar"
            :encoding {:x {:title  "Week of year"
                           :field :date-formatted 
                           :timeUnit "yearweek"}
                       :y {:title "Number of migraines/week" 
                           ; https://vega.github.io/vega-lite/docs/aggregate.html#transform
                           :aggregate "count" 
                           :scale {:domain [0 10]}}
                       :color {:value "red"}}}]})
