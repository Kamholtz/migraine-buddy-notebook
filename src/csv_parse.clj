(ns csv-parse
  (:require [clojure.string :as str]
            [java-time :as jt]
            [clojure.set :as cset]
            ))

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

(defn csv->col 
  "Split a string separated by commas"
  [csv-str]
  (str/split csv-str #","))

(comment 
  (csv->col "Right Front Head, Left Front Head, Left Eye, Right Eye") ; ["Right Front Head" " Left Front Head" " Left Eye" " Right Eye"]
  (csv->col nil) ; Exception
  )

(def migraine-dt-format "d/MM/yyyy HH:mm")

(comment 
  ; Learning java-time
  (jt/local-date-time migraine-dt-format "4/04/22 06:55")
  (jt/local-date-time migraine-dt-format "31/01/22 07:42")

  (str (jt/local-date-time migraine-dt-format "29/04/22 07:03")) ; "2022-04-29T07:03"
  (.getMinute (jt/local-date-time migraine-dt-format "29/04/22 07:03"))
  (jt/as-map (jt/local-date-time migraine-dt-format "29/04/22 07:03"))

  (-> (jt/local-date-time "dd/MM/yy HH:mm" "21/12/21 22:05")
      (jt/as :minutes-of-hour)))


(defn str->datetime 
  "Get a datetime object"
  [date-str]
  (jt/local-date-time migraine-dt-format date-str))


(defn str->iso-datetime 
  "Parses to an iso datetime str... I think?"
  [date-str]
  (jt/format (str->datetime date-str) ))

(comment
 (str->datetime "30/05/2022 6:47" )
 (jt/local-date-time "d/MM/yyyy HH:mm" "30/05/2022 6:47")
 (str->iso-datetime "30/05/2022 6:47" )
 )


(defn dt->iso-datetime 
  "Datetime to iso datetime string"
  [dt]
  (jt/format dt))


(comment 
  (str->iso-datetime "29/04/22 07:03") ; "2022-04-29T07:03"
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


(def health-event-d-format "dd/MM/yyyy")


(defn str->date 
  "Get a date object from health event date string"
  [date-str]
  (jt/local-date health-event-d-format date-str))

(comment 
  
  ;; toString creates ISO date apparently
  (-> (str->date "09/04/2022")
      (.toString)) ; "2022-04-09"
  )


(defn str->start-date-str 
 "Parse out the start date from the form" 
  [date-range-str]
  (first (str/split date-range-str #" - ")))

