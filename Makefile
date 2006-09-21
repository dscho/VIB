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
JAVACOPTS=-classpath $(PLUGINSHOME)/../ImageJ/ij.jar$(CPSEP)$(PLUGINSHOME)/jzlib-1.0.7.jar$(CPSEP)$(PLUGINSHOME)/imagescience.jar$(CPSEP).
JAVACOPTSCOMPAT= -source 1.3 -target 1.3

all: $(CLASSES)

%.class: %.java
	javac -O $(JAVACOPTS) "$^"

vib/FloatMatrix.java: vib/FastMatrix.java math3d/FloatMatrixN.class
	sed -e "s/double/float/g" -e "s/FastMatrix/FloatMatrix/g" -e "s/[0-9][0-9]*\.[0-9][0-9]*/&f/g" < $< > $@

math3d/FloatMatrixN.java: math3d/FastMatrixN.java
	sed -e "s/double/float/g" -e "s/FastMatrixN/FloatMatrixN/g" -e "s/[0-9][0-9]*\.[0-9][0-9]*/&f/g" < $< > $@

VIB_-compat.jar: SOURCES=$(JAVAS) vib/segment/icons/*.png

%-compat.jar:
	test ! -d tempdir || rm -rf tempdir
	mkdir tempdir
	tar cvf - $(SOURCES) | (cd tempdir; tar xvf -)
	(cd tempdir && sh ../compile1.3.sh && jar cvf ../$@ $$(find -type f)) && rm -rf tempdir

Delaunay_Voronoi.jar: SOURCES=$(wildcard Delaunay_Voronoi.java delaunay/*.java)

AmiraMesh_.jar: SOURCES=AmiraMeshReader_.java AmiraMeshWriter_.java \
	vib/AmiraParameters.java vib/AmiraMeshEncoder.java \
	vib/AmiraMeshDecoder.java vib/AmiraTableEncoder.java vib/AmiraTable.java

SIMPLE_JARS=Two_Point_Correlation.jar Scrollable_StackWindow.jar

$(SIMPLE_JARS): SOURCES=$(patsubst %.jar,%.java,$@)

JARS=Delaunay_Voronoi.jar AmiraMesh_.jar VIB_-compat.jar $(SIMPLE_JARS)

%.jar:
	test ! -d tempdir || rm -rf tempdir
	mkdir tempdir
	tar cvf - $(SOURCES) | (cd tempdir; tar xvf -)
	(cd tempdir && javac $(JAVACOPTS) $(JAVACOPTSCOMPAT) $(SOURCES) && jar cvf ../$@ $$(find -type f)) && rm -rf tempdir

