(ns rydz.core
  (:require [clojure.string :as str])
  (:gen-class))

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

(comment 
The rate-book should be a function that takes 2 addresses and returns a price.

1. create/find rate book in stages
  a. user
  b. service
  c. time?
  c. from/to
  )
