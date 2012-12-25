Initial Data
============

Sometimes you want to provide sane defaults for fields in a form.  To do this
you can use the `:initial` entry in the `defform` option map.  For example:

    :::clojure
    (defform sample-form {:initial {:email "user@example.com"}}
      :email [])

When creating a fresh form, this initial data will populate the `:data` map in
the result:

    :::clojure
    (sample-form)
    ; =>
    {:fresh true
     :results nil
     :data {:email "user@example.com"}
     ...}

When you run some data through the form, the initial data is ignored:

    :::clojure
    (sample-form {"email" "foo@bar.com"})
    ; =>
    {:fresh false
     :results {:email "foo@bar.com"}
     :data    {:email "foo@bar.com"}
     ...}

This means when you're rendering a form field to HTML you can simply pull the
field's entry from the `:data` map and fill the field with its contents.  It
doesn't matter whether it happed to be a fresh form or a form that had some
errors.

Note that initial data should always be specified as a string, since the `:data`
map will contain strings once you run data through it.

