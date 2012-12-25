Cleaners
========

Cleaners are the workhorses of Red Tape.  They massage your form's data into the
shape you want, and detect bad data so you can bail out if necessary.

[TOC]

Cleaners are Functions
----------------------

Cleaners are plain old Clojure functions -- there's nothing special about them.
They take one argument (the data to clean) and return a result.

Here are a few examples:

    :::clojure
    ; A cleaner to turn the raw string into a Long.
    (defn to-long [v]
      (Long. v))

    ; A cleaner to take a Long user ID and look up the
    ; user in a database.
    (def users {1 "Steve"})

    (defn to-user [id]
      (let [user (get users id)]
        (if user
          user
          (throw+ "Invalid user ID!"))))

    ; Use both of these cleaners to first turn the input
    ; string into a long, then into a user.
    (defform user-form {}
      :user [to-long to-user])

    ; Now we'll call the form with some data.
    (user-form {"user" "1"})
    ; =>
    {:valid true
     :results {:user "Steve"}
     ...}

Validation Errors
-----------------

Cleaners can report a validation error by throwing an exception, or by using
Slingshot's `throw+` to throw *anything*.

If a cleaner throws something, the result map's `:valid` entry will be `false`
and the `:errors` entry will contain whatever was thrown.  Continuing the
example above:

    :::clojure
    (user-form {"user" "400"})
    ; =>
    {:valid false
     :errors {:user "Invalid user ID!"}
     ...}

Form-Level Cleaners
-------------------

Built-In Cleaners
-----------------

Red Tape contains a number of common cleaners in `red-tape.cleaners`.  See the
[Reference](../reference) section for the full list.

