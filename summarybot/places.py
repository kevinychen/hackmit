# run sudo pip install wikipedia
# usage: python getplace.py "White House"

import wikipedia
import sys
from subprocess import call

print wikipedia.summary(sys.argv[1], sentences=sys.argv[2])

