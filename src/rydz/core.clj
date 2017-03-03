(ns rydz.core
  (:require [clojure.string :as str]
            [clj-http.client :as client]
            [cheshire.core :refer :all])
  (:gen-class))

(defn load-config
  []
  (read-string (slurp "config.clj")))

(defn ratebook-load
  [filename]
  (read-string (slurp filename)))

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
  ([k1 k2 m]
   (let [v (m k1)]
     (assoc (dissoc m k1) k2 v)))
  ([ks m]
   (reduce
    (fn [a [k1 k2]] 
      (if (and (map? a) (contains? a k1))
        (if (map? (a k1))
          (assoc (dissoc a k1) k2 (swap-keys ks (a k1)))
          (if (sequential? (a k1))
            (assoc (dissoc a k1) k2 (map (fn [m] (swap-keys ks m)) (a k1)))
            (swap-keys k1 k2 a)))
        a))
    m ks)))

(defn handle-google-distance
  [response]
  (let
    [body (parse-string (response :body))
     key-swapper (partial
                  swap-keys 
                  {"destination_addresses" :destination-addresses
                   "origin_addresses" :origin-addresses
                   "elements" :elements
                   "distance" :distance
                   "text" :text
                   "value" :value
                   "duration" :duration
                   "rows" :rows
                   "status" :status})
     body' (key-swapper body)]
    (assoc body' :rows (map (fn [m] (key-swapper m)) (get body' :rows)))))

(def google-api-key (get-in (load-config) [:keys :google-distance]))

(defn geolocate-url
  [address]
  (format
    "https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s"
    (address-to-string address)
    google-api-key))

(defn map-google-address-keys
  [body]
  (swap-keys
    {"results" :results
     "status" :status
     "address_components" :address-components
     "long_name" :long-name
     "short_name" :short-name
     "formatted_address" :formatted-address
     "geometry" :bounds
     "place_id" :place-id
     "types" :types
     "lat" :latitude
     "lng" :longitude
     "location_type" :location-type
     "location" :location
     "viewport" :viewport
     "northeast" :northeast
     "southwest" :southwest
     "bounds" :bounds
     "partial_match" :partial-match}
    body))

(defn extract-address-components
  [results]
  (reduce
    (fn [a x] (assoc a (first (x :types)) (x :long-name)))
    {}
    ((first results) :address-components)))

(defn address-from-geolocate-json
  [json]
  ;; TODO decompose this function
  ;;      Add testcases for each nested step
  (let
    [body (map-google-address-keys json)
     address (swap-keys
               {"street_number" :house
                "route" :street
                "postal_town" :city
                "country" :country
                "postal_code" :postcode
                "geometry" :geometry}
              (extract-address-components (body :results)))
     location (get-in (first (body :results)) [:bounds :location])]
    (assoc
      (select-keys address [:house :street :city :postcode :country :geometry])
      :location (assoc location :type :latlong))))

(defn geolocate
  [address]
  (let
    [url (geolocate-url address)
     response (client/get url)]
    (address-from-geolocate-json (parse-string (response :body)))))

(comment 
The rate-book should be a function that takes 2 addresses and returns a price.

A ratebook is just a function that takes 2 addesses and returns a price

1. create/find rate book in stages
  a. user
  b. service
  c. time?
  c. from/to
  )
