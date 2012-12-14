(ns red-tape.cleaners-test
  (:require [clojure.test :refer :all]
            [red-tape.cleaners :as cl]
            [slingshot.slingshot :refer [try+]]))


(defmacro throws [& body]
  `(~'is (not (nil? (try+ (do ~@body nil)
                        (catch Object e# e#))))))

(defmacro are-bad [msg test-fn & values]
  `(testing ~msg
     ~@(map (fn [v]
         `(throws (~test-fn ~v))) values)))


(deftest test-non-blank
  (testing
    "non-blank passes through non-blank values"
    (are [s] (= s (cl/non-blank s))
         "a"
         "foo"
         " "))

  (are-bad
    "non-blank throws on blank values"
    cl/non-blank
    ""))

(deftest test-to-long
  (testing
    "to-long converts strings to longs"
    (are [s result] (= (cl/to-long s) result)
         "0" 0
         "10" 10
         "-20" -20))

  (are-bad
    "to-long throws on bad data"
    cl/to-long
    ""
    "dogs"
    "a10"
    "   10"
    "10a"))

(deftest test-positive
  (testing
    "positive passes through positive numbers"
    (are [n] (= n (cl/positive n))
         10
         20
         10.5
         0.2
         100000))

  (are-bad
    "positive throws on non-positive numbers (and garbage)"
    cl/positive
    ""
    "10"
    "dogs"
    0
    -10
    -1.5))

(deftest test-negative
  (testing
    "negative passes through negative numbers"
    (are [n] (= n (cl/negative n))
         -10
         -20
         -10.5
         -0.2
         -100000))

  (are-bad
    "negative throws on non-negative numbers (and garbage)"
    cl/negative
    ""
    "-10"
    "dogs"
    0))
