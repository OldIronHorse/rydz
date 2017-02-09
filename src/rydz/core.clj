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

(defn postcode-price
  "Return a postcode to postcode price based on the supplied rate book"
  [ratebook from to]
  (get-in ratebook [(postcode-area (from :postcode)) (postcode-area (to :postcode))]))

(defn mileage-price
  "Return a mileage based price using the supplied rate and distance source funtions"
  [rate-source distance-source from to]
  (let
    [distance (distance-source from to)
     rate (rate-source distance)]
    (/ (* distance rate) 1000.0)))

(defn address-to-string
  [address]
  (str/join
    ", "
    (filter
      #(not (nil? %1))
      (map #(address %1) [:house :street :city :postcode :country]))))

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

A ratebook is just a function that takes 2 addesses and returns a price

1. create/find rate book in stages
  a. user
  b. service
  c. time?
  c. from/to
  )
