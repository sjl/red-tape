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

Optional Fields
---------------

Sometimes you want to be have a field that is optional, but when it's given you
want to transform it.

You *could* do this by adapting your cleaner functions like this:

    :::clojure
    (defform user-profile {}
      :user-id [...]
      :username [...]
      :bio [#(when-let [bio %] bio)
            #(when-let [bio %]
               (if (< (length bio) 10)
                 (throw+ "If given, must be at least 10 characters.")
                 bio))
            #(when-let [bio %]
               (if (> (length bio) 2000)
                 (throw+ "Must be under 2000 characters.")
                 bio))])

This will certainly work, but it means you have to convert the empty string to
`nil` manually and then do a lot of `when-let`ing in your cleaners to pass the
`nil` through to the end.

You can avoid this by marking the cleaners as `:red-tape/optional`:

    :::clojure
    (defform user-profile {}
      :user-id [...]
      :username [...]
      :bio ^:red-tape/optional [
        #(if (< (length %) 10)
           (throw+ "If given, must be at least 10 characters.")
           %)
        #(if (> (length %) 2000)
           (throw+ "Must be under 2000 characters.")
           %)])

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
      :user-id []
      :old-password []
      :new-password-1 []
      :new-password-2 []
      :red-tape/form new-passwords-match)

    (change-password-form {:user-id "101"
                           :old-password "foo"
                           :new-password-1 "a"
                           :new-password-2 "b"})
    ; =>
    {:valid false
     :errors {:red-tape/form #{"New passwords do not match!"}}}

There's a lot to see here.  First, we defined a function that takes a map of
form data (after any field cleaners have been run).

If the new password fields match, the function returns the map of data.  In this
case it doesn't modify it at all, but it could if we wanted to.

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
                       [my-cleaner-part-1 my-cleaner-part-2]})

Each entry in the set will be evaluated according to the rules above, and its
output fed into the other entries.

This happens in an unspecified order, so you should only use a set to define
form-level cleaners that explicitly do *not* depend on each other.  If one
cleaner depends on another one adjusting the data first, you need to use
a vector to make sure they run in the correct order.

The last thing to notice here is that the form-level errors are returned as
a *set* in the result map.  This is because Red Tape will return *all* the
errors for each entry in the set of cleaners at once.  For example:

    :::clojure
    (defn new-passwords-match [form-data]
      (if (not= (:new-password-1 form-data)
                (:new-password-2 form-data))
        (throw+ "New passwords do not match!")
        form-data))

    (defn old-password-is-correct [form-data]
      (if (check-password (:user-id form) (:old-password form)))
        form-data
        (throw+ "Current password is not correct!"))

    (defform change-password-form {}
      :user-id []
      :old-password []
      :new-password-1 []
      :new-password-2 []
      :red-tape/form #{old-password-is-correct new-passwords-match})

    (change-password-form {:user-id "101"
                           :old-password "wrong"
                           :new-password-1 "a"
                           :new-password-2 "b"})
    ; =>
    {:valid false
     :errors {:red-tape/form ["Current password is not correct!"
                              "New passwords do not match!"]}}

Since the form-level cleaners were both specified in a set, Red Tape knows that
one doesn't depend on the other.  So even though one of them failed, Red Tape
will still run the others and return *all* the errors so you can show them all
to the user at once.  Otherwise the user would have to tediously fix one error
at a time and submit to see if there were any other problems.

Built-In Cleaners
-----------------

Red Tape contains a number of common cleaners in `red-tape.cleaners`.  There are
also some handy macros for making your own cleaners.

`ensure-is` is a macro that takes a value, a predicate, and an error message.
If the value satisfies the predicate, that value is passed straight through.
Otherwise the error is thrown:

    :::clojure
    (defform user-profile {}
      :user-id [...
                #(ensure-is % pos? "Invalid ID.")
                ...]
      :username [...]
      :bio ^:red-tape/optional [...])

`ensure-not` passes the value through if it does *not* satisfy the predicate,
and throws the error if it *does*.

    :::clojure
    (defform user-profile {}
      :user-id [...]
      :username [...
                 #(ensure-not % #{"admin" "administrator"}
                              "That username is reserved, sorry.")
                 ...]
      :bio ^:red-tape/optional [...])

`red-tape.cleaners` also contains some pre-made cleaners that you'll probably
find useful:

    :::clojure
    (ns ...
      (:require [red-tape.cleaners :as cleaners]))

    (defform user-profile {}
      :user-id [cleaners/to-long
                cleaners/positive
                ...]
      :username [cleaners/non-blank
                 #(cleaners/length 3 20 %)
                 ...]
      :bio ^:red-tape/optional [#(cleaners/max-length 2000 %)]
      :state [cleaners/non-blank
              clojure.string/upper-case
              #(cleaners/choices #{"NY" "PA" "OR" ...})])

Most of the built-in cleaners take an extra argument that lets you provide
a custom error message when they fail:

    :::clojure
    (defform signup-form {}
      :username [#(cleaners/matches #"[a-zA-Z0-9]+" %)])

    (signup-form {:username "cats and dogs!"})
    ; =>
    {...
     :errors {:username "Invalid format."}
     ...}

    (defform better-signup-form {}
      :username [#(cleaners/matches #"[a-zA-Z0-9]+" %
                   "Username may contain only letters and numbers.")])

    (signup-form {:username "cats and dogs!"})
    ; =>
    {...
     :errors {:username "Username may contain only letters and numbers."}
     ...}

See the [Reference](../reference) section for the full list of built-in
cleaners.

Results
-------

Once all cleaners have been run on the data, the results (or errors) will be
returned as a result map.  Read the [result maps](../result-maps/) guide for
more information.

