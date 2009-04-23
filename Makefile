.PHONY : show test

JAVAS=$(shell find * -name \*.java | grep -v ^tempdir)

# if no Java3d is available, do not attempt to compile the corresponding plugins
JAVA3DS=$(wildcard util/Meshes_From_Label_File.java marchingcubes/*.java voltex/*.java ij3d/*.java isosurface/*.java orthoslice/*.java ImageJ_3D_Viewer.java MC_Test.java Test_Java3D.java view4d/*.java surfaceplot/*.java)
FILTEROUT=$(JAVA3DS)
ifneq ($(JAVA_HOME),)
	ifneq ($(wildcard $(JAVA_HOME)/jre/lib/ext/j3dcore.jar),)
		FILTEROUT=
	endif
	ifneq ($(wildcard $(JAVA_HOME)/Home/lib/ext/j3dcore.jar),)
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
ifeq ($(wildcard $(PLUGINSHOME)/../ImageJ/ij.jar),)
	IJ_JAR=../ij.jar
else
	IJ_JAR=../ImageJ/ij.jar
endif

JAVACOPTSCOMPAT= -source 1.5 -target 1.5
JAVACOPTS=-classpath $(PLUGINSHOME)/$(IJ_JAR)$(CPSEP)$(PLUGINSHOME)/jzlib-1.0.7.jar$(CPSEP)$(PLUGINSHOME)/imagescience.jar$(CPSEP).$(CPSEP)$(PLUGINSHOME)/jython.jar$(CPSEP).$(CPSEP)$(PLUGINSHOME)/Jama-1.0.2.jar$(CPSEP)$(PLUGINSHOME)/junit-4.4.jar $(JAVACOPTSCOMPAT)

all: $(CLASSES)

show:
	echo $(JAVAS)

TRACERSOURCES=stacks/ThreePanes.java \
	stacks/ThreePanesCanvas.java \
	stacks/PaneOwner.java \
	tracing/SearchNode.java \
	tracing/SearchProgressCallback.java \
	tracing/SearchThread.java \
	tracing/TracerThread.java \
	tracing/Path.java \
	tracing/PathAndFillManager.java \
	tracing/PathAndFillListener.java \
	tracing/Simple_Neurite_Tracer.java \
	tracing/Auto_Tracer.java \
	tracing/AutoSearchThread.java \
	tracing/AutoPoint.java \
	tracing/SinglePathsGraph.java \
	tracing/AutoTracerCanvas.java \
	tracing/NeuriteTracerResultsDialog.java \
	tracing/PointInImage.java \
	tracing/TracerCanvas.java \
	tracing/InteractiveTracerCanvas.java \
	tracing/Fill.java \
	tracing/FillerThread.java \
	tracing/FillerProgressCallback.java \
	tracing/NormalPlaneCanvas.java \
	tracing/TraceLoaderListener.java \
	$(wildcard pal/math/*.java) \
	features/ComputeCurvatures.java \
	features/GaussianGenerationCallback.java \
	features/TubenessProcessor.java \
	features/HessianEvalueProcessor.java \
	client/ArchiveClient.java \
	util/BatchOpener.java \
	util/RGB_to_Luminance.java \
	tracing/README tracing/COPYING \
	math3d/JacobiDouble.java \
	math3d/JacobiFloat.java \
	math3d/FastMatrixN.java \
	math3d/FloatMatrixN.java \
	amira/AmiraParameters.java \
	amira/AmiraMeshDecoder.java \
	amira/AmiraTable.java

test :
	@echo "Run these tests from Fiji instead."
	@echo "( cd ../tests && ./vib-junit-tests.sh )"

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

math3d/Eigensystem3x3Float.java: math3d/Eigensystem3x3Double.java
	sed -e "s,/\\*change\\*/double,float,g" -e "s/Double/Float/g" < $< > $@

math3d/Eigensystem2x2Float.java: math3d/Eigensystem2x2Double.java
	sed -e "s,/\\*change\\*/double,float,g" -e "s/Double/Float/g" < $< > $@

FibonacciHeapInt.java: FibonacciHeap.java Makefile
	sed -e "s/FibonacciHeap/FibonacciHeapInt/g" -e "s/ implements Comparable//" -e "s/Comparable/int/g" -e "s/\.compareTo(\([^)]*\))/- \1/g" -e "s/Object other/int other/g" -e "s/heap.add(p, p);/heap.add((int)prios[i], new Double((int)prios[i]));/" -e "s/Node(null/Node(0/" < $< > $@

VIB_compat.jar: SOURCES=$(filter-out $(FILTEROUT), $(filter-out $(wildcard vib/transforms/*.java vib/oldregistration/*.java landmarks/*.java process3d/*.java tracing/*.java oldsegmenters/*.java client/*.java features/*.java Compute_Curvatures.java), $(JAVAS))) vib/segment/icons/*.png

VIB_.jar: SOURCES=$(filter-out $(FILTEROUT), $(filter-out $(wildcard vib/transforms/*.java vib/oldregistration/*.java landmarks/*.java tracing/*.java oldsegmenters/*.java client/*.java util/Bookstein_FromMarkers.java), $(JAVAS))) vib/segment/icons/*.png $(TRACERSOURCES) $(wildcard vib/segment/materials/*) $(wildcard view4d/icons/*.png)

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
	amira/AmiraTable.java

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
	vib/InterpolatedImage.java \
	math3d/Point3d.java math3d/NormalEstimator.java

ImageJ_3D_Viewer.jar: SOURCES=$(wildcard voltex/*.java)\
	$(wildcard ij3d/*.java) $(wildcard ij3d/shapes/*.java) \
	$(wildcard ij3d/behaviors/*.java) \
	$(wildcard ij3d/pointlist/*.java) \
	$(wildcard marchingcubes/*.java) \
	$(wildcard isosurface/*.java) \
	$(wildcard orthoslice/*.java) \
	$(wildcard javax/media/j3d/*.java) \
	$(wildcard math3d/*.java) \
	$(wildcard view4d/*.java) $(wildcard view4d/icons/*.png) \
	$(wildcard surfaceplot/*.java) \
	$(wildcard octree/*.java) \
	$(wildcard customnode/*.java) \
	$(wildcard process3d/*.java) \
	$(wildcard nrrd/*.java) \
	vib/segment/ImageButton.java vib/segment/Border.java \
	vib/Resample_.java vib/InterpolatedImage.java \
	vib/PointList.java vib/BenesNamedPoint.java \
	amira/AmiraParameters.java amira/AmiraTable.java \
	vib/FastMatrix.java vib/FloatMatrix.java ImageJ_3D_Viewer.java

ImageJ_3D_Viewer.pdf: viewer_paper/paper.tex viewer_paper/bibliography.bib
	cd viewer_paper; \
	latex paper && \
	bibtex paper && \
	latex paper && \
	latex paper && \
	dvips paper.dvi && \
	ps2pdf paper.ps && \
	mv paper.pdf ../ImageJ_3D_Viewer.pdf; \
	rm paper.aux paper.bbl paper.blg paper.log paper.dvi paper.ps

Install_Java3D.jar: SOURCES=Install_Java3D.java

Three_Pane_Crop.jar: SOURCES=stacks/PaneOwner.java \
	stacks/ThreePaneCropCanvas.java \
	stacks/Three_Pane_Crop.java \
	stacks/ThreePaneCrop.java \
	stacks/ThreePanesCanvas.java \
	stacks/ThreePanes.java stacks/COPYING stacks/README

Unpack_To_PNG.jar: SOURCES=stacks/Unpack_To_PNG.java \
	amira/AmiraParameters.java \
	amira/AmiraTable.java \
	util/BatchOpener.java \
	zeiss/LSM_Reader.java

Find_Connected_Regions.jar: SOURCES=util/Find_Connected_Regions.java \
	amira/AmiraParameters.java \
	amira/AmiraTable.java \
	util/COPYING

Mask_Of_Nearby_Points.jar: SOURCES=util/Mask_Of_Nearby_Points.java \
	util/COPYING

Tubeness_.jar: SOURCES=features/Tubeness_.java \
	features/HessianEvalueProcessor.java \
	features/ComputeCurvatures.java \
	features/TubenessProcessor.java \
	features/GaussianGenerationCallback.java \
	math3d/Eigensystem2x2Double.java \
	math3d/Eigensystem2x2Float.java \
	math3d/Eigensystem3x3Double.java \
	math3d/Eigensystem3x3Float.java \
	math3d/JacobiFloat.java \
	math3d/JacobiDouble.java \
	math3d/Point3d.java \
	math3d/FloatMatrixN.java \
	math3d/FastMatrixN.java

Simple_Neurite_Tracer.jar: SOURCES=$(filter-out tracing/Auto_Tracer.java tracing/AutoTraceThread.java tracing/AutoSearchThread.java tracing/AutoTracerCanvas.java,$(TRACERSOURCES))

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

Quantile_Based_Normalization.jar: SOURCES=util/Quantile_Based_Normalization.java \
	vib/app/FileGroup.java \
	vib/app/gui/FileGroupDialog.java \
	util/BatchOpener.java

VIB_Protocol.jar: SOURCES=$(wildcard vib/app/*.java) \
	$(wildcard vib/app/gui/*.java) \
	$(wildcard vib/app/module/*.java) \
	$(wildcard vib/segment/*.java) \
	$(wildcard vib/segment/materials/*) \
	$(wildcard vib/segment/icons/*.png) \
	$(wildcard distance/*.java) \
	$(wildcard leica/*.java) \
	$(wildcard amira/*.java) \
	$(wildcard math3d/*.java) \
	$(wildcard pal/math/*.java) \
	util/BatchOpener.java \
	util/StupidLog.java \
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
	vib/Local_Threshold.java \
	VIB_Protocol.java

Average_Color.jar: SOURCES=Average_Color.java CIELAB.java

Bilateral_Filter.jar: SOURCES=Bilateral_Filter.java \
	vib/InterpolatedImage.java math3d/Point3d.java

SIMPLE_JARS=Two_Point_Correlation.jar Scrollable_StackWindow.jar \
	Align_Image.jar Moving_Least_Squares.jar \
	Seam_Remover.jar Triangle_Algorithm.jar Menu_Font.jar

$(SIMPLE_JARS): SOURCES=$(patsubst %.jar,%.java,$@)

JARS=Delaunay_Voronoi.jar AmiraMesh_.jar AmiraSurface_.jar \
	Rigid_Registration.jar Extract_Surface.jar \
	Segmentation_Editor_compat.jar VIB_compat.jar \
	ImageJ_3D_Viewer.jar Quantile_Based_Normalization.jar \
	Install_Java3D.jar Three_Pane_Crop.jar Unpack_To_PNG.jar \
	Simple_Neurite_Tracer.jar ExportMesh_.jar VIB_Protocol.jar \
	Average_Color.jar Bilateral_Filter.jar \
	$(SIMPLE_JARS)

show-jars:
	echo $(JARS)

all-jars: $(JARS)

clean-jars:
	-rm $(JARS)

%-compat.jar: %.jar
	test ! -d tempdir || rm -rf tempdir
	mkdir tempdir
	(cd tempdir && jar xvf ../$< && find . -name \*.class -exec rm {} \; && sh ../compile1.3.sh && jar cvf ../$@ $$(find . -type f)) && rm -rf tempdir

ifeq ($(TARGET_JAR),)
%.jar: $(JAVAS) Makefile
	@$(MAKE) TARGET_JAR=$@ SOURCES="$(SOURCES)" $@
else
$(TARGET_JAR): $(SOURCES)
	test ! -d tempdir || rm -rf tempdir
	mkdir tempdir
	test ! -f `basename $(TARGET_JAR) .jar`.config || \
		cp `basename $(TARGET_JAR) .jar`.config tempdir/plugins.config
	tar cvf - $(SOURCES) $(EXTRAS) | \
		(cd tempdir; tar xvf -)
	(cd tempdir && \
		javac $(JAVACOPTS) $(filter %.java,$(SOURCES)) && \
		jar cvf ../$@ $$(find . -type f)) && \
	rm -rf tempdir
endif

# Unpack the jar, remove the source files and jar it up again :)

%.jar-without-source: %.jar
	test ! -d tempdir || rm -rf tempdir
	mkdir tempdir
	(cd tempdir && jar xvf ../$< && find . -name '*.java' -exec rm {} \; && jar cvf ../$@ $$(find . -type f)) && rm -rf tempdir

clean:
	find . -name \*.class -exec rm {} \;
	rm -f $(JARS)
