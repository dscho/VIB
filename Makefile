JAVAS=$(wildcard *.java */*.java */*/*.java)

# if no Java3d is available, do not attempt to compile the corresponding plugins
JAVA3DS=$(wildcard Viewer_3D.java marchingcubes/*.java)
FILTEROUT=$(JAVA3DS)
ifneq ($(JAVA_HOME),)
	ifneq ($(wildcard $(JAVA_HOME)/jre/lib/ext/j3dcore.jar),)
		FILTEROUT=
	endif
endif
CLASSES=$(patsubst %.java,%.class,$(filter-out $(FILTEROUT),$(JAVAS)))

uname_O := $(shell sh -c 'uname -o 2>/dev/null || echo not')
ifeq ($(uname_O),Cygwin)
PLUGINSHOME=$(shell cygpath --mixed $(shell pwd))
CPSEP=\;
else
PLUGINSHOME=$(shell pwd)
CPSEP=:
endif
JAVACOPTS=-classpath $(PLUGINSHOME)/../ImageJ/ij.jar$(CPSEP)$(PLUGINSHOME)/jzlib-1.0.7.jar$(CPSEP)$(PLUGINSHOME)/imagescience.jar$(CPSEP)$(PLUGINSHOME)/Quick3dApplet-1.0.8.jar$(CPSEP).
JAVACOPTSCOMPAT= -source 1.3 -target 1.3

all: $(CLASSES)

%.class: %.java
	javac -O $(JAVACOPTS) "$<"

Affine_FromMarkers.class: Affine_FromMarkers.java Name_Points.class PCA_Registration.class

vib/FloatMatrix.java: vib/FastMatrix.java math3d/FloatMatrixN.class
	sed -e "s/double/float/g" -e "s/FastMatrix/FloatMatrix/g" -e "s/JacobiDouble/JacobiFloat/g" -e "s/[0-9][0-9]*\.[0-9][0-9]*/&f/g" < $< > $@

math3d/FloatMatrixN.java: math3d/FastMatrixN.java
	sed -e "s/double/float/g" -e "s/FastMatrixN/FloatMatrixN/g" -e "s/[0-9][0-9]*\.[0-9][0-9]*/&f/g" < $< > $@

math3d/JacobiFloat.java: math3d/JacobiDouble.java
	sed -e "s/double/float/g" -e "s/FastMatrix/FloatMatrix/g" -e "s/Double/Float/g" < $< > $@

VIB_compat.jar: SOURCES=$(filter-out $(FILTEROUT), $(JAVAS)) vib/segment/icons/*.png

%_compat.jar:
	test ! -d tempdir
	mkdir tempdir
	tar cvf - $(SOURCES) | (cd tempdir; tar xvf -)
	(cd tempdir && sh ../compile1.3.sh && jar cvf ../$@ $$(find -type f)) && rm -rf tempdir

Delaunay_Voronoi.jar: SOURCES=$(wildcard Delaunay_Voronoi.java delaunay/*.java)

AmiraMesh_.jar: SOURCES=AmiraMeshReader_.java AmiraMeshWriter_.java \
	vib/AmiraParameters.java vib/AmiraMeshEncoder.java \
	vib/AmiraMeshDecoder.java vib/AmiraTableEncoder.java vib/AmiraTable.java

AmiraSurface_.jar: SOURCES=ShowAmiraSurface_.java vib/AmiraParameters.java \
	vib/AmiraTable.java vib/Image3dCanvas.java

SIMPLE_JARS=Two_Point_Correlation.jar Scrollable_StackWindow.jar \
	Align_Image.jar

$(SIMPLE_JARS): SOURCES=$(patsubst %.jar,%.java,$@)

JARS=Delaunay_Voronoi.jar AmiraMesh_.jar VIB_-compat.jar $(SIMPLE_JARS)

%.jar:
	test ! -d tempdir || rm -rf tempdir
	mkdir tempdir
	tar cvf - $(SOURCES) | (cd tempdir; tar xvf -)
	(cd tempdir && javac $(JAVACOPTS) $(JAVACOPTSCOMPAT) $(SOURCES) && jar cvf ../$@ $$(find -type f)) && rm -rf tempdir

