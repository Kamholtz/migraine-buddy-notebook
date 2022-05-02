(ns core
  (:require [nextjournal.clerk :as clerk]
            [migraine :as m]
            [health-event :as he]))

^{:nextjournal.clerk/viewer :hide-result}
(use 'debux.core)

; Command to run in powershell to start repl
; clj -m nrepl.cmdline `
; >>     --middleware "[cider.nrepl/cider-middleware]" `
; >>     --interactive

;; Import data and parse for use with Vega Lite
(def mb-export-path "./datasets/MigraineBuddy_20211216_20220430_1651315369524_-555206987.csv")
(defonce migraine-import (m/get-migraine-data mb-export-path))
(defonce health-event-import (he/get-health-event-data mb-export-path))
(def parse-migraine-data (m/get-parsed-migraine-data migraine-import))
(def parsed-health-event-data (he/get-parsed-health-event-data health-event-import))


(clerk/html [:h1 "Migraine intensity/day of year"])

(clerk/vl 
  {:width 675
   :height 400
   :data {:values parse-migraine-data}
   :mark "bar"
   :encoding {:x {:field :date-formatted :type :temporal :title "Date"}
              :y {:field :pain-level :type :quantitative :title "Pain Level (0 - 10)"}}})


(clerk/html [:h1 "Migraine count/week of year"])

(clerk/table parsed-health-event-data)

(clerk/vl 
  {:width 675
   :height 400
   :data {:values parse-migraine-data}
   ; https://vega.github.io/vega-lite/docs/layer.html
   ; https://vega.github.io/vega-lite/examples/layer_falkensee.html
   :layer [{:mark "rect"
            :data {:values (take 2 parsed-health-event-data)}
            :encoding {:x {:field :start-date-formatted
                           ; https://vega.github.io/vega-lite/docs/datetime.html
                           ; https://vega.github.io/vega-lite/docs/timeunit.html
                           :timeUnit :yearmonthdate}
                       :x2 {:field :end-date-formatted
                            :timeUnit :yearmonthdate}
                       :color {:field :description 
                               :type :nominal
                               :legend {:orient :bottom}}}}

           {:mark :bar 
            :encoding {:x {:title  "Week of year"
                           :field :date-formatted 
                           :timeUnit "yearweek"}
                       :y {:title "Number of migraines/week" 
                           ; https://vega.github.io/vega-lite/docs/aggregate.html#transform
                           :aggregate "count" 
                           :scale {:domain [0 10]}}
                       :color {:value "red"}}}]})
