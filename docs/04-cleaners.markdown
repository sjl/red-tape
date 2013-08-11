Cleaners
========

Cleaners are the workhorses of Red Tape.  They massage your form's data into the
shape you want, and detect bad data so you can bail out if necessary.

[TOC]

Cleaners are Functions
----------------------

Cleaners are plain old Clojure functions -- there's nothing special about them.
They take one argument (the data to clean) and return a result.

Let's look at a few examples.  First we have a cleaner function that takes
a value and turns it into a Long:

    :::clojure
    ; A cleaner to turn the raw string into a Long.
    (defn to-long [v]
      (Long. v))

The next cleaner function takes a user ID, looks up the user in a "database",
and returns the user.  We'll talk about the `throw+` in the next section.

    :::clojure
    ; A cleaner to take a Long user ID and look up the
    ; user in a database.
    (def users {1 "Steve"})

    (defn to-user [id]
      (let [user (get users id)]
        (if user
          user
          (throw+ "Invalid user ID!"))))

Now we can use these two cleaners in a simple form:

    :::clojure
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

What happened here?

First, the string `"400"` was given to the first cleaner, which turned it into
the long `400`.

Then that long was given to the second cleaner, which tried to look it up in the
database.  Since it wasn't found, the cleaner used `throw+` to throw a string as
an error, so the form was marked as invalid.

Form-Level Cleaners
-------------------

Sometimes you need to clean or validate based on more than one field in your
form.  For that you need to use form-level cleaners.

Form-level cleaners are similar to field cleaners: they're vanilla Clojure
functions that take and return a single value.  That value is a map of all the
fields, *after* the field-level cleaners have been run.

Note that if any individual fields had errors, the form-level cleaners will
*not* be run.  It doesn't make sense to run them on garbage input.

They can throw errors just like field-level cleaners too.

Let's look at how to use form-level cleaners with a simple example:

    :::clojure
    (defn new-passwords-match [form-data]
      (if (not= (:new-password-1 form-data)
                (:new-password-2 form-data))
        (throw+ "New passwords do not match!")
        form-data))

    (defform change-password-form {}
      :old-password []
      :new-password-1 []
      :new-password-2 []
      :red-tape/form new-passwords-match)

    (change-password-form {:old-password "foo"
                           :new-password-1 "a"
                           :new-password-2 "b"})
    ; =>
    {:valid false
     :errors {:red-tape/form "New passwords do not match!"}}

There's a lot to see here.  First, we defined a function that takes a map of
form data (after any field cleaners have been run).

If the new password fields match, the function returns the map of data.  In this
case it doesn't modify it at all, but we could if we wanted to.

If the new passwords don't match, an error is thrown with Slingshot's `throw+`.

Next we define the form.  The form-level cleaners are specified by attaching
them to the special `:red-tape/form` "field".

Notice how the form-level cleaner in the example is given on its own, not as
a vector.  There are actually three ways to specify form-level cleaners,
depending on how they need to interact.

The first way is to give a single function like we did in the example:

    :::clojure
    (defform foo {}
      ...
      :red-tape/form my-cleaner)

If you only have one form-level cleaner this is the simplest way to go.

The second option is to give a vector of functions, just like field cleaners:

    :::clojure
    (defform foo {}
      ...
      :red-tape/form [my-cleaner-1 my-cleaner-2])

These will be run in sequence, with the output of each feeding into the next.
This allows you to split up your form-level cleaners just like your field-level
ones.

Finally, you can give a set containing zero or more entries of either of the
first two types: 

    :::clojure
    (defform foo {}
      ...
      :red-tape/form #{my-standalone-cleaner
                       [my-cleaner-part-1
                        my-cleaner-part-2]})

Each entry in the set will be evaluated according to the rules above, and its
output fed into the other entries.

This happens in an unspecified order, so you should only use a set to define
form-level cleaners that explicitly do *not* depend on each other.  If one
cleaner depends on another one adjusting the data first, you need to use
a vector to make sure they run in the correct order.

Built-In Cleaners
-----------------

Red Tape contains a number of common cleaners in `red-tape.cleaners`.  See the
[Reference](../reference) section for the full list.

Results
-------

Once all cleaners have been run on the data, the results (or errors) will be
returned as a result map.  Read the [result maps](../result-maps/) guide for
more information.

