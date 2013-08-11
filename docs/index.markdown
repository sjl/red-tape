Red Tape is a Clojure library for processing form data.  It's heavily inspired
by Django's forms (the good parts), with some Clojurey twists.

**License:** MIT/X11  
**Documentation:** <http://sjl.bitbucket.org/red-tape/>  
**Issues:** <http://github.com/sjl/red-tape/issues/>  
**Mercurial:** <http://bitbucket.org/sjl/red-tape/>  
**Git:** <http://github.com/sjl/red-tape/>

What Does it Look Like?
-----------------------

You'll need to read the docs to really understand what's going on, but here's
a quick example so you can see the shape of the code.

First you'll define a form:

    :::clojure
    (ns my-web-app
        (:require [red-tape.core :refer defform]
                  [red-tape.cleaners :as cleaners]))

    (defform comment-form
      {:arguments [user]
       :initial {:email (:email-address user)}}

      :email ^:red-tape/optional
             [#(cleaners/matches #"\S+@\S+" %
                                 "Enter a valid email (or leave it blank).")]
      :comment [clojure.string/trim
                cleaners/non-blank
                #(cleaners/max-length 2000 %)])

A form can have some arguments, initial data, as well as some fields.  Fields
have functions for validating data and massaging it into what you need.

Now we can use the form to create an initial form (which we'll display to the
user so they can fill it in):

    :::clojure
    (def steve {:email "steve@stevelosh.com"})
    (def anon {:email ""})

    (comment-form steve)
    ; =>
    {:fresh true
     :valid nil
     :results nil
     :errors nil
     :data {:email "steve@stevelosh.com" :comment ""}
     :arguments {:user {:email "steve@stevelosh.com"}}}

    (comment-form anon)
    ; =>
    {:fresh true
     :valid nil
     :results nil
     :errors nil
     :data {:email "" :comment ""}
     :arguments {:user {:email ""}}}

And once they submit it we use the form once again to validate and transform the
data:

    :::clojure
    (comment-form steve {:email "steve+nospam@stevelosh.com"
                         :comment "    Hello!"})
    ; =>
    {:fresh false
     :valid true
     :results {:email "steve+nospam@stevelosh.com"
               :comment "Hello!"}
     :errors nil
     :data {:email "steve+nospam@stevelosh.com"
            :comment "    Hello!"}
     :arguments {:user {:email "steve@stevelosh.com"}}}

If there are errors, we know the data was bad and we need to show the form to
the user again so they can fix it:

    :::clojure
    (comment-form anon {:email ""
                        :comment ""})
    ; =>
    {:fresh false
     :valid false
     :results nil
     :errors {:comment "This field is required."}
     :data {:email ""
            :comment ""}
     :arguments {:user {:email ""}}}

But you'll really need to read the docs to understand what's happening here.

Why Red Tape?
-------------

There are a lot of other Clojure "validation" libraries out there.  I wasn't
happy with any of them for a few reasons:

* Some try to be too general and validate *anything*.  Red Tape is designed to
  work with user-submitted web form data with very little friction.
* Some don't do enough.  Validating data is only the first step.  When working
  with forms you want to transform data, handle initial data, and so on.  Red
  Tape knows what real-world web forms needs and gives it to you.
* Some are too complicated.  Red Tape uses just a little bit of macro magic
  combined with vanilla Clojure functions and Slingshot to do everything.  This
  makes it very easy to extend and work with.

In a nutshell, Red Tape was made by someone who used Django's forms in client
sites for years and knows all the [rough edges][goodfields] that needed sanding.

[goodfields]:

Get Started
-----------

Get started by [installing](./installation/) Red Tape, then move on to [the
basics](./basics/).

