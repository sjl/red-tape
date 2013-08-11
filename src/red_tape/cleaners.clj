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
  ([s]
   (non-blank s "This field is required."))
  ([s msg]
   (ensure-not s #(= "" %) msg)))

(defn min-length
  "Ensure that the string is at least N characters long."
  ([n s]
   (min-length n s
               (str "This field must be at least " n " characters.")))
  ([n s msg]
   (ensure-not s #(< (count %) n) msg)))

(defn max-length
  "Ensure that the string is at most N characters long."
  ([n s]
   (max-length n s
               (str "This field must be at most " n " characters.")))
  ([n s msg]
   (ensure-not s #(> (count %) n) msg)))

(defn length
  "Ensure that the string is at least min and at most max characters long."
  ([min max s]
   (->> s
     (min-length min)
     (max-length max)))
  ([min max s min-error max-error]
   (->> s
     (min-length min min-error)
     (max-length max max-error))))

(defn matches
  "Ensure that the string (fully) matches the given regular expression."
  ([regex s]
   (matches regex s "Invalid format."))
  ([regex s msg]
   (ensure-is s #(re-matches regex %) msg)))


; Numerics
(defn to-long
  "Convert a string to a Long."
  ([s]
   (to-long s "Please enter a whole number."))
  ([s msg]
   (try+
     (Long. s)
     (catch Object _ (throw+ msg)))))

(defn to-double
  "Convert a string to a Double."
  ([s]
   (to-double s "Please enter a number."))
  ([s msg]
   (try+
     (Double. s)
     (catch Object _ (throw+ msg)))))

(defn positive
  ([n]
   (positive n "Please enter a positive number."))
  ([n msg]
   (ensure-is n pos? msg)))

(defn negative
  ([n]
   (negative n "Please enter a negative number."))
  ([n msg]
   (ensure-is n neg? msg)))

(defn nonzero
  ([n]
   (nonzero n "Cannot be zero."))
  ([n msg]
   (ensure-not n zero? msg)))


; Other
(defn choices
  ([cs v]
   (choices cs v "Invalid choice."))
  ([cs v msg]
   (ensure-is v #(contains? cs %) msg)))

