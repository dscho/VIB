JAVAS=$(wildcard *.java */*.java */*/*.java)
CLASSES=$(patsubst %.java,%.class,$(JAVAS))

ifeq ($(uname_O),Cygwin)
PLUGINSHOME=$(shell cygpath --mixed $(shell pwd))
JAVACOPTS=-classpath $(PLUGINSHOME)/../ImageJ/ij.jar\;$(PLUGINSHOME)/jzlib-1.0.7.jar\;.
else
PLUGINSHOME=$(shell pwd)
JAVACOPTS=-classpath $(PLUGINSHOME)/../ImageJ/ij.jar:$(PLUGINSHOME)/jzlib-1.0.7.jar:.
endif

all: $(CLASSES)

%.class: %.java
	javac -O $(JAVACOPTS) "$^"

alll:
	sh compile.sh

