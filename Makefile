JAVAS=$(wildcard *.java */*.java */*/*.java)
CLASSES=$(patsubst %.java,%.class,$(JAVAS))

uname_O := $(shell sh -c 'uname -o 2>/dev/null || echo not')
ifeq ($(uname_O),Cygwin)
PLUGINSHOME=$(shell cygpath --mixed $(shell pwd))
CPSEP=\;
else
PLUGINSHOME=$(shell pwd)
CPSEP=:
endif
JAVACOPTS=-classpath $(PLUGINSHOME)/../ImageJ/ij.jar$(CPSEP)$(PLUGINSHOME)/jzlib-1.0.7.jar$(CPSEP).

all: $(CLASSES)

%.class: %.java
	javac -O $(JAVACOPTS) "$^"

vib/FloatMatrix.java: vib/FastMatrix.java math3d/FloatMatrixN.class
	sed -e "s/double/float/g" -e "s/FastMatrix/FloatMatrix/g" -e "s/[0-9][0-9]*\.[0-9][0-9]*/&f/g" < $< > $@

math3d/FloatMatrixN.java: math3d/FastMatrixN.java
	sed -e "s/double/float/g" -e "s/FastMatrixN/FloatMatrixN/g" -e "s/[0-9][0-9]*\.[0-9][0-9]*/&f/g" < $< > $@
