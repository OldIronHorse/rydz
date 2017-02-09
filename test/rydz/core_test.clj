(ns rydz.core-test
  (:require [clojure.test :refer :all]
            [clojure.data :refer :all]
            [rydz.core :refer :all]))

(deftest test-address
  (testing "valid UK address"
    (let
      [home {:country "UK"
             :postcode "TW11 9PA"
             :city "Teddington"
             :street "Atbara Road"
             :house "35"}]
    (is
      (=
        home
        (address "UK" "TW11 9PA" "Teddington" "Atbara Road" "35"))))))

(deftest test-postcode-area
  (testing "valid UK postcode"
    (is
      (= "TW11" (postcode-area "TW11 9PA")))))

(deftest test-build-rate-book
  (testing "add a point to point price to an empty rate book"
    (is
      (=
        {"NW1" {"TW11" 22.5}}
        (rate-book-add {} "NW1" "TW11" 22.5))))
  (testing "add a new route to a book"
    (is
      (=
        {"NW1" {"TW11" 22.5}
         "TW11" {"NW1" 23.5}}
        (rate-book-add
          {"NW1" {"TW11" 22.5}}
          "TW11" "NW1" 23.5)))))

(deftest test-postcode-price
  (testing "postcode ratebook as partially applied pricing function"
    (let
      [ratebook (partial postcode-price {"NW1" {"TW11" 22.5} "TW11" {"NW1" 23.5}})]
      (is (= 23.50 (ratebook {:postcode "TW11 9PA"} {:postcode "NW1 1AB"})))
      (is (= 22.50 (ratebook {:postcode "NW1 1AB"} {:postcode "TW11 9PA"})))
      (is (nil? (ratebook {:postcode "TW11 9PA"} {:postcode "SE1 1AB"}))
      (is (nil? (ratebook {:postcode "SE1 1AB"} {:postcode "EC4 1AB"})))))))

(deftest test-mileage-price
  (testing "mileage ratebook as partially applied pricing function with distance source"
    (let
      [distance-source (fn [from to] 123456)
       banded-rate (fn [distance] (if (< distance 5000) 0.50 (if (< distance 20000) 0.25 0.10)))
       ratebook (partial (partial mileage-price banded-rate) distance-source)]
      (is (= (/ (* 123456 0.1) 1000) (ratebook {:postcode "TW11 9PA"} {:postcode "NW1 1AB"}))))))

(deftest test-load-config
  (testing "check config file is there"
    (is (not
      (nil? (load-config)))))
  (testing "config file contains :keys"
    (is (not
      (nil? (get (load-config) :keys)))))
  (testing "config contains :keys,:google-distance"
    (is (not
      (nil? (get-in (load-config) [:keys :google-distance]))))))

(deftest test-swap-keys
  (testing "flat map, multiple keys"
    (is (=
      {:one 1, :two 2, "three" 3}
      (swap-keys {"one" 1 "two" 2 "three" 3} {"one" :one, "two" :two, "four" :four}))))
  (testing "nestsed maps"
    (is (=
      {:one 1, :four {:two 2}, "three" 3}
      (swap-keys {"one" 1 "four" {"two" 2} "three" 3} {"one" :one, "two" :two, "four" :four}))))
  (testing "nested sequence of maps"
    (is (=
      {:one 1, :four [{:two 2}, {"three" 3}]}
      (swap-keys
        {"one" 1 "four" [{"two" 2}, {"three" 3}]}
        {"one" :one, "two" :two, "four" :four})))))

(deftest test-address-to-string
  (testing "Fully populated address"
    (is (=
      "53, Nile Street, Twickenham, TW31 7YE, UK"
      (address-to-string {:country "UK"
                          :postcode "TW31 7YE"
                          :city "Twickenham"
                          :street "Nile Street"
                          :house "53"}))))
  (testing "No house number"
    (is (=
      "Nile Street, Twickenham, TW31 7YE, UK"
      (address-to-string {:country "UK"
                          :postcode "TW31 7YE"
                          :city "Twickenham"
                          :street "Nile Street"})))))

(deftest test-distance-url
  (testing "valid from, to and key"
    (is (=
      "https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial&origins=###from###&destinations=###to###&key=###key###"
      (distance-url "###key###" "###from###" "###to###")))))

(deftest test-distance-from-json
  (testing "valid, sucessful response from google Distance API"
    (is (=
      {:distance {:metres 90163} :time {:seconds 4534}}
      (distance-from-json "{\n   \"destination_addresses\" : [ \"Forth Rd, Upminster RM14 1PX, UK\" ],\n   \"origin_addresses\" : [ \"Atbara Rd, Teddington TW11 9PA, UK\" ],\n   \"rows\" : [\n      {\n         \"elements\" : [\n            {\n               \"distance\" : {\n                  \"text\" : \"56.0 mi\",\n                  \"value\" : 90163\n               },\n               \"duration\" : {\n                  \"text\" : \"1 hour 16 mins\",\n                  \"value\" : 4534\n               },\n               \"status\" : \"OK\"\n            }\n         ]\n      }\n   ],\n   \"status\" : \"OK\"\n}\n")))))

(deftest test-handle-google-distance
  (testing "valid, successful single from/to response"
    (let
      [r (handle-google-distance {:request-time 786, :repeatable? false, :protocol-version {:name "HTTP", :major 1, :minor 1}, :streaming? true, :chunked? false, :reason-phrase "OK", :headers {"Server" "mafe", "Content-Type" "application/json; charset=UTF-8", "Alt-Svc" "quic=\":443\"; ma=2592000; v=\"35,34\"", "X-Frame-Options" "SAMEORIGIN", "Connection" "close", "Expires" "Mon, 30 Jan 2017 18:02:39 GMT", "Date" "Sun, 29 Jan 2017 18:02:39 GMT", "Vary" "Accept-Language", "X-XSS-Protection" "1; mode=block", "Cache-Control" "public, max-age=86400"}, :orig-content-encoding nil, :status 200, :length -1, :body "{\n   \"destination_addresses\" : [ \"Edinburgh, UK\" ],\n   \"origin_addresses\" : [ \"London, UK\" ],\n   \"rows\" : [\n      {\n         \"elements\" : [\n            {\n               \"distance\" : {\n                  \"text\" : \"414 mi\",\n                  \"value\" : 666433\n               },\n               \"duration\" : {\n                  \"text\" : \"7 hours 11 mins\",\n                  \"value\" : 25874\n               },\n               \"status\" : \"OK\"\n            }\n         ]\n      }\n   ],\n   \"status\" : \"OK\"\n}\n", :trace-redirects ["https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial&origins=London&destinations=Edinburgh&key=AIzaSyAC95_dw0iNYFCQRwlE1ZmfVf1qQO6FOJg"]})]
    (is (=
      {:destination-addresses ["Edinburgh, UK"]
       :origin-addresses ["London, UK"]
       :rows [{:elements [{:distance {:text "414 mi" :value 666433} :duration {:text "7 hours 11 mins" :value 25874} :status "OK"}]}]
       :status "OK"}
      r))
    (is (=
      ["Edinburgh, UK"]
      (r :destination-addresses)))
    (is (=
      ["London, UK"]
      (r :origin-addresses))))))
