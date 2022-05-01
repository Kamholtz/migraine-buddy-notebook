(ns core
  (:require [nextjournal.clerk :as clerk]
            [dk.ative.docjure.spreadsheet :as ss]
            [meta-csv.core :as csv]
            [java-time :as jt]
            [clojure.string :as str]))


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
  (jt/local-date-time "d/MM/yy HH:mm" "29/04/22 07:03")


  (jt/local-date-time "dd/MM/yy HH:mm" "21/12/21 22:00"))

;; TODO: parse dates, 
;; split :pain-positions (comma)
;; split potential-triggers (comma)
;; split :lasted (newline)
;; split symptoms (comma)
;;

(def mb-path "./datasets/MigraineBuddy_20211216_20220430_1651315369524_-555206987.csv")

(def mb-data
  (csv/read-csv mb-path {:header? false :skip 6 :fields fields}))

(defn parse-date-string [date-str]
  (jt/local-date-time "d/MM/yy HH:mm" date-str))

(defn parse-csv [csv-str]
  (str/split csv-str #","))

(comment 
  (parse-csv "Right Front Head, Left Front Head, Left Eye, Right Eye")
  (parse-csv nil)
  )

(defn parse-date [row]
  (let [date-string (:date row)]
    (assoc row :date (parse-date-string date-string))))

(defn parse-col [col-key parse-fn]
  (fn [row]
    (let [str-in (col-key row)]
      (assoc row col-key (parse-fn str-in)))))

(def parsed-mb-data
  (->> mb-data
       (map (parse-col :date parse-date-string))
       (map (parse-col :affected-activities parse-csv))
       (map (parse-col :potential-triggers parse-csv))
       (map (parse-col :symptoms parse-csv))
       (map (parse-col :auras parse-csv))
       ; (map (parse-col :helpful-medication parse-csv))
       (map (parse-col :pain-positions parse-csv))

       ))

(clerk/table parsed-mb-data)


