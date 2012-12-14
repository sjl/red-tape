(ns red-tape.cleaners
  (:require [slingshot.slingshot :refer [try+ throw+]]))

(defmacro ensure-is [value f msg]
  `(let [v# ~value]
     (if-not (~f v#)
       (throw+ ~msg)
       v#)))

(defmacro ensure-not [value f msg]
  `(let [v# ~value]
     (if (~f v#)
       (throw+ ~msg)
       v#)))


(defn non-blank [s]
  (ensure-not s #(= "" %1)
              "This field is required."))

(defn to-long [s]
  (Long. s))

(defn positive [n]
  (ensure-is n pos?
             "Please enter a positive number."))

(defn negative [n]
  (ensure-is n neg?
             "Please enter a negative number."))

(defn min-length [n s]
  (ensure-not s #(< (count %) n)
              (str "This field must be at least " n " characters.")))

(defn max-length [n s]
  (ensure-not s #(> (count %) n)
              (str "This field must be at most " n " characters.")))
(defn choices [cs v]
  (ensure-is v #(contains? cs %)
             "Invalid choice."))

(defn to-nil [s]
  (if (= s "")
    nil
    s))
