#!/usr/bin/python

import os
import sys

# Jython does not support removedirs
# Warning: this implementation is not space-safe!
if 'removedirs' in dir(os):
	def removedirs(dir):
		os.removedirs(dir)
else:
	def removedirs(dir):
		os.system('rm -rf ' + dir)

for d in sys.argv[1:]:
	removedirs(d)
