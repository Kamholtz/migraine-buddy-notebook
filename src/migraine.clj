(ns migraine
  (:require [meta-csv.core :as csv]))

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
  
