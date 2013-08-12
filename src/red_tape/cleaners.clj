(ns red-tape.cleaners
  (:require [slingshot.slingshot :refer [try+ throw+]]))


; Ensure Macros
(defmacro ensure-is
  "Ensure the given value returns true for the given predicate.

  If the value satisfies the predicate, it is returned unchanged.

  Otherwise, the given message is thrown with Slingshot's throw+.

  "
  [value predicate message]
  `(let [v# ~value]
     (if-not (~predicate v#)
       (throw+ ~message)
       v#)))

(defmacro ensure-not
  "Ensure the given value returns false for the given predicate.

  If (predicate value) returns false, the value is returned unchanged.

  Otherwise, the given message is thrown with Slingshot's throw+.

  "
  [value pred error-message]
  `(let [v# ~value]
     (if (~pred v#)
       (throw+ ~error-message)
       v#)))


; Strings
(defn non-blank
  "Ensure the string is not empty.

  Whitespace is treated like any other character -- use clojure.string/trim to
  remove it if you want.

  "
  ([s]
   (non-blank s "This field is required."))
  ([s error-message]
   (ensure-not s #(= "" %) error-message)))

(defn min-length
  "Ensure that the string is at least N characters long."
  ([n s]
   (min-length n s
               (str "This field must be at least " n " characters.")))
  ([n s error-message]
   (ensure-not s #(< (count %) n) error-message)))

(defn max-length
  "Ensure that the string is at most N characters long."
  ([n s]
   (max-length n s
               (str "This field must be at most " n " characters.")))
  ([n s error-message]
   (ensure-not s #(> (count %) n) error-message)))

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
  ([regex s error-message]
   (ensure-is s #(re-matches regex %) error-message)))


; Numerics
(defn to-long
  "Convert a string to a Long."
  ([s]
   (to-long s "Please enter a whole number."))
  ([s error-message]
   (try+
     (Long. s)
     (catch Object _ (throw+ error-message)))))

(defn to-double
  "Convert a string to a Double."
  ([s]
   (to-double s "Please enter a number."))
  ([s error-message]
   (try+
     (Double. s)
     (catch Object _ (throw+ error-message)))))

(defn positive
  "Ensure the given number is positive.

  This function expects a number, so use to-long or to-double first.

  "
  ([n]
   (positive n "Please enter a positive number."))
  ([n error-message]
   (ensure-is n pos? error-message)))

(defn negative
  "Ensure the given number is negative.

  This function expects a number, so use to-long or to-double first.

  "
  ([n]
   (negative n "Please enter a negative number."))
  ([n error-message]
   (ensure-is n neg? error-message)))


; Other
(defn choices
  "Ensure the value is contained in the given choices.

  Useful when you have a list of valid things for the user to choose from:

  (def us-states #{\"NY\" \"PA\" \"OR\" ...})

    (defform address-form {}
      ...
      :state [#(choices us-states % \"Please enter a valid US state.\")])

  Not limited to strings, so you can parse the input into something else and
  then check for those with choices if you want.

  Note that this uses clojure.core/contains? which is stupid about vectors, so
  be careful!  Use a set if possible.

  "
  ([cs value]
   (choices cs value "Invalid choice."))
  ([cs value error-message]
   (ensure-is value #(contains? cs %) error-message)))

(defn to-boolean
  "Convert a string to a boolean.

  The following strings are considered true (case-insensitive):

    1
    true
    t

  The following strings are considered false (case-insensitive):

    0
    false
    f

  Anything else is an error.

  "
  ([s]
   (to-boolean s "Please enter a boolean."))
  ([s error-message]
   (case (clojure.string/lower-case s)
     ("1" "true" "t") true
     ("0" "false" "f") false
     (throw+ error-message))))


