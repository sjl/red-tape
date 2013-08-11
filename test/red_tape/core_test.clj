(ns red-tape.core-test
  (:require [clojure.test :refer :all]
            [red-tape.core :refer [defform]]
            [red-tape.cleaners :as cs]
            [slingshot.slingshot :refer [throw+]]))


(defn fresh-form
  ([data]
   (fresh-form data {}))
  ([data arguments]
   {:fresh true
    :valid nil
    :data data
    :results nil
    :arguments arguments
    :errors nil}))


(defform number-form {}
  :n [cs/to-long])

(defform numbers-form {}
  :n [cs/to-long]
  :m [cs/to-long])

(defform stripping-number-form {}
  :n [clojure.string/trim cs/to-long])

(defform state-form {:arguments [states]}
  :state [clojure.string/trim (partial cs/choices states)])

(defform initial-form {:initial {:x "42"}}
  :x []
  :y [])

(defform dynamic-initial-form
  {:arguments [x]
   :initial {:a (first x)
             :b (second x)}}
  :a []
  :b [])


(defn a-b-match [data]
  (if (= (:a data) (:b data))
    data
    (throw+ "No match!")))

(defn b-c-match [data]
  (if (= (:b data) (:c data))
    data
    (throw+ "No match 2!")))

(defform form-cleaner-a-b {}
  :a []
  :b []
  :red-tape/form a-b-match)

(defform form-cleaner-a-b-c-vec {}
  :a []
  :b []
  :c []
  :red-tape/form [a-b-match b-c-match])

(defform form-cleaner-a-b-c-set {}
  :a []
  :b []
  :c []
  :red-tape/form #{a-b-match b-c-match})


(deftest test-number-form
  (is (= (number-form)
         (fresh-form {:n ""})))

  (is (= (:results (number-form {"n" "10"})))
      10)

  (are [n result]
       (= (:results (number-form {:n n}))
          {:n result})
       "10" 10
       "1" 1
       "-42" -42))

(deftest test-numbers-form
  (is (= (numbers-form)
         (fresh-form {:n ""
                      :m ""})))

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

(deftest test-initial-form
  ; Initial data should be passed through to the initial :data map.
  (is (= (initial-form)
         (fresh-form {:x "42"
                      :y ""})))

  ; Initial data should be ignored when we have real data.
  (is (= (initial-form {:x "1" :y "2"})
         {:valid true
          :fresh false
          :data {:x "1" :y "2"}
          :results {:x "1" :y "2"}
          :arguments {}
          :errors nil})))

(deftest test-dynamic-initial-form
  (is (= (dynamic-initial-form ["1" "2"])
         (fresh-form {:a "1"
                      :b "2"}
                     {:x ["1" "2"]})))

  (is (= (dynamic-initial-form ["1" "2"]
                               {:a "3" :b "4"})
         {:valid true
          :fresh false
          :data {:a "3" :b "4"}
          :results {:a "3" :b "4"}
          :arguments {:x ["1" "2"]}
          :errors nil})))

(deftest test-form-cleaner-a-b
  (is (= (:results (form-cleaner-a-b {:a "foo" :b "foo"}))
         {:a "foo" :b "foo"}))
  (is (= (:errors (form-cleaner-a-b {:a "foo" :b "bar"}))
         {:red-tape/form #{"No match!"}})))

(deftest test-form-cleaner-a-b-c
  (is (= (:results (form-cleaner-a-b-c-vec {:a "foo" :b "foo" :c "foo"}))
         {:a "foo" :b "foo" :c "foo"}))
  (is (= (:errors (form-cleaner-a-b-c-vec {:a "foo" :b "bar" :c "baz"}))
         {:red-tape/form #{"No match!"}}))
  (is (= (:errors (form-cleaner-a-b-c-vec {:a "foo" :b "foo" :c "baz"}))
         {:red-tape/form #{"No match 2!"}}))
  (is (= (:errors (form-cleaner-a-b-c-set {:a "foo" :b "bar" :c "baz"}))
         {:red-tape/form #{"No match!" "No match 2!"}}))
  (is (= (:errors (form-cleaner-a-b-c-set {:a "foo" :b "bar" :c "bar"}))
         {:red-tape/form #{"No match!"}}))
  (is (= (:errors (form-cleaner-a-b-c-set {:a "foo" :b "foo" :c "baz"}))
         {:red-tape/form #{"No match 2!"}})))
