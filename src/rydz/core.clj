(ns rydz.core
  (:require [clojure.string :as str]
            [clj-http.client :as client]
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

(defn distance
  [from to]
  (let
    [url (distance-url (get-in (load-config) [:keys :google-distance]) from to)
     json ((client/get url) :body)]
    (distance-from-json json)))

(defn swap-keys
  ([m k1 k2]
   (let [v (m k1)]
     (assoc (dissoc m k1) k2 v)))
  ([m ks]
   (reduce
    (fn [a [k1 k2]] 
      (if (and (map? a) (contains? a k1))
        (if (map? (a k1))
          (assoc (dissoc a k1) k2 (swap-keys (a k1) ks))
          (if (sequential? (a k1))
            (assoc (dissoc a k1) k2 (map (fn [m] (swap-keys m ks)) (a k1)))
            (swap-keys a k1 k2)))
        a))
    m ks)))

(defn handle-google-distance
  [response]
  (let
    [body (parse-string (response :body))
     key-map {"destination_addresses" :destination-addresses
           "origin_addresses" :origin-addresses
           "elements" :elements
           "distance" :distance
           "text" :text
           "value" :value
           "duration" :duration
           "rows" :rows
           "status" :status}
     body' (swap-keys body key-map)]
    (assoc body' :rows (map (fn [m] (swap-keys m key-map)) (get body' :rows)))))

(comment 
The rate-book should be a function that takes 2 addresses and returns a price.

1. create/find rate book in stages
  a. user
  b. service
  c. time?
  c. from/to
  )
