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


