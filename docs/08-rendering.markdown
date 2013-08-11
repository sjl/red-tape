Rendering
=========

Red Tape does not provide any built-in functionality for rendering HTML.  It
leaves that up to you, so you can choose your preferred templating library.
However, it's built with a few key ideas in mind that should make it easier for
you do create forms that are usable and friendly for your users.

Let's look at a full example that shows all the things you should think about
when rendering a form.  This example will use the Hiccup templating library for
brevity, but as mentioned you can use any templating library you like.

Once you've read this section you're all set!  Dig in and start making some
forms.  The [reference guide](../reference/) will be there when you need it.

[TOC]

Basic Structure
---------------

Recall the structure of the GET handler from the [basics](../basics/) guide:

    :::clojure
    (defn handle-get
      ([request]
        (handle-get request (your-form)))
      ([request form]
        (... render the page, passing in the form ...)))

And also recall the POST handler:

    :::clojure
    (defn handle-post [request]
      (let [data (:params request)
            form (your-form data)]
        (if (:valid form)
          (... do whatever you need, then redirect somewhere else ...)
          (handle-get request form))))

As you can see, there's only one place where the form gets rendered in this
code.  This means your rendering code will all be in a single place, and should
handle result maps from fresh forms as well as ones that contain errors.

Rendering a Fresh Form
----------------------

Let's talk about the fresh form first.  We'll use a simple form as an example:

    :::clojure
    (require '[red-tape.cleaners :as cleaners])

    (defform simple-form {:initial {:name "Steve"}}
      :name [clojure.string/trim
             cleaners/non-blank])

Now we'll create the get handler for this:

    :::clojure
    (defn handle-get
      ([request]
        (handle-get request (simple-form)))
      ([request form]
        (simple-page form)))

Now we'll use Hiccup to create `simple-page`:

    :::clojure
    (defpage simple-page [form]
      (html
        [:body
         [:form {:method "POST"}
          [:label "What is your name?"]
          [:input {:name "name"
                   :value (get-in form [:data :name])}]
          [:input {:type "submit"}]]]))

Notice how the `value` for the name input is pulled out of the `:data` entry of
the result map.  This ensures that the field will be filled in with the correct
initial data ("Steve" in this example).

Rendering Errors
----------------

Suppose the user backspaces out the initial name and submits the form.  The
`non-blank` cleaner in the form means that a blank name is considered invalid.

The usual POST handler for a Red Tape form will use the GET handler to re-render
the result map of the invalid form, so our template should take this into
account and display errors properly.  Let's see how we might do that:

    :::clojure
    (defpage simple-page [form]
      (html
        [:body
         (if (:errors form)
           [:p "There was a problem.  Please try again."])
         [:form {:method "POST"}
          [:label "What is your name?"]
          (when-let [error (get-in form [:errors :name])]
            [:p error])
          [:input {:name "name"
                   :value (get-in form [:data :name])}]
          [:input {:type "submit"}]]]))

There are two additions here.  First we check if there were any errors at the
top of the form.  If there were, we display a message at the top of the form.

This is especially important when you have more than a single field, because the
field with the problem may be further down the page and the user needs to know
that they need to scroll down to see it.

We also check if there were any errors for the specific field, and if so we
display it.  In this simple example there's only one field, but in most forms
you'll have many fields, each of which may or may not have errors.

This form didn't have any form-level cleaners, but if it did we would have
wanted to display those too (probably at the top of the form).

Also remember that the `:data` in the form that we're using to prepopulate the
field will now contain whatever the user submitted.  That way we won't blow away
the things the user typed if there happens to be an error.  They can fix the
error and just hit submit.
