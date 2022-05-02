(ns migraine
  (:require [meta-csv.core :as csv]
            [csv-parse :as csvp]))

(defn get-migraine-data [migraine-csv-path]
  (csv/read-csv migraine-csv-path 
                {:header? false 
                 :skip 6 
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
  
(defn get-parsed-migraine-data [mb-data]
  (->> mb-data
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
