javaVersion=1.5
all <- remove-tempdir VIB_.jar

CLASSPATH=$CLASSPATH:junit-4.4.jar:.
VIB_.jar <- **/*.java \
	vib/FloatMatrix.java math3d/FloatMatrixN.java FibonacciHeapInt.java \
	math3d/JacobiFloat.java math3d/Eigensystem3x3Float.java \
	math3d/Eigensystem2x2Float.java \
	vib/segment/materials/* \
	vib/segment/icons/*.png view4d/icons/*.png

# clean up from "make"

remove-tempdir[rm.py tempdir] <-

# pre-Java5 generics ;-)

vib/FloatMatrix.java[sed.py $PRE $TARGET] <- vib/FastMatrix.java
math3d/FloatMatrixN.java[sed.py $PRE $TARGET] <- math3d/FastMatrixN.java
math3d/JacobiFloat.java[sed.py $PRE $TARGET] <- math3d/JacobiDouble.java
math3d/Eigensystem3x3Float.java[sed.py $PRE $TARGET] <- \
	math3d/Eigensystem3x3Double.java
math3d/Eigensystem2x2Float.java[sed.py $PRE $TARGET] <- \
	math3d/Eigensystem2x2Double.java
FibonacciHeapInt.java[sed.py $PRE $TARGET] <- FibonacciHeap.java

# single-plugin jars

all-jars[] <- simple_jars jars

simple_jars[] <- Two_Point_Correlation.jar Scrollable_StackWindow.jar \
	Align_Image.jar Moving_Least_Squares.jar Install_Java3D.jar \
	Seam_Remover.jar Triangle_Algorithm.jar Menu_Font.jar \
	Thread_Killer.jar

*.jar <- *.java

jars[] <- Delaunay_Voronoi.jar AmiraMesh_.jar AmiraSurface_.jar \
	Rigid_Registration.jar Extract_Surface.jar \
	Segmentation_Editor.jar \
	ImageJ_3D_Viewer.jar Quantile_Based_Normalization.jar \
	Three_Pane_Crop.jar Unpack_To_PNG.jar \
	Simple_Neurite_Tracer.jar ExportMesh_.jar VIB_Protocol.jar \
	Average_Color.jar Bilateral_Filter.jar \
	Mask_Of_Nearby_Points.jar Find_Connected_Regions.jar \
	Tubeness_.jar Lasso_.jar

JAVAVERSION(Segmentation_Editor.jar)=1.3
SOURCES(Segmentation_Editor.jar)=amira/*.java \
	vib/InterpolatedImage.java math3d/Point3d.java \
	vib/SegmentationViewerCanvas.java vib/segment/*.java \
	vib/IDT_Interpolate_Binary.java math3d/FastMatrixN.java \
	Segmentation_Editor.java vib/segment/icons/*.png \
	vib/segment/materials/* vib/Local_Threshold.java
SOURCES(Delaunay_Voronoi.jar)=Delaunay_Voronoi.java delaunay/*.java
SOURCES(AmiraMesh_.jar)=AmiraMeshReader_.java AmiraMeshWriter_.java \
	amira/AmiraParameters.java amira/AmiraMeshEncoder.java \
	amira/AmiraMeshDecoder.java amira/AmiraTableEncoder.java \
	amira/AmiraTable.java
SOURCES(AmiraSurface_.jar)=ShowAmiraSurface_.java amira/AmiraParameters.java \
	amira/AmiraTable.java
SOURCES(Rigid_Registration.jar)=vib/RigidRegistration_.java \
	vib/InterpolatedImage.java vib/TransformedImage.java \
	vib/FastMatrix.java vib/RegistrationOptimizer.java \
	amira/AmiraParameters.java amira/AmiraTable.java vib/VIB.java \
	vib/BatchLog_.java vib/Resample_.java vib/FloatMatrix.java \
	distance/PixelPairs.java distance/Euclidean.java \
	distance/TwoValues.java distance/MutualInformation.java \
	distance/Thresholded.java distance/Correlation.java \
	pal/math/*.java \
	math3d/Point3d.java math3d/Triangle.java math3d/Line.java \
	math3d/Plane.java math3d/JacobiDouble.java math3d/FastMatrixN.java \
	math3d/JacobiFloat.java math3d/FloatMatrixN.java \
	util/StupidLog.java
SOURCES(Extract_Surface.jar)=vib/ArrayBase.java vib/IntArray.java \
	vib/DoubleArray.java vib/Extract_Surface.java \
	vib/InterpolatedImage.java \
	math3d/Point3d.java math3d/NormalEstimator.java
SOURCES(ImageJ_3D_Viewer.jar)=ij3d/*.java voltex/*.java\
	marchingcubes/*.java isosurface/*.java \
	orthoslice/*.java \
	javax/media/j3d/*.java \
	math3d/*.java \
	view4d/*.java view4d/icons/*.png \
	surfaceplot/*.java \
	vib/segment/ImageButton.java vib/segment/Border.java \
	vib/Resample_.java vib/InterpolatedImage.java \
	vib/PointList.java vib/BenesNamedPoint.java \
	amira/AmiraParameters.java amira/AmiraTable.java \
	vib/FastMatrix.java vib/FloatMatrix.java ImageJ_3D_Viewer.java
SOURCES(Three_Pane_Crop.jar)=stacks/PaneOwner.java \
	stacks/ThreePaneCropCanvas.java \
	stacks/Three_Pane_Crop.java \
	stacks/ThreePaneCrop.java \
	stacks/ThreePanesCanvas.java \
	stacks/ThreePanes.java stacks/COPYING stacks/README
SOURCES(Unpack_To_PNG.jar)=stacks/Unpack_To_PNG.java \
	amira/AmiraParameters.java \
	amira/AmiraTable.java \
	util/BatchOpener.java \
	zeiss/LSM_Reader.java
SOURCES(Find_Connected_Regions.jar)=util/Find_Connected_Regions.java \
	amira/AmiraParameters.java \
	amira/AmiraTable.java
SOURCES(Mask_Of_Nearby_Points.jar)=util/Mask_Of_Nearby_Points.java
SOURCES(Tubeness_.jar)=features/Tubeness_.java \
	features/HessianEvalueProcessor.java \
	features/ComputeCurvatures.java \
	features/TubenessProcessor.java \
	features/GaussianGenerationCallback.java \
	math3d/JacobiFloat.java \
	math3d/JacobiDouble.java \
	math3d/Point3d.java \
	math3d/FloatMatrixN.java \
	math3d/FastMatrixN.java
SOURCES(Simple_Neurite_Tracer.jar)=stacks/ThreePanes.java \
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
	tracing/AutoPoint.java \
	tracing/SinglePathsGraph.java \
	tracing/NeuriteTracerResultsDialog.java \
	tracing/PointInImage.java \
	tracing/TracerCanvas.java \
	tracing/InteractiveTracerCanvas.java \
	tracing/Fill.java \
	tracing/FillerThread.java \
	tracing/FillerProgressCallback.java \
	tracing/NormalPlaneCanvas.java \
	tracing/TraceLoaderListener.java \
	pal/math/*.java \
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
SOURCES(ExportMesh_.jar)=marchingcubes/ExportMesh_.java \
	marchingcubes/MCTriangulator.java \
	marchingcubes/MCCube.java \
	vib/Resample_.java \
	amira/AmiraParameters.java \
	amira/AmiraTable.java \
	ij3d/ColorTable.java \
	isosurface/Triangulator.java \
	vib/InterpolatedImage.java \
	math3d/Point3d.java
SOURCES(Quantile_Based_Normalization.jar)=\
	util/Quantile_Based_Normalization.java \
	vib/app/FileGroup.java \
	vib/app/gui/FileGroupDialog.java \
	util/BatchOpener.java
SOURCES(VIB_Protocol.jar)=vib/app/*.java \
	vib/app/gui/*.java \
	vib/app/module/*.java \
	vib/segment/*.java \
	vib/segment/materials/* \
	vib/segment/icons/*.png \
	distance/*.java \
	leica/*.java \
	amira/*.java \
	math3d/*.java \
	pal/math/*.java \
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
SOURCES(Average_Color.jar)=Average_Color.java CIELAB.java
SOURCES(Bilateral_Filter.jar)=Bilateral_Filter.java \
	vib/InterpolatedImage.java math3d/Point3d.java
SOURCES(Lasso_.jar)=Lasso_.java FibonacciHeapDouble.java

*.jar <- $SOURCES
