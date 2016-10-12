.PHONY: test docs pubdocs test-sbcl test-ccl test-ecl vendor

docfiles = $(shell hg files docs)

docs: docs/build/index.html

docs/build/index.html: $(docfiles)
	cd docs && ~/.virtualenvs/d/bin/d

pubdocs: docs/build/index.html
	hg -R ~/src/sjl.bitbucket.org pull -u
	rsync --delete -a ./docs/build/ ~/src/sjl.bitbucket.org/red-tape
	hg -R ~/src/sjl.bitbucket.org commit -Am 'red-tape: Update site.'
	hg -R ~/src/sjl.bitbucket.org push
