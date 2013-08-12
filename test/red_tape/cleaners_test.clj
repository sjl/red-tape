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


; Strings
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

(deftest test-min-length
  (testing
    "min-length passes through long strings"
    (are [s min] (= s (cl/min-length min s))
         "" 0
         "foo" 1
         "foo" 3))

  (are-bad
    "min-length throws on short strings"
    #(cl/min-length 5 %)
    ""
    "foo"
    "ffoo"))

(deftest test-max-length
  (testing
    "max-length passes through short strings"
    (are [s max] (= s (cl/max-length max s))
         "" 0
         "foo" 10
         "foo" 3))

  (are-bad
    "max-length throws on long strings"
    #(cl/max-length 5 %)
    "boots and cats"))

(deftest test-length
  (testing
    "length passes through well-sized strings"
    (are [s min max] (= s (cl/length min max s))
         "" 0 5
         "foo" 3 5
         "fooo" 3 5
         "foooo" 3 5))

  (are-bad
    "length throws on out of range strings"
    #(cl/length 3 5 %)
    ""
    "fo"
    "fooooo"))

(deftest test-matches
  (testing
    "matches passes through strings that match the regex"
    (are [s re] (= s (cl/matches re s))
         "foo" #"foo"
         "foo" #"[fF]oo"
         "Foo" #"[fF]oo"
         "Foo and bar" #"[fF]oo .*"))
  (are-bad
    "matches throws on strings that don't match the (entire) regex"
    #(cl/matches #"foo+" %)
    "fo"
    "fooooobar"
    "fffoooo"))


; Numerics
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

(deftest test-to-double
  (testing
    "to-double converts strings to doubles"
    (are [s result] (= (cl/to-double s) result)
         "0" 0.0
         "10" 10.0
         "-20" -20.0
         "1.2" 1.2
         "-0.987" -0.987))

  (are-bad
    "to-double throws on bad data"
    cl/to-double
    ""
    "--10.9"
    "dogs"
    "a10.0"
    "10_0"
    "10,0a"))

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


; Other
(deftest test-choices
  (testing
    "choices passes through anything in the given set"
    (are [v] (= v (cl/choices #{1 2 "cats"} v))
         1
         2
         "cats"))

  (are-bad
    "choices throws on anything that isn't in the set"
    #(cl/choices #{1 2 "cats"} %)
    ""
    "-10"
    "dogs"
    0
    "2"
    2.0
    ))
