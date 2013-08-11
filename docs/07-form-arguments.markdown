Form Arguments
==============

Sometimes a simple form isn't enough.  When you need dynamic functionality in
your forms you can use Red Tape's form arguments.

Once you've gone through this document, read the [rendering](../rendering/)
guide.

[TOC]

The Problem
-----------

Let's use a simple example to demonstrate why form arguments are necessary and
how they work.  Suppose you have a site that hosts videos.  You'd like to have
a "delete video" form to let users delete their videos.  A first crack at it
might look like this:

    :::clojure
    (defn video-exists [video-id]
      ; Checks that the given video id exists...
    )

    (defform delete-video-form {}
      :video-id [video-exists])

Nothing new here.  But this form will let *anyone* delete *any* video.  In real
life you probably only want users to be able to delete their *own* videos.

Defining Form Arguments
-----------------------

To define form arguments you can use the `:arguments` entry in the form options
map, passing a vector of symbols.  Let's change our `delete-video-form` form:

    :::clojure
    (defform delete-video-form {:arguments [user]}
      :video-id [video-exists])

The form will now take a `user` argument.  So calling the form *without* data
would be done like this:

    :::clojure
    (let [current-user ...
          fresh-delete-form (delete-video-form current-user)])

And calling it *with* data would look like this:

    :::clojure
    (let [current-user ...
          post-data ...
          form (delete-video-form current-user post-data)])

Using Form Arguments
--------------------

Using form arguments relies on one key idea: your cleaner definitions are
evaluated in a context where the form arguments are bound to the symbols you
specified.

Here's how we could use our `user` argument:

    :::clojure
    (defn video-owned-by [user video-id]
      ; Check that the given user owns the given video id.
    )

    (defform delete-video-form {:arguments [user]}
      :video-id [video-exists
                 #(video-owned-by user %)])

    (let [current-user ...
          post-data ...
          form (delete-video-form current-user post-data)]
      ...)

Notice how the second cleaner in the `defform` is defined as `#(video-owned-by
user %)`.  `user` here is the form argument, so the anonymouse function will be
created with the appropriate user each time `delete-form` is called.

Initial Data
------------

The initial data map is also evaluated in a context where the form arguments are
bound.  You can use this to define default values based on the form arguments.

For example: suppose you have a profile page where users can change their email
address and profile.  A form for this might look like:

    :::clojure
    (defform profile-form {:arguments [user]
                           :initial {:email (:email user)
                                     :bio (:bio user)}}
       :email [#(cleaners/matches #"\S+@\S+" %
                                  "Please enter a valid email address.")]
       :bio [])

    (profile-form some-user)
    ; =>
    {...
     :results nil
     :errors nil
     :data {:email "steve@stevelosh.com"
            :bio "Computers are terrible."}}

    (profile-form some-user {:email "this is not an email"
                             :bio "I like cats."})
    ; =>
    {...
     :results nil
     :errors {:email "Please enter a valid email address"}
     :data {:email "this is not an email"
            :bio "I like cats."}}

Notice how in the fresh form the `:data` is prepopulated with the user's
information that was pulled from the form argument.  Once the user actually
enters some data, the initial data is ignored.
