#!/usr/bin/env bash

rm -rf ./build
~/.virtualenvs/d/bin/d
hg -R ~/src/sjl.bitbucket.org pull -u
rsync --delete -a ./build/ ~/src/sjl.bitbucket.org/red-tape
hg -R ~/src/sjl.bitbucket.org commit -Am 'red-tape: Update site.'
hg -R ~/src/sjl.bitbucket.org push

