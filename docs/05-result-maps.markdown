Result Maps
===========

After you pass data through a form (or call a form without data) you'll get
a result map back.

This map contains a number of entries that describe the results of running the
cleaners on that data (or some placeholders when you run a form without data).
Let's look at each of the entries in detail.

Once you're done here, move on to the [initial data](../initial-data/) guide.

[TOC]

:fresh
------

This will be `true` if this is a fresh form (i.e.: it was called without data),
and `false` otherwise.

:valid
------

If the form was called without data, this will be `nil`.

Otherwise, if the form was called with data, and the data passed through all
cleaners without anything being thrown, this will be `true`.

Otherwise it will be `false.`

This should be used to determine whether to trust the data from the form, or
whether to re-render it for the user (and display the errors).

:errors
-------

If the form was called with data and a cleaner threw an exception (or anything
else with Slingshot's `throw+`) then `:errors` will be a map of field keys to
whatever was thrown.  For example:

    :::clojure
    (defn always-fail [x]
      (throw+ "Error!"))

    (defform failing-form {}
      :foo [always-fail]
      :bar [])

    (failing-form {"foo" "aaa"
                   "bar" "bbb"})
    ; =>
    {:errors {:foo "Error!"}
     ...}

Only fields that actually threw errors will have entries in the `:errors` map.

If any form-level cleaners threw an error, `:errors` will contain an entry for
`:red-tape/form`.  This will be a set of form-level errors (for the reasons
described in the previous section).

If no errors were thrown anywhere, `:errors` will be `nil`.

:data
-----

The `:data` entry in the result map will contain the raw data that was passed
into the form, *before* it was run through the cleaners.

This is useful for re-rendering the form when there are errors.

TODO: give a full example here.

If this is the result map of a fresh form (a form that has been called without
form data), every entry in `:data` will be an empty string (or initial data,
which we'll discuss in the next chapter).

:results
--------

The `:results` entry is a map of field keys to the values *after* they've been
passed through all the cleaners.  After you've checked that the form is valid by
looking at `:valid`, you should use the data in `:results` to do whatever you
need to do.

If this is a fresh form, or if any cleaners threw errors, `:results` will be
`nil`.

:arguments
----------

The `:arguments` entry is a mapping of keywords to form arguments, which will be
discussed in a later chapter.

