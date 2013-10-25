#!/bin/sh -ev

# If you want to use SSH-based URLs for submodules instead of GitHub's
# normal HTTPS-based URLs, run this script.
#
# Note that at one time, SSH-based URLs were required for pushing, but that is no
# longer true.  For further details, see
# http://stackoverflow.com/questions/11041729/why-does-github-recommend-https-over-ssh

git config submodule.models.url git@github.com:/NetLogo/models.git

# The above seems not to be doing the trick unless you run it before cloning,
# which is hard to remember to do.  So we bring out the big hammer:

( cd models; git remote set-url origin git@github.com:/NetLogo/models.git )
