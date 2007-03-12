JAVAS=$(wildcard *.java */*.java */*/*.java */*/*/*.java */*/*/*/*.java)

# if no Java3d is available, do not attempt to compile the corresponding plugins
JAVA3DS=$(wildcard Viewer_3D.java marchingcubes/*.java voltex/*.java Volume_Renderer.java ij3d/*.java)
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

vib/FastMatrix.class: vib/FloatMatrix.class

math3d/FloatMatrixN.java: math3d/FastMatrixN.java
	sed -e "s/double/float/g" -e "s/FastMatrixN/FloatMatrixN/g" -e "s/[0-9][0-9]*\.[0-9][0-9]*/&f/g" < $< > $@

math3d/JacobiFloat.java: math3d/JacobiDouble.java
	sed -e "s/double/float/g" -e "s/FastMatrix/FloatMatrix/g" -e "s/Double/Float/g" < $< > $@

FibonacciHeapInt.java: FibonacciHeap.java Makefile
	sed -e "s/FibonacciHeap/FibonacciHeapInt/g" -e "s/ implements Comparable//" -e "s/Comparable/int/g" -e "s/\.compareTo(\([^)]*\))/- \1/g" -e "s/Object other/int other/g" -e "s/heap.add(p, p);/heap.add((int)prios[i], new Double((int)prios[i]));/" -e "s/Node(null/Node(0/" < $< > $@

VIB_compat.jar: SOURCES=$(filter-out $(FILTEROUT), $(JAVAS)) vib/segment/icons/*.png

Segmentation_Editor_compat.jar: SOURCES=amira/AmiraParameters.java \
	amira/AmiraTable.java vib/InterpolatedImage.java math3d/Point3d.java \
	vib/SegmentationViewerCanvas.java vib/segment/*.java

%_compat.jar:
	test ! -d tempdir
	mkdir tempdir
	tar cvf - $(SOURCES) | (cd tempdir; tar xvf -)
	(cd tempdir && sh ../compile1.3.sh && jar cvf ../$@ $$(find -type f)) && rm -rf tempdir

Delaunay_Voronoi.jar: SOURCES=$(wildcard Delaunay_Voronoi.java delaunay/*.java)

AmiraMesh_.jar: SOURCES=AmiraMeshReader_.java AmiraMeshWriter_.java \
	amira/AmiraParameters.java amira/AmiraMeshEncoder.java \
	amira/AmiraMeshDecoder.java amira/AmiraTableEncoder.java amira/AmiraTable.java

AmiraSurface_.jar: SOURCES=ShowAmiraSurface_.java amira/AmiraParameters.java \
	amira/AmiraTable.java vib/Image3dCanvas.java

Rigid_Registration.jar: SOURCES=vib/RigidRegistration_.java \
	vib/InterpolatedImage.java vib/TransformedImage.java \
	vib/FastMatrix.java vib/RegistrationOptimizer.java \
	amira/AmiraParameters.java amira/AmiraTable.java vib/VIB.java \
	vib/BatchLog_.java vib/Resample_.java \
	distance/PixelPairs.java distance/Euclidean.java \
	distance/TwoValues.java distance/MutualInformation.java \
	distance/Thresholded.java \
	pal/math/*.java \
	math3d/Point3d.java math3d/Triangle.java math3d/Line.java \
	math3d/Plane.java math3d/JacobiDouble.java math3d/FastMatrixN.java

Extract_Surface.jar: SOURCES=vib/ArrayBase.java vib/IntArray.java \
	vib/DoubleArray.java vib/Extract_Surface.java \
	vib/InterpolatedImage.java vib/Image3dCanvas.java \
	math3d/Point3d.java math3d/NormalEstimator.java

SIMPLE_JARS=Two_Point_Correlation.jar Scrollable_StackWindow.jar \
	Align_Image.jar Moving_Least_Squares.jar

$(SIMPLE_JARS): SOURCES=$(patsubst %.jar,%.java,$@)

JARS=Delaunay_Voronoi.jar AmiraMesh_.jar VIB_compat.jar $(SIMPLE_JARS)

%-compat.jar: %.jar
	test ! -d tempdir || rm -rf tempdir
	mkdir tempdir
	(cd tempdir && jar xvf ../$< && find -name \*.class -exec rm {} \; && sh ../compile1.3.sh && jar cvf ../$@ $$(find -type f)) && rm -rf tempdir

%.jar:
	test ! -d tempdir || rm -rf tempdir
	mkdir tempdir
	tar cvf - $(SOURCES) | (cd tempdir; tar xvf -)
	(cd tempdir && javac $(JAVACOPTS) $(JAVACOPTSCOMPAT) $(SOURCES) && jar cvf ../$@ $$(find -type f)) && rm -rf tempdir

clean:
	find -name \*.class -exec rm {} \;
	rm -f $(JARS)
