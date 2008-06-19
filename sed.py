#!/usr/bin/python

import re
import sys

if len(sys.argv) < 3:
	print "Invalid invocation"
	exit(1)

if sys.argv[2] in ["vib/FloatMatrix.java", "math3d/FloatMatrixN.java", \
		"math3d/JacobiFloat.java"]:
	replace = [r"double", r"float",
		r"FastMatrix", r"FloatMatrix",
		r"Double", r"Float",
		r"([0-9][0-9]*\.[0-9][0-9]*)(?!f)", r"\1f"]
elif sys.argv[2] in ["math3d/Eigensystem3x3Float.java", \
		"math3d/Eigensystem2x2Float.java"]:
	replace = [r"/\*change\*/double", r"float",
		r"Double", r"Float"]
elif sys.argv[2] == "FibonacciHeapInt.java":
	replace = [r"FibonacciHeap", r"FibonacciHeapInt",
		r" implements Comparable", r"",
		r"Comparable", r"int",
		r"\.compareTo\(([^\)]*)\)", r"- \1",
		r"Object other", r"int other",
		r"heap.add\(p, p\);",
			r"heap.add((int)prios[i], new Double((int)prios[i]));",
		r"Node\(null", r"Node(0"]

if replace == None or len(replace) == 0 or (len(replace) % 2) != 0:
	print "Invalid replace array"
	exit(1)

f = open(sys.argv[1], 'r')
buf = ''.join(f.readlines())
f.close()

for i in range(0, len(replace), 2):
	pattern = re.compile(replace[i])
	buf = pattern.sub(replace[i + 1], buf)

f = open(sys.argv[2], 'w')
f.write(buf)
f.close()
