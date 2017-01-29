(ns rydz.core
  (:require [clojure.string :as str]
            [cheshire.core :refer :all])
  (:gen-class))

(defn load-config
  []
  (read-string (slurp "config.clj")))

(defn rate-book-add
  [book from to price]
  (assoc-in book [from to] price))

(defn address
  "Create an address map from a multiline address [country postcode city street house]"
  [& lines]
  (zipmap [:country :postcode :city :street :house] lines))

(defn postcode-area
  "Extract the pricing-significant portion of a postcode"
  [postcode]
  (first (str/split postcode #" ")))

(defn job-quote
  "Return a point to point price based on the supplied rate book"
  [rate-book from to]
  (let
    [from' (postcode-area (from :postcode))
     to' (postcode-area (to :postcode))]
    (get-in rate-book [from' to'])))

(defn distance-url
  [key from to]
  (format
    "https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial&origins=%s&destinations=%s&key=%s"
    from to key))

(defn distance-from-json
  [json]
  (let
    [rows ((parse-string json) "rows")
     elements ((first rows) "elements")
     distance (get-in (first elements) ["distance" "value"])
     duration (get-in (first elements) ["duration" "value"])]
    {:distance {:metres distance}
     :time {:seconds duration}}))

(comment 
The rate-book should be a function that takes 2 addresses and returns a price.

1. create/find rate book in stages
  a. user
  b. service
  c. time?
  c. from/to
  )
