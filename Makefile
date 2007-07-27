JAVAS=$(shell find * -name \*.java | grep -v ^tempdir)

# if no Java3d is available, do not attempt to compile the corresponding plugins
JAVA3DS=$(wildcard marchingcubes/*.java voltex/*.java ij3d/*.java isosurface/*.java orthoslice/*.java ImageJ_3D_Viewer.java)
FILTEROUT=$(JAVA3DS)
ifneq ($(JAVA_HOME),)
	ifneq ($(wildcard $(JAVA_HOME)/jre/lib/ext/j3dcore.jar),)
		FILTEROUT=
	endif
else
	ifneq ($(wildcard /System/Library/Java/Extensions/j3dcore.jar),)
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
JAVACOPTS=-classpath $(PLUGINSHOME)/../ImageJ/ij.jar$(CPSEP)$(PLUGINSHOME)/jzlib-1.0.7.jar$(CPSEP)$(PLUGINSHOME)/imagescience.jar$(CPSEP)$(PLUGINSHOME)/Quick3dApplet-1.0.8.jar$(CPSEP).$(CPSEP)$(PLUGINSHOME)/jython.jar$(CPSEP).
JAVACOPTSCOMPAT= -source 1.3 -target 1.3

all: $(CLASSES)

show:
	echo $(JAVAS)

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

Segmentation_Editor_compat.jar: SOURCES=amira/*.java \
	vib/InterpolatedImage.java math3d/Point3d.java \
	vib/SegmentationViewerCanvas.java vib/segment/*.java \
	vib/IDT_Interpolate_Binary.java math3d/FastMatrixN.java \
	Segmentation_Editor.java vib/segment/icons/*.png \
	vib/segment/materials/* vib/Local_Threshold.java

%_compat.jar:
	test ! -d tempdir || rm -rf tempdir
	mkdir tempdir
	tar cvf - $(SOURCES) | (cd tempdir; tar xvf -)
	(cd tempdir && sh ../compile1.3.sh && jar cvf ../$@ $$(find . -type f)) && rm -rf tempdir

Delaunay_Voronoi.jar: SOURCES=$(wildcard Delaunay_Voronoi.java delaunay/*.java)

AmiraMesh_.jar: SOURCES=AmiraMeshReader_.java AmiraMeshWriter_.java \
	amira/AmiraParameters.java amira/AmiraMeshEncoder.java \
	amira/AmiraMeshDecoder.java amira/AmiraTableEncoder.java \
	amira/AmiraTable.java

AmiraSurface_.jar: SOURCES=ShowAmiraSurface_.java amira/AmiraParameters.java \
	amira/AmiraTable.java vib/Image3dCanvas.java

Rigid_Registration.jar: SOURCES=vib/RigidRegistration_.java \
	vib/InterpolatedImage.java vib/TransformedImage.java \
	vib/FastMatrix.java vib/RegistrationOptimizer.java \
	amira/AmiraParameters.java amira/AmiraTable.java vib/VIB.java \
	vib/BatchLog_.java vib/Resample_.java vib/FloatMatrix.java \
	distance/PixelPairs.java distance/Euclidean.java \
	distance/TwoValues.java distance/MutualInformation.java \
	distance/Thresholded.java \
	pal/math/*.java \
	math3d/Point3d.java math3d/Triangle.java math3d/Line.java \
	math3d/Plane.java math3d/JacobiDouble.java math3d/FastMatrixN.java \
	math3d/JacobiFloat.java math3d/FloatMatrixN.java

Extract_Surface.jar: SOURCES=vib/ArrayBase.java vib/IntArray.java \
	vib/DoubleArray.java vib/Extract_Surface.java \
	vib/InterpolatedImage.java vib/Image3dCanvas.java \
	math3d/Point3d.java math3d/NormalEstimator.java

ImageJ_3D_Viewer.jar: SOURCES=$(wildcard ij3d/*.java) $(wildcard voltex/*.java)\
	$(wildcard marchingcubes/*.java) $(wildcard isosurface/*.java) \
	$(wildcard orthoslice/*.java) \
	vib/Resample_.java vib/InterpolatedImage.java \
	amira/AmiraParameters.java amira/AmiraTable.java \
	math3d/Point3d.java math3d/Transform_IO.java ImageJ_3D_Viewer.java

Install_Java3D.jar: SOURCES=Install_Java3D.java

ThreePaneCrop_.jar: SOURCES=stacks/PaneOwner.java \
	stacks/ThreePaneCropCanvas.java \
	stacks/ThreePaneCrop_.java \
	stacks/ThreePaneCrop.java \
	stacks/ThreePanesCanvas.java \
	stacks/ThreePanes.java

SimpleNeuriteTracer_.jar: SOURCES=stacks/ThreePanes.java \
	stacks/ThreePanesCanvas.java \
	stacks/PaneOwner.java \
	tracing/AStarNode.java \
	tracing/AStarProgressCallback.java \
	tracing/AStarThread.java \
	tracing/Connection.java \
	tracing/SimpleNeuriteTracer_.java \
	tracing/NeuriteTracerResultsDialog.java \
	tracing/PointInImage.java \
	tracing/SegmentedConnection.java \
	tracing/TracerCanvas.java \
	tracing/FillerThread.java \
	tracing/FillerProgressCallback.java \
	tracing/FillerNode.java \
	tracing/NormalPlaneCanvas.java \
	$(wildcard pal/math/*.java) \
	features/ComputeCurvatures.java \
	features/GaussianGenerationCallback.java \
	client/ArchiveClient.java \
	util/Arrow.java \
	util/ArrowDisplayer.java

ExportMesh_.jar: SOURCES=marchingcubes/ExportMesh_.java \
	marchingcubes/MCTriangulator.java \
	marchingcubes/MCCube.java \
	vib/Resample_.java \
	amira/AmiraParameters.java \
	amira/AmiraTable.java \
	ij3d/ColorTable.java \
	isosurface/Triangulator.java \
	vib/InterpolatedImage.java \
	math3d/Point3d.java

VIB_Protocol.jar: SOURCES=$(wildcard vib/app/*.java) \
	$(wildcard vib/app/gui/*.java) \
	$(wildcard vib/app/module/*.java) \
	$(wildcard vib/segment/*.java) \
	$(wildcard distance/*.java) \
	$(wildcard leica/*.java) \
	$(wildcard amira/*.java) \
	$(wildcard math3d/*.java) \
	$(wildcard pal/math/*.java) \
	vib/DiffusionInterpol2_.java \
	vib/Resample_.java \
	vib/InterpolatedImage.java \
	vib/TissueStatistics_.java \
	vib/FloatMatrix.java \
	vib/FastMatrix.java \
	vib/TransformedImage.java \
	vib/RigidRegistration_.java \
	vib/RegistrationOptimizer.java \
	vib/AverageBrain_.java \
	vib/SegmentationViewerCanvas.java \
	vib/VIB.java \
	vib/BatchLog_.java \
	vib/IDT_Interpolate_Binary.java \
	VIB_Protocol.java

SIMPLE_JARS=Two_Point_Correlation.jar Scrollable_StackWindow.jar \
	Align_Image.jar Moving_Least_Squares.jar

$(SIMPLE_JARS): SOURCES=$(patsubst %.jar,%.java,$@)

JARS=Delaunay_Voronoi.jar AmiraMesh_.jar Extract_Surface.jar \
	Rigid_Registration.jar \
	Segmentation_Editor_compat.jar VIB_compat.jar \
	ImageJ_3D_Viewer.jar \
	$(SIMPLE_JARS)

show-jars:
	echo $(JARS)

all-jars: $(JARS)

clean-jars:
	rm $(JARS)

%-compat.jar: %.jar
	test ! -d tempdir || rm -rf tempdir
	mkdir tempdir
	(cd tempdir && jar xvf ../$< && find . -name \*.class -exec rm {} \; && sh ../compile1.3.sh && jar cvf ../$@ $$(find . -type f)) && rm -rf tempdir

%.jar:
	test ! -d tempdir || rm -rf tempdir
	mkdir tempdir
	tar cvf - $(SOURCES) | (cd tempdir; tar xvf -)
	(cd tempdir && javac $(JAVACOPTS) $(SOURCES) && jar cvf ../$@ $$(find . -type f)) && rm -rf tempdir

# Unpack the jar, remove the source files and jar it up again :)

%.jar-without-source: %.jar
	test ! -d tempdir || rm -rf tempdir
	mkdir tempdir
	(cd tempdir && jar xvf ../$< && find . -name '*.java' -exec rm {} \; && jar cvf ../$@ $$(find . -type f)) && rm -rf tempdir

clean:
	find . -name \*.class -exec rm {} \;
	rm -f $(JARS)
