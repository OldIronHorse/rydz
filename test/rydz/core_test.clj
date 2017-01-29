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

(deftest test-job-quote
  (testing "quote, valid"
    (let
      [rate-book {"NW1" {"TW11" 23.5}
                  "TW11" {"NW1" 22.5}}
       from {:postcode "TW11 9PA"}
       to {:postcode "NW1 3ER"}]
      (is
        (=
          22.50
          (job-quote rate-book from to))))))

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

(deftest test-distance-url
  (testing "valid from, to and key"
    (is
      (=
        "https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial&origins=###from###&destinations=###to###&key=###key###"
        (distance-url "###key###" "###from###" "###to###")))))

