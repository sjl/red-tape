Reference
=========

The following is a list of all user-facing parts of Red Tape.

If there are any backwards-incompatible changes to anything listed here, they
will be noted in the changelog and the author will feel bad.

Anything not listed here is subject to change at any time with no warning, so
don't touch it.

[TOC]

red-tape.core
-------------

`red-tape.core` contains the `defform` and `form` macros you use to build forms.
Everything else in the namespace is internal.  Don't poke at it.

### defform

    :::clojure
    (red-tape.core/defform form-name options & fields)

Define a form.

The first argument is the name the form will be bound to.

The `options` map can contain some or all of the following entries:

* `:arguments`: a vector of symbols that the form function will take as
  arguments.  See the [form arguments](../form-arguments/) guide for more
  information.
* `:initial`: a map of field names to expressions that will be evaluated to get
  initial data.  See the [initial data](../initial-data/) guide for more
  information.

More options may be added in the future.

Finally the fields are defined as pairs of `:field [... cleaners ...]`.  The
field name `:red-tape/form` is used to define [form-level
cleaners](../cleaners/#form-level-cleaners).

### form

`form` is like `defform` except that you don't specify a name, and it creates an
anonymous form.  In a nutshell: `form` is to `defform` as `fn` is to `defn`.

red-tape.cleaners
-----------------

Red Tape contains a number of built-in cleaners and utility functions in the
`red-tape.cleaners` namespace.

Most built-in cleaners take an optional `error-message` argument that you can
use to provide a custom error message if they fail (instead of the generic one
they each provide).

Also, remember that cleaners are just vanilla Clojure functions, which means
you'll find a lot of useful cleaners in the `clojure.string` namespace.

### Utilities

The `ensure-is` and `ensure-not` macros are useful when writing your own
reusable cleaner functions.

#### ensure-is

    :::clojure
    (red-tape.cleaners/ensure-is value predicate message)

Ensure the given value returns `true` for the given predicate.

If the value satisfies the predicate, it is returned unchanged.

Otherwise, the given message is thrown with Slingshot's `throw+`.

    :::clojure
    (ensure-is 10 pos? "Enter a positive number.")
    ; =>
    10

    (ensure-is -10 pos? "Enter a positive number.")
    ; =>
    ; throws "Enter a positive number."

#### ensure-not

    :::clojure
    (red-tape.cleaners/ensure-not value predicate message)

Ensure the given value returns `false` for the given predicate.

If the value satisfies the predicate, it is returned unchanged.

Otherwise, the given message is thrown with Slingshot's `throw+`.

    :::clojure
    (ensure-not 255 #(< % 10) "Cannot be less than 10.")
    ; =>
    255

    (ensure-not 1 #(< % 10) "Cannot be less than 10.")
    ; =>
    ; throws "Cannot be less than 10."

### String Cleaners

There are a number of built-in cleaners designed to be used with String data.

#### non-blank

    :::clojure
    (red-tape.cleaners/non-blank s)
    (red-tape.cleaners/non-blank s error-message)

Ensure the given string is not empty.

Whitespace is treated like any other character -- use `clojure.string/trim` to
remove it first if you want to.

#### min-length

    :::clojure
    (red-tape.cleaners/min-length n s)
    (red-tape.cleaners/min-length n s error-message)

Ensure that the given string is at least `n` characters long.

#### max-length

    :::clojure
    (red-tape.cleaners/max-length n s)
    (red-tape.cleaners/max-length n s error-message)

Ensure that the given string is at most `n` characters long.

#### length

    :::clojure
    (red-tape.cleaners/length min max s)
    (red-tape.cleaners/length min max s min-error max-error)

Ensure that the given string is at least `min` and at most `max` characters
long.

`length` takes two optional error message arguments, one to throw if the string
is too short and one if it's too long.

#### matches

    :::clojure
    (red-tape.cleaners/matches regex s)
    (red-tape.cleaners/matches regex s error-message)

Ensure that the string (fully) matches the given regular expression.

The regex must match the *entire* string, so `(matches #"foo" "foo bar")` will
throw an error.

### Numeric Cleaners

Red Tape has a few built-in cleaners for working with numbers too.

#### to-long

    :::clojure
    (red-tape.cleaners/to-long s)
    (red-tape.cleaners/to-long s error-message)

Convert a String to a Long.

#### to-double

    :::clojure
    (red-tape.cleaners/to-double s)
    (red-tape.cleaners/to-double s error-message)

Convert a String to a Double.

#### positive

    :::clojure
    (red-tape.cleaners/positive n)
    (red-tape.cleaners/positive n error-message)

Ensure the given number is positive.

This function expects a number, so use `to-long` or `to-double` first.

#### negative

    :::clojure
    (red-tape.cleaners/negative n)
    (red-tape.cleaners/negative n error-message)

Ensure the given number is negative.

This function expects a number, so use `to-long` or `to-double` first.

### Other Cleaners

Finally, there are some other cleaners that don't fit into one of the other
categories.

#### choices

    :::clojure
    (red-tape.cleaners/choices cs value)
    (red-tape.cleaners/choices cs value error-message)

Ensure the value is contained in the given choices.

Useful when you have a list of valid things for the user to choose from:

    :::clojure
    (def us-states #{"NY" "PA" "OR" ...})

    (defform address-form {}
      ...
      :state [#(choices us-states %
                        "Please enter a valid US state.")])

Not limited to strings, so you can parse the input into something else and then
check for those with choices if you want.

Note that this uses `clojure.core/contains?` which is stupid about vectors, so
be careful!  Use a set if possible.

#### to-boolean

    :::clojure
    (red-tape.cleaners/to-boolean s)
    (red-tape.cleaners/to-boolean s error-message)

Convert a string to a boolean.
  
The following strings are considered true (case-insensitive):

* `"1"`
* `"true"`
* `"t"`

The following strings are considered false (case-insensitive):

* `"0"`
* `"false"`
* `"f"`

Anything else is an error.

