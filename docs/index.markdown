Red Tape is a Clojure library for processing form data.  It's heavily inspired
by Django's forms (the good parts), with some Clojurey twists.

**License:** MIT/X11  
**Documentation:** <http://sjl.bitbucket.org/red-tape/>  
**Issues:** <http://github.com/sjl/red-tape/issues/>  
**Mercurial:** <http://bitbucket.org/sjl/red-tape/>  
**Git:** <http://github.com/sjl/red-tape/>

What Does it Look Like?
-----------------------

Why Red Tape?
-------------

There are a lot of other Clojure "validation" libraries out there.  I wasn't
happy with any of them:

* Some try to be too general and validate *anything*.  Red Tape is designed to
  work with user-submitted web form data with very little friction.
* Some don't do enough.  Validating data is only the first step.  When working
  with forms you want to transform data, handle initial data, and so on.
* Some are too complicated.  Red Tape uses just a little bit of macro magic
  combined with vanilla Clojure functions and Slingshot to do everything.

In a nutshell, Red Tape was made by someone who used Django's forms in client
sites for years and knows all the [rough edges][goodfields] that needed sanding.

[goodfields]:

Get Started
-----------

Get started by [installing](./installation/) Red Tape, then move on to [the
basics](./basics/).

