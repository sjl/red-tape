(ns red-tape.core-test
  (:require [clojure.test :refer :all]
            [red-tape.core :refer [defform]]
            [red-tape.cleaners :as cs]))


(defform number-form {}
  :n [cs/to-long])

(defform numbers-form {}
  :n [cs/to-long]
  :m [cs/to-long])

(defform stripping-number-form {}
  :n [clojure.string/trim cs/to-long])

(defform state-form {:arguments [states]}
  :state [clojure.string/trim (partial cs/choices states)])


(deftest test-number-form
  (are [n result]
       (= (:results (number-form {:n n}))
          {:n result})
       "10" 10
       "1" 1
       "-42" -42))

(deftest test-numbers-form
  (are [n m rn rm]
       (= (:results (numbers-form {:m m :n n}))
          {:n rn :m rm})
       "10" "0" 10 0
       "1" "2" 1 2))

(deftest test-stripping-number-form
  (are [n result]
       (= (:results (stripping-number-form {:n n}))
          {:n result})
       "     10"   10
       "1   "       1
       "   -42  " -42))
(deftest test-state-form
  (are [available-states data result]
       (= (:results (state-form available-states {:state data}))
          {:state result})
       #{"pa" "ny"} " ny"   "ny"
       #{"ny"}      " ny  " "ny"
       #{"ny"}      "ny"    "ny")
  (are [available-states data errors]
       (= (:errors (state-form available-states {:state data}))
          errors)
       #{"pa" "ny"} "nj" {:state "Invalid choice."}))

