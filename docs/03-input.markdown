Form Input
==========

Red Tape forms take a map of data as input.  Red Tape is designed with web-based
forms in mind, but since it operates on plain old maps you could certainly use
it for other things as well.

[TOC]

Keys
----

The input map should contain keys that match the field definitions of a form.
For example:

    :::clojure
    (defform change-password-form {}
      :old-password [...]
      :new-password-1 [...]
      :new-password-2 [...])

    (change-password-form {:old-password "foo"
                           :new-password-1 "bar"
                           :new-password-2 "bar"})
    ; =>
    {... result map ...}

For convenience, the keys in the input map can be strings and everything will
still work:

    :::clojure
    (change-password-form {"old-password" "foo"
                           "new-password-1" "bar"
                           "new-password-2" "bar"})
    ; =>
    {... result map ...}

If the input map contains both a string and a keyword key for a field, the
results are undefined.  Don't do that.

Values
------

All values in the input map should be strings.

Using other types of data may *appear* to work, but Red Tape's behavior may
change in the future so you really should just stick to string input and use
cleaners to convert them to whatever kind of data you want.

Move on to the [cleaners](../cleaners/) guide to learn more about this.
