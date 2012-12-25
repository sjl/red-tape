Form Arguments
==============

Sometimes a simple form isn't enough.  When you need dynamic functionality in
your forms you can use Red Tape's form arguments.

[TOC]

The Problem
-----------

Let's use a simple example to demonstrate why form arguments are necessary and
how they work.  Suppose you have a site that hosts videos.  You'd like to have
a "Delete Video" form to let users delete their videos.  A first crack at this
might look like this:

    :::clojure
    (defn video-exists [video-id]
      ; Checks that the given video id exists...
    )

    (defform delete-video {}
      :video-id [video-exists])

Nothing new here.  But this form will let *anyone* delete *any* video.  In real
life you probably only want users to be able to delete their *own* videos.

Defining Form Arguments
-----------------------

To define form arguments you can use the `:arguments` entry in the form options
map, passing a vector of symbols.  Let's change our `delete-video` form:

    :::clojure
    (defform delete-video {:arguments [user]}
      :video-id [video-exists])

The form will now take a `user` argument.  So calling the form without data
would be done like this:

    :::clojure
    (let [current-user ...
          fresh-delete-form (delete-form current-user)])

And calling it with data would look like this:

    :::clojure
    (let [current-user ...
          post-data ...
          form (delete-form current-user post-data)])

Using Form Arguments
--------------------

Using form arguments is simple.  There's one key idea: your cleaner definitions
are evaluated in a context where the form arguments are bound.

Here's how we could use our `user` argument:

    :::clojure
    (defn owned-by [user video-id]
      ; Check that the given user owns the given video id.
    )

    (defform delete-video {:arguments [user]}
      :video-id [video-exists (partial owned-by user)])

    (let [current-user ...
          post-data ...
          form (delete-form current-user post-data)])

Notice how the second cleaner in the `defform` is defined as `(partial owned-by
user)`.  `user` here is the form argument, so the partial function will be
created with the appropriate user each time `delete-form` is called.

Initial Data
------------

The initial data map is also evaluated in a context where the form arguments are
bound.

TODO: More about this.
