(ns red-tape.core
  (:require [slingshot.slingshot :refer [try+ throw+]]))


(defmacro map-for [& body]
  `(into {} (for ~@body)))


(defn pipe-through
  "Pipe the data through each function.

  Returns a [results error] pair, one of which will always be nil.

  "
  [data fns]
  (try+
    [(reduce #(%2 %1) data fns) nil]
    (catch Object error
      [nil error])))


(defn zip-fields
  "Zip the fields and data together into a unified vector.

  Fields should be a vector of pairs of keys and cleaners:

  [
  [:username [...cleaners...]]
  [:password [...cleaners...]]
  [:bio      [...cleaners...]]
  ]

  Data should be a map of those keys (as strings or keywords) to their data.  If
  no data is given an empty string will be used:

  {
  \"username\" \"sjl\"
  :password \"hunter2\"
  }

  This will result in a vector of triples:

  [
  [:username \"sjl\"     [...cleaners...]]
  [:password \"hunter2\" [...cleaners...]]
  [:bio      \"\"        [...cleaners...]]
  ]

  "
  [fields data]
  (for [[field-name cleaners] fields]
    [field-name (get data (keyword field-name) "") cleaners]))

(defn process-fields
  "Process the zipped fields and return a map of the outcome.

  The map returned will contain two entries:

  {:results ... :errors ...}

  One of these will always be nil.  The other will be a map of field keys to the
  outcome:

  {:results {:username \"cleaned-username\"} :errors nil}
  {:errors {:username \"This field cannot be blank.\"} :results nil}

  "
  [zipped-fields]
  (let [results (for [[k data cleaners] zipped-fields
                      :let [[value error] (pipe-through data cleaners)]]
                  [k value error])
        values (map-for [[k v _] results]
                         [k v])
        errors (map-for [[k _ e] results
                         :when (not (nil? e))]
                        [k e])]
    (if (empty? errors)
      {:results values :errors nil}
      {:results nil :errors errors})))

(defn get-cleaners
  "Return a set of form-cleaner sequences.

  clean can be given as one of the following:

  nil
  single fn
  a collection of fns
  a set of (single fn or collection of fns)

  "
  [clean]
  (letfn [(cleaner-to-cleaner-seq [c]
            (if (coll? c)
              c
              [c]))]
    (cond
      (nil? clean)  #{}
      (set? clean)  (set (map cleaner-to-cleaner-seq clean))
      (coll? clean) #{clean}
      :else         #{[clean]})))

(defn clean-results
  "Run the whole-form cleaners over the results.

  Takes the preliminary set of results (from per-field cleaning) and run the
  whole-form cleaners on them.

  Assumes that there were no errors on the per-field cleaning side of things.

  Returns a [results errors] pair.  One of these will always be nil.

  Results is a map of the cleaned values.

  Errors is a vector of errors from the various form-cleaning sequences.

  "
  [results cleaner-set]
  (loop [results results
         errors []
         [c & cs] (vec cleaner-set)]
    (if-not c
      (if (empty? errors)
        [results nil]
        [nil errors])
      (let [[cleaned-results error] (pipe-through results c)]
        (if error
          (recur results (conj errors error) cs)
          (recur cleaned-results errors cs))))))


(defn process-result
  "Take the preliminary, per-field-cleaned results and process them.

  clean-spec is the cleaners option given in the defform, which can be a single
  function, a sequence of functions, or a set of either of the above.

  "
  [{:keys [results errors]} clean-spec]
  (if errors
    ; If the individual fields had some errors, we don't even bother trying to
    ; clean the form as a whole.  Just bail.
    {:results nil
     :errors errors
     :valid false}
    (let [form-cleaners (get-cleaners clean-spec)
          [results form-errors] (clean-results results form-cleaners)]
      (if form-errors
        {:results nil
         :errors {:form form-errors}
         :valid false}
        {:results results
         :errors nil
         :valid true}))))


(defn initial-data
  "Return the initial data for a fresh form."
  [field-keys initial]
  (let [blank (into {} (map #(vector % "") field-keys))]
    (merge blank initial)))

(defn zip-map [a b]
  (into {} (map vector a b)))

(defn form-guts
  "For internal use only.  You probably want form or defform.  Turn back now.

  Return the guts of a form, suitable for splicing into (fn ..)
  or (defn name ...).

  "
  [{:keys [arguments initial clean] :or {initial {} arguments []}} fields]
  (let [arg-keys (map keyword arguments)

        ; Create the binding map, which is a map of keywords to symbols:
        ;
        ; {:f1 f1 :f2 f2}
        ;
        ; This will end up being the body for (part of) the form function:
        ;
        ; (defn foo-form [f1 f2]
        ;   {... :arguments {:f1 f1 :f2 f2} ...})
        binding-map (zip-map arg-keys arguments)

        ; Transform fields from:
        ;
        ; [:f1 [a] :f2 [b c]]
        ;
        ; into vector pairs like:
        ;
        ; [[:f1 [a]]
        ;  [:f2 [b c]]]
        fields (mapv vec (partition 2 fields))

        ; Get a vector of just the field keys like [:f1 :f2].
        field-keys (mapv first fields)

        ; A fresh form simply returns a map.
        fresh `{:fresh true
                :arguments ~binding-map
                :data (initial-data ~field-keys ~initial)
                :valid nil
                :errors nil
                :results nil}]
    `[([~@arguments]
       ~fresh)
      ([~@arguments data#]
       (-> ~fields
         (zip-fields data#)
         process-fields
         (process-result ~clean)
         (assoc :fresh false
                :data data#
                :arguments ~binding-map)))]))


(defmacro form
  [{:keys [arguments initial clean] :as options} & fields]
  `(fn ~@(form-guts options fields)))

(defmacro defform
  [form-name {:keys [arguments initial clean] :as options} & fields]
  `(defn ~form-name ~@(form-guts options fields)))

