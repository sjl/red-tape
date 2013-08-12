(ns red-tape.core-test
  (:require [clojure.test :refer :all]
            [red-tape.core :as rt :refer [defform form]]
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
  :n [#(cs/to-long % "bad")]
  :m [#(cs/to-long % "bad")])

(defform stripping-number-form {}
  :n [clojure.string/trim cs/to-long])

(defform state-form {:arguments [states]}
  :state [clojure.string/trim (partial cs/choices states)])

(defform initial-form {:initial {:x "42"}}
  :x [cs/to-long]
  :y [cs/to-long])

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
  (testing
    "fresh vanilla form structure"
    (is (= (number-form)
           (fresh-form {:n ""}))))

  (testing
    "forms can take string keys"
    (is (= (:results (number-form {"n" "10"}))
           {:n 10})))

  (testing
    "successful forms return results"
    (are [n result]
         (= (number-form {:n n})
            {:valid true
             :fresh false
             :data {:n n}
             :results {:n result}
             :arguments {}
             :errors nil})
         "10" 10
         "1" 1
         "-42" -42
         "0" 0))

  (testing
    "unsuccessful forms return errors"
    (are [n]
         (let [form (number-form {:n n})]
           (and
             (= (:fresh form) false)
             (= (:valid form) false)
             (= (:results form) nil)
             (= (:errors form) {:n "Please enter a whole number."})
             (= (:data form) {:n n})))
         ""
         "1 + 1"
         "1dogs0"
         "zero")))

(deftest test-numbers-form
  (testing
    "fresh form structure"
    (is (= (numbers-form)
           (fresh-form {:n ""
                        :m ""}))))

  (testing "multiple field results"
    (are [n m rn rm]
         (= (:results (numbers-form {:m m :n n}))
            {:n rn :m rm})
         "10" "0" 10 0
         "1" "2" 1 2))

  (testing
    "multiple field errors"
    (are [n m errors]
         (= (numbers-form {:n n :m m})
            {:fresh false
             :valid false
             :results nil
             :errors errors
             :data {:n n :m m}
             :arguments {}})
         "1" "dogs" {:m "bad"}
         "cats" "1" {:n "bad"}
         "cats" "dogs" {:n "bad" :m "bad"})))

(deftest test-stripping-number-form
  (are [n result]
       (= (:results (stripping-number-form {:n n}))
          {:n result})
       "     10"   10
       "1   "       1
       "   -42  " -42))

(deftest test-state-form
  (testing
    "choices loops things up"
    (are [available-states data result]
         (= (:results (state-form available-states {:state data}))
            {:state result})
         #{"pa" "ny"} " ny"   "ny"
         #{"ny"}      " ny  " "ny"
         #{"ny"}      "ny"    "ny"))

  (testing "choices excludes nonmembers, is case sensitive"
    (are [available-states data errors]
         (= (:errors (state-form available-states {:state data}))
            errors)
         #{"pa" "ny"} "nj" {:state "Invalid choice."}
         #{"pa" "ny"} "NY" {:state "Invalid choice."})))

(deftest test-initial-form
  (testing
    "initial data should be passed through to the initial :data map"
    (is (= (initial-form)
           (fresh-form {:x "42"
                        :y ""}))))

  (testing
    "ignore initial data when we have real data, regardless of success"
    (is (= (initial-form {:x "1" :y "2"})
           {:valid true
            :fresh false
            :data {:x "1" :y "2"}
            :results {:x 1 :y 2}
            :arguments {}
            :errors nil}))
    (is (= (initial-form {:x "dogs" :y "2"})
           {:valid false
            :fresh false
            :data {:x "dogs" :y "2"}
            :results nil
            :arguments {}
            :errors {:x "Please enter a whole number."}}))))

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


; "Simple".
(defmacro macro-asserts [m]
  (try
    (macroexpand-1 m)
    `(let [~'assertion-thrown false]
       (is ~'assertion-thrown
           (format "Macro form %s should throw an AssertionError" (quote ~m))))
    (catch AssertionError _
      `(is true))))

(defmacro macro-asserts-not [m]
  (try
    (macroexpand-1 m)
    `(is true)
    (catch AssertionError _
      `(let [~'assertion-not-thrown false]
         (is ~'assertion-not-thrown
             (format "Macro form %s should not throw an AssertionError" (quote ~m)))))))

(deftest test-macro-sanity
  (testing "form arguments must be given as a vector"
    (macro-asserts (form {:arguments (dogs)}))
    (macro-asserts (form {:arguments {dogs 1}}))
    (macro-asserts (form {:arguments {1 dogs}}))
    (macro-asserts (form {:arguments dogs}))
    (macro-asserts-not (form {:arguments []})))

  (testing "form arguments must be symbols"
    (macro-asserts (form {:arguments [:cats]}))
    (macro-asserts (form {:arguments [dogs :cats]}))
    (macro-asserts (form {:arguments [(symbol "cats")]}))
    (macro-asserts-not (form {:arguments [cats]}))
    (macro-asserts-not (form {:arguments [user/cats]})))

  (testing "initial form data must be given as a map"
    (macro-asserts (form {:initial [:cats 1]} :cats []))
    (macro-asserts (form {:initial :cats} :cats []))
    (macro-asserts-not (form {:initial {}} :cats []))
    (macro-asserts-not (form {:initial {:cats ""}} :cats [])))

  (testing "initial form data keys must be keywords"
    (macro-asserts (form {:initial {cats ""}} :cats []))
    (macro-asserts (form {:initial {"cats" ""}} :cats []))
    (macro-asserts (form {:initial {[:cats] ""}} :cats []))
    (macro-asserts-not (form {:initial {:cats ""}} :cats [])))

  (testing "initial form data keys must be valid fields"
    (macro-asserts (form {:initial {:dogs ""}} :cats [])))

  (testing "form fields must be named with keywords"
    (macro-asserts (form {} cats []))
    (macro-asserts (form {} "cats" []))
    (macro-asserts-not (form {} :cats [])))

  (testing "field cleaners must be given as a vector"
    (macro-asserts (form {} :cats #{}))
    (macro-asserts (form {} :cats {}))
    (macro-asserts (form {} :cats ()))
    (macro-asserts (form {} :cats identity))
    (macro-asserts-not (form {} :cats [])))

  (testing "form-level cleaners can be a vector, a function, or a set"
    (macro-asserts (form {} :red-tape/form {}))
    (macro-asserts (form {} :red-tape/form ()))
    (macro-asserts-not (form {} :red-tape/form identity))
    (macro-asserts-not (form {} :red-tape/form []))
    (macro-asserts-not (form {} :red-tape/form [identity]))
    (macro-asserts-not (form {} :red-tape/form #{identity}))
    (macro-asserts-not (form {} :red-tape/form #{identity [identity]}))))

