(ns red-tape.cleaners
  (:require [slingshot.slingshot :refer [try+ throw+]]))


; Ensure Macros
(defmacro ensure-is
  "Ensure the given value returns true for the given predicate.

  If the value satisfies the predicate, it is returned unchanged.

  Otherwise, the given message is thrown with Slingshot's throw+.

  "
  [value pred msg]
  `(let [v# ~value]
     (if-not (~pred v#)
       (throw+ ~msg)
       v#)))

(defmacro ensure-not
  "Ensure the given value returns false for the given predicate.

  If (predicate value) returns false, the value is returned unchanged.

  Otherwise, the given message is thrown with Slingshot's throw+.

  "
  [value pred msg]
  `(let [v# ~value]
     (if (~pred v#)
       (throw+ ~msg)
       v#)))


; Strings
(defn non-blank
  "Ensure that the string is not empty.

  Whitespace is treated like any other character -- use clojure.string/trim to
  remove it if you want.

  "
  [s]
  (ensure-not s #(= "" %1)
              "This field is required."))

(defn min-length
  "Ensure that the string is at least N characters long."
  [n s]
  (ensure-not s #(< (count %) n)
              (str "This field must be at least " n " characters.")))

(defn max-length
  "Ensure that the string is at most N characters long."
  [n s]
  (ensure-not s #(> (count %) n)
              (str "This field must be at most " n " characters.")))

(defn length
  "Ensure that the string is at least min and at most max characters long."
  [min max s]
  (->> s
    (min-length min)
    (max-length max)))

(defn empty-to-nil
  "Converts empty strings to nil, leaving other strings alone.

  Whitespace is treated like any other character -- use clojure.string/trim to
  remove it if you want.

  "
  [s]
  (if (= s "")
    nil
    s))


; Numerics
(defn to-long
  "Convert a string to a Long."
  [s]
  (try+
    (Long. s)
    (catch Object _ "Please enter a whole number.")))

(defn to-double
  "Convert a string to a Double."
  [s]
  (try+
    (Double. s)
    (catch Object _ "Please enter a number.")))

(defn positive [n]
  (ensure-is n pos?
             "Please enter a positive number."))

(defn negative [n]
  (ensure-is n neg?
             "Please enter a negative number."))


; Other
(defn choices [cs v]
  (ensure-is v #(contains? cs %)
             "Invalid choice."))

