(ns core
  (:require [nextjournal.clerk :as clerk]
            [dk.ative.docjure.spreadsheet :as ss]
            [meta-csv.core :as csv]))

(defn load-first-sheet
  "Return the first sheet of an Excel spreadsheet as a seq of maps."
  [filename]
  (let [rows (->> (ss/load-workbook filename) ; load the file
                  (ss/sheet-seq)              ; seq of sheets in the file
                  first                       ; take the first (only)
                  ss/row-seq                  ; get the rows from it
                  (drop 6)
                  (mapv ss/cell-seq))         ; each row -> seq of cells
        ;; break off the headers to produce a seq of maps
        headers   (mapv (comp keyword ss/read-cell) (first rows))]
    ;; map over the rows creating new maps with the headers as keys
    (mapv #(zipmap headers (map ss/read-cell %)) (rest rows))))


;; # :started :lasted :pain-level :affected-activities :potential-triggers :symptoms :most-bothersome-symptom :auras :pain-positions :helpful-medication :somewhat-helpful-medication :unhelpful-medication :unsure-medication :helpful-non-drug-relief-methods :somewhat-helpful-non-drug-relief-methods :unhelpful-non-drug-relief-methods :unsure-non-drug-relief-methods :notes
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

(def mb-path "./datasets/MigraineBuddy_20211216_20220430_1651315369524_-555206987.csv")
; (def mb-path "./datasets/MigraineBuddy-2.csv")

(def mb-data
  (csv/read-csv mb-path {:header? true :skip 6 :fields fields}))

(clerk/table mb-data)

