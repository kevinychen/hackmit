# run sudo pip install wikipedia
# usage: python getplace.py "White House"

import wikipedia
import sys
from subprocess import call
import codecs

UTF8Writer = codecs.getwriter('utf8')
sys.stdout = UTF8Writer(sys.stdout)

print wikipedia.summary(sys.argv[1], sentences=sys.argv[2])

