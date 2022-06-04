(ns migraine
  (:require [meta-csv.core :as csv]
            [csv-parse :as csvp]))


(defn get-migraine-data [csv-path]
  (csv/read-csv csv-path 
                {:header? false 
                 :skip 8
                 :fields [{:field :index, :type :long}
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
                          {:field :notes, :type :string}]}))
  

(defn get-parsed-migraine-data 
  "Return collection of maps with iso dates and csv values as collections"
  [migraine-data]
  (->> migraine-data
       (map (csvp/get-column-parser :date :date-formatted csvp/str->iso-datetime))
       (map (csvp/get-column-parser :date :date-as-map csvp/str->date-as-map))
       (map (csvp/get-column-parser :affected-activities csvp/csv->col))
       (map (csvp/get-column-parser :potential-triggers csvp/csv->col))
       (map (csvp/get-column-parser :symptoms csvp/csv->col))
       (map (csvp/get-column-parser :auras csvp/csv->col))
       (map (csvp/get-column-parser :helpful-medication csvp/csv->col))
       (map (csvp/get-column-parser :somewhat-helpful-medication csvp/csv->col))
       (map (csvp/get-column-parser :unhelpful-medication csvp/csv->col))
       (map (csvp/get-column-parser :unsure-medication csvp/csv->col))
       (map (csvp/get-column-parser :helpful-non-drug-relief-methods csvp/csv->col))
       (map (csvp/get-column-parser :somewhat-helpful-non-drug-relief-methods csvp/csv->col))
       (map (csvp/get-column-parser :unhelpful-non-drug-relief-methods csvp/csv->col))
       (map (csvp/get-column-parser :unsure-non-drug-relief-methods csvp/csv->col))
       (map (csvp/get-column-parser :pain-positions csvp/csv->col))))


(defn get-grouped-by-year-migraine-data
  "Return migraine data grouped by year and week with number of migraines in each week
  i.e. {:year .. :week .. :year-and-week :migraines-in=week}"
  [migraine-data]
  (->> migraine-data
       ; Group by year/week of year
       (group-by #(vector (get-in % [:date-as-map :week-based-year])
                          (get-in % [:date-as-map :week-of-week-based-year])))
       ; Count items in group
       (map (fn [[group-key group]] {:year (first group-key) 
                                     :week (second group-key)
                                     :year-and-week group-key
                                     :migraines-in-week (count group)}))
      
       (sort-by (juxt :year :week))))

#_ (clerk/vl 
  {:width 675
   :height 400
   :data {:values grouped-mb-data}
   :layer [{:mark "bar"
            :encoding {:x {:field :year-and-week :type :nominal :title "Year and Week"}
                       :y {:field :migraines-in-week :type :quantitative :title "# Migraines/Week" :scale {:domain [0 10]}}
                       :color {:value "red"}}}]})
