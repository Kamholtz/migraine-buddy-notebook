(ns core
  (:require [nextjournal.clerk :as clerk]
            [dk.ative.docjure.spreadsheet :as ss]
            [meta-csv.core :as csv]
            [java-time :as jt]
            [clojure.string :as str]
            [clojure.java.io :as jio]
            
            [csv-parse :as csvp]
            [migraine :as m]
            [health-event :as he]))

(use 'debux.core)

(def mb-path "./datasets/MigraineBuddy_20211216_20220430_1651315369524_-555206987.csv")
(defonce mb-data (m/get-migraine-data mb-path))
(defonce mb-health-event-data (he/get-mb-health-event-data mb-path))
(def parsed-mb-data (m/get-parsed-migraine-data mb-data))


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

(def parsed-mb-health-event-data
  (->> mb-health-event-data
       (map (csvp/get-column-parser :date :start-date-formatted
                               #(-> % 
                                    (csvp/str->start-date-str)
                                    (csvp/str->date)
                                    (csvp/dt->iso-datetime))))
       (map get-with-end-formated-end-date)))


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
