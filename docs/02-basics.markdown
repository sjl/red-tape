Basics
======

Red Tape is a fairly simple library.  It's designed to take raw form data
(strings), validate it, and turn it into useful data structures.

Red Tape does *not* handle rendering form fields into HTML.  That's the job of
your templating library, and you always need to customize `<input>` tags anyway.

It's designed with Ring, Compojure, and friends in mind (though it's not limited
to them) so let's take a look at a really simple application to see it in
action.

[TOC]

Scaffolding
-----------

For this tutorial we'll create a Compojure app that allows people to submit
comments.  Let's sketch out the normal structure of that now:

    :::clojure
    (ns feedback
      (require [compojure.core :refer :all]
               [compojure.route :as route]
               [hiccup.page :refer [html5]]
               [ring.adapter.jetty :refer [run-jetty]]
               [ring.middleware.params :refer [wrap-params]]))

    (defn page []
      (html5
        [:body
         [:form {:method "POST" :action "/"}
          [:label "Who are you?"]
          [:input {:type "text" :name "name"}]
          [:label "What do you want to say?"]
          [:textarea {:name "comment"}]]]))

    (defn save-feedback [from comment]
      ; In a real app this would save the string to a database, email
      ; it to someone, etc.
      (println from "said:" comment))

    (defn handle-get []
      ; ...
    )

    (defn handle-post []
      ; ...
    )

    (defroutes app
      (GET  "/" request (handle-get request))
      (POST "/" request (handle-post request)))

    (def handler (-> app
                   wrap-params))

    (defonce server
      (run-jetty #'handler {:port 3000}))

That's about it for the boilerplate.  The next step is to fill in the bodies of
`handle-get` and `handle-post`.  We'll need to do a few things:

* Render the initial comment form when the user first GETs the page.
* Validate incoming POST data to make sure it's sane.
* If the data isn't valid, inform the user and re-render the form nicely so they
  can fix it.
* Once we've got valid data, clean it up and send it off to be saved.

This is where Red Tape comes in.

Defining the Form
-----------------

The main part of Red Tape is the `defform` macro.  Let's define a simple
feedback form:

    :::clojure
    (ns feedback
      ; ...
      (require [red-tape.core :refer [defform]]))

    (defform feedback-form {}
      :name []
      :comment [])

`defform` takes a name, a map of form options, and a sequence of keywords and
vectors representing fields.  We'll look at each of those parts in more detail
later, but for now let's actually *use* the form we've defined.

Using the Form
--------------

Defining a form results in a simple function that can be called with or without
data.  Let's sketch out how our handler functions will look:

    :::clojure
    (defn handle-get
      ([request]
        (handle-get request (feedback-form)))
      ([request form]
        (page)))

There are a couple of things going on here.

We've split the definition of `handle-get` into two pieces.  The first piece
takes a request, builds the default feedback form and forwards those along to
the second piece, which actually renders the page.  You'll see why we split it
up like that shortly.

Calling `(feedback-form)` without data returns a "result map" representing a fresh
form.  It will look like this:

    :::clojure
    {:fresh true
     :valid false
     :arguments {}
     :data {}
     :results nil
     :errors nil}

We'll see how to use this later.  Let's move on to `handle-post`:

    :::clojure
    (defn handle-post [request]
      (let [data (:params request)
            form (feedback-form data)]
        ; ...))

`handle-post` takes the raw HTTP POST data (from `(:params request)`) and passes
it through the feedback form.  Once again this results in a map.  Assuming the
user entered the name "Steve" and the comment "Hello!", the resulting map will
look like this:

    :::clojure
    {:fresh false
     :valid true
     :arguments {}
     :data {:name "Steve"
            :comment "Hello!"}
     :results {:name "Steve"
               :comment "Hello!"}
     :errors nil}

In a nutshell, this is all Red Tape does.  You define form functions using
`defform`, and those functions take in data and turn it into a result map like
this.

Let's add a bit of data cleaning to the form to get something more useful.

Cleaners
--------

Every field you define in a `defform` also gets a vector of "cleaners"
associated with it.  A cleaner is a vanilla Clojure function that takes one
argument (the incoming value) and returns a new value (the outgoing result).

Let's see this in action by modifying our form to strip leading and trailing
whitespace from the user's name automatically:

    :::clojure
    (defform feedback-form {}
      :name [clojure.string/trim]
      :comment [])

`clojure.string/trim` is just a normal Clojure function that trims off
whitespace.  Let's imagine that the user entered `     Steve ` as their name
this time.  Calling `(feedback-form data)` now results in the following map:

    :::clojure
    {:fresh false
     :valid true
     :arguments {}
     :data    {:name "    Steve " :comment "Hello!"}
     :results {:name "Steve"      :comment "Hello!"}
     :errors nil}

The `:data` in the result map still contains the raw data the user entered, but
the `:results` have had their values passed through their cleaners first.

You can define as many cleaners as you want for each field.  The data will be
threaded through them in order, much like the `->` macro.  This lets you define
simple cleaning functions and combine them as needed.  For example:

    :::clojure
    (defform feedback-form {}
      :name [clojure.string/trim
             clojure.string/lower-case]
      :comment [clojure.string/trim])

    (feedback-form {:name "    Steve " :comment " Hello! "})
    ; =>
    {:fresh false
     :valid true
     :data    {:name "    Steve " :comment " Hello! "}
     :results {:name "steve"      :comment "Hello!"}
     ; ...
     }

Here we're trimming the name and then lowercasing it, and trimming the comment
as well (but not lowercasing that).

Validation
----------

Cleaners also serve another purpose.  If a cleaner function throws an Exception,
the value won't progress any further, and the result map will be marked as
invalid.

Let's look at an example:

    :::clojure
    (defform age-form {}
      :age [clojure.string/trim
            #(Long. %)])

If we call this form with a number, everything is fine:

    :::clojure
    (age-form {:age "27"})
    ; =>
    {:fresh false
     :valid true
     :data    {:age "27"}
     :results {:age 27}
     :errors nil}

But if we try to feed it garbage:

    :::clojure
    (age-form {:age "cats"})
    ; =>
    {:fresh false
     :valid false
     :data {:age "cats"}
     :results nil
     :errors {:age <NumberFormatException: ...>}}

There are a few things to see here.  If any cleaner function throws an
Exception, the resulting map will have `:valid` set to `false`.

There will also be no `:results` entry in an invalid result.  You only get
`:results` if your *entire* form is valid.  This is to help prevent you from
accidentally using the results of a form with invalid data.

The `:errors` map will map field names to the exception their cleaners threw.
This happens on a per-field basis, so you can have separate errors for each
field.

Red Tape uses Slingshot's `try+` to catch exceptions, so if you want to you can
use `throw+` to throw errors in an easier-to-manage way and they'll be caught
just fine.

    :::clojure
    (defn ensure-not-immortal [age]
      (if (> age 150)
        (throw+ "I think you're lying!")
        age))

    (defform age-form {}
      :age [clojure.string/trim
            #(Long. %)
            ensure-not-immortal])

    (age-form {:age "1000"})
    ; =>
    {:fresh false
     :valid false
     :data {:age "1000"}
     :results nil
     :errors {:age "I think you're lying!"}}

Notice how `ensure-not-immortal` expected a number and not a String.  This is
fine because we still kept the `#(Long. %)` cleaner to handle that conversion.

Finally, also notice that the `:data` entry in the result map is present and
contains the data the user entered, even though it turned out to be invalid.
We'll use this later when we want to rerender the form, so the user doesn't have
to type all the data again.

Built-In Cleaners
-----------------

Red Tape contains a number of useful cleaner functions pre-defined in the
`red-tape.cleaners` namespace.

We'll use `red-tape.cleaners/non-blank` in this tutorial.  `non-blank` is
a simple cleaner that throws an exception if it receives an empty string, or
otherwise passes through the data unchanged.

Let's change the form to make sure that users don't try to submit an empty
comment (but we'll still allow an empty name, in case someone wants to comment
anonymously):

    :::clojure
    (ns feedback
      ; ...
      (require [red-tape.cleaners :as cleaners]))

    (defform feedback-form {}
      :name [clojure.string/trim]
      :comment [clojure.string/trim cleaners/non-blank])

Notice that we trim whitespace *before* checking for a non-blank string, so
a comment of all whitespace would result in an error.

Putting it All Together
-----------------------

Now that we've seen how to clean and validate, we can finally connect the
missing pieces to our feedback form.

First we'll redefine our little HTML page to take the form as an argument, so we
can pre-fill the inputs with any initial data:

    :::clojure
    (defn page [form]
      (html5
        [:body
         [:form {:method "POST" :action "/"}
          [:label "Who are you?"]
          [:input {:type "text" :name "name"
                   :value (get-in form [:data :name])}]
          [:label "What do you want to say?"]
          [:textarea {:name "comment"} (get-in form [:data :comment])]]]))

Notice how we pull the values of each field out of the `:data` entry in the form
result map.

Now we can write the final GET handler:

    :::clojure
    (defn handle-get
      ([request]
        (handle-get request (feedback-form)))
      ([request form]
        (page form)))

And the POST handler:

    :::clojure
    (defn handle-post [request]
      (let [data (:params request)
            form (feedback-form data)]
        (if (:valid form)
          (let [{:keys [name comment]} (:results form)]
            (save-feedback name comment)
            (redirect "/"))
          (handle-get request form))))

We use the form to process the raw data, and then examine the result.  If it is
valid, we save the feedback by using the cleaned `:results` and we're done.

If it's *not* valid, we use the GET handler to re-render the form without
redirecting.  We pass along our *invalid* form as we do that, so that when the
GET handler calls the `page` and uses the `:data` it will fill in the fields
correctly so the user doesn't have to retype everything.

Summary
-------

That was a lot to cover, but now you've seen the basic Red Tape workflow!  Most
of the time you'll be doing what we just finished:

* Defining the form.
* Defining a GET handler that creates a blank form.
* Defining a GET handler that takes a form (either blank or invalid) and renders
  it to HTML.
* Defining a POST handler that runs data through the form, examines the result,
  and does the appropriate thing depending on whether it's valid or not.

Now that you've got the general idea, it's time to look at a few topics in more
detail.  Start with the [form input](../input/) guide.

