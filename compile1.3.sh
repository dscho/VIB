prefix=.
if [ ! -e $prefix/imagescience.jar ]; then
	prefix=..
fi
CP=$prefix/../ImageJ/ij.jar:$prefix/jzlib-1.0.7.jar:$prefix/Quick3dApplet-1.0.8.jar:$prefix/imagescience.jar 

case "$(uname)" in
CYGWIN*) CP="$(echo $CP | tr \: \;)";;
esac

java5s="adt/Connectivity2D.java adt/Points.java adt/Sparse3DByteArray.java Affine_FromMarkers.java AutoLabeller.java AutoLabellerNaive.java BatchProcessor_.java events/RoiWatcher.java events/SliceWatcher.java Fill_holes.java gui/GuiBuilder.java LabelBinaryOps.java vib/PointList.java vib/BenesName_Points.java vib/LocalRigidRegistration_.java Particle_Analyzer_3D.java marchingcubes/MCCube.java marchingcubes/MCShape.java vib/app/FileGroup.java vib/app/gui/Bubble.java vib/app/Options.java vib/app/gui/dialog/FileGroupDialog.java vib/app/gui/dialog/OptionsDialog.java vib/app/module/Module.java vib/app/module/Load.java vib/Center_Transformation.java ij3d/RoiCanvas3D.java voltex/VolRend.java vib/app/gui/FileGroupDialog.java vib/Show_centers.java isosurface/IsoShape.java marchingcubes/ExportMesh_.java vib/transforms/BoundsInclusive.java vib/transforms/OrderedTransformations.java vib/oldregistration/Bookstein_FromMarkers.java vib/oldregistration/PCA_Registration.java oldsegmenters/LabelInterpolator_.java oldsegmenters/AutoLabeller.java oldsegmenters/Segmenter_.java oldsegmenters/Utils.java oldsegmenters/LabelBinaryOps.java oldsegmenters/Segmentator_.java oldsegmenters/LabelThresholder_.java client/ArchiveClient.java tracing/Connectivity.java tracing/AStarThread.java tracing/AnalyzeTracings_.java tracing/Fill.java tracing/FillerThread.java tracing/CreateTracingVolume_.java tracing/SegmentedConnection.java tracing/PathAndFillManager.java stacks/ThreePaneCrop.java stacks/Unpack_To_PNG.java landmarks/NamedPoint.java landmarks/Name_Points.java vib/app/gui/ProgressIndicator.java vib/app/gui/FileGroupDialog.java vib/transforms/FastMatrixTransform.java"
for i in $java5s; do
	if [ -e $i ]; then
	classfile=$(echo $i | sed "s/java$/class/")
	if [ ! -e $i.five ]; then
		mv $i $i.five
	fi
	cat $i.five | \
	sed \
		-e '24s/implements Iterable<BenesNamedPoint>//' \
		-e '24s/implements Iterable<NamedPoint>//' \
		-e 's/<[A-Za-z][]A-Za-z3[]*>//g' \
		-e 's/< [A-Za-z][]A-Za-z3[]* \[\] >//g' \
		-e 's/< [A-Za-z][]A-Za-z3[]* >//g' \
		-e 's/<[A-Za-z]*, *[A-Za-z][]A-Za-z[]*>//g' \
		-e 's/< [A-Za-z]*, *[A-Za-z][]A-Za-z[]* >//g' \
		-e 's/< [A-Za-z]*, *[A-Za-z][]A-Za-z[]* \[\] >//g' \
		-e 's/<[A-Za-z]*, *[A-Za-z][]A-Za-z[]*>//g' \
		-e 's/<[A-Za-z]*, *[A-Za-z][]A-Za-z[]*>//g' \
		-e 's/<[A-Za-z]*, *[A-Za-z][]A-Za-z[]*>//g' \
		-e 's/for *(Frame  *\([^ ]*\) *:\(.*\)) *{/Frame[] frames\1 = \2; for(int i\1 = 0; i\1 < frames\1.length; i\1++) { Frame \1 = frames\1[i\1];/' \
		-e "s/for *( *\([^ ]*\) \(.*\): *([^)]*)\(.*\)) *{/java.util.Iterator iter\2 = \3.iterator();\
                      while (iter\2.hasNext()) {\
                              \1 \2 = (\1)iter\2.next();/" \
		-e "s/for *( *\([^ ]*\) \(.*\):\(.*\)) *{/java.util.Iterator iter\2 = \3.iterator();\
                      while (iter\2.hasNext()) {\
                              \1 \2 = (\1)iter\2.next();/" \
		-e 's/Iterable/java.util.Collection/g' \
		-e 's/^[ 	]*assert *\(.*\);/if (!(\1)) throw new RuntimeException("assert failed");/' \
		-e '32s/\(data\.get(\)\([^)]*\)/(HashMap)\1new Integer(\2)/g' \
		-e '49s/\(data\.get(\)\([^)]*\)/(HashMap)\1new Integer(\2)/g' \
		-e 's/\( zDim\.get(\)\([^)]*\)/(Byte)\1new Integer(\2)/g' \
		-e 's/\(yzDim\.get(\)\([^)]*\)/(HashMap)\1new Integer(\2)/g' \
		-e 's/\(data\.put(\)\([^,]*\),/\1new Integer(\2),/g' \
		-e 's/\(zDim\.put(\)\([^,]*\), val/\1new Integer(\2), new Byte(val)/g' \
		-e 's/\(yzDim\.put(\)\([^,]*\),/\1new Integer(\2),/g' \
		-e '64s/return val/return val.byteValue()/' \
		-e 's/\(stats\.put(\)\(labelStats\.id\)/\1new Byte(\2)/' \
		-e 's/\(LabelStats.*=[	 ]*\)\(stats\.get(\)\([^)]*\)/\1(LabelStats)\2new Byte(\3)/g' \
		-e 's/\(materialNames.put(\)\([^,]*\)/\1new Byte(\2)/' \
		-e 's/points[01]\.get(choice/(NamedPoint)&/' \
		-e 's/materialNames\.get/(String)&/' \
		-e 's/materialPixels\.get/(byte[][])&/' \
		-e 's/materialPixelStore\.get/(byte[][])&/' \
		-e 's/statsStore\.get/(PixelStats)&/' \
		-e 's/materialStats\.get/(PixelStats)&/' \
		-e 's/\(dilateOffset\.add(\)\([^)]*\)/\1new Integer(\2)/' \
		-e 's/pixelData\[offset\] = id2/&.intValue()/' \
		-e 's/\(erodeOffsets\.add(\)\([^)]*\)/\1new Integer(\2)/' \
		-e 's/pixelData\[erodeOffset/&.intValue()/' \
		-e 's/pixelData\[errodeOffset/&.intValue()/' \
		-e 's/\(labelledSlices\.add(\)\([^)]*\)/\1new Integer(\2)/' \
		-e 's/labelledSlices.get([^)]*)/((Integer)&).intValue()/' \
		-e 's/pixelData\[offset/&.intValue()/' \
		-e 's/out\.append/out.write/' \
		-e 's/\(memory.put(new Point(x, y), \)\(data.get(x, y)\)/\1new Integer(\2)/' \
		-e 's/\(data.set(p.x, p.y, \)\(memory.get(p)\)/\1((Integer)\2).intValue()/' \
		-e 's/\(int\[\].*=\)\(.*clone();\)/\1(int[])\2/' \
		-e 's/\(double .*= *\)\((Double)[^;]*\)/\1(\2).doubleValue()/' \
		-e 's/\(boxedEigenValues\[i\] = \)\([^;]*\)/\1new Double(\2)/' \
		-e 's/= boxedEigenValues\[i\]/&.doubleValue()/' \
		-e 's/\(double *\[\].*=\)\(.*clone();\)/\1(double[])\2/' \
		-e 's/this.vectors\[j\] = /&(double[])/' \
		-e 's/this.meanXYZ = /&(double[])/' \
		-e 's/public Point next() {/public Object next() {/' \
		-e 's/spiral\.next()/(Point)&/' \
		-e 's/\(pathData\.add(\)\(.*\)\();\)/\1new Float(\2)\3/' \
		-e 's/\(floatRepresentation\[.*=\)\([^;]*\)/\1((Float)\2).floatValue()/' \
		-e '212s/\(c\)\(.add(panel)\)/((JFrame)\1).getContentPane()\2/' \
		-e '219s/\(c\)\(.add(panel)\)/((JFrame)\1).getContentPane()\2/' \
		-e '146s/this\.add(b)/((JFrame)this).getContentPane().add(b)/' \
		-e '58s/c\.add(b)/((JFrame)c).getContentPane().add(b)/' \
		-e '67s/c\.add(b)/((JFrame)c).getContentPane().add(b)/' \
		-e '84s/c\.add(p)/((JFrame)c).getContentPane().add(p)/' \
		-e '180s/container\.add(box)/((JFrame)container).getContentPane().add(box)/' \
		-e 's/[^ ]*children(\?)\?\.get(i)/((RoiNode)(&))/' \
		-e '48s/BenesNamedPoint/Object/g' \
		-e '48s/NamedPoint/Object/g' \
		-e '45s/points.get/(BenesNamedPoint)&/g' \
		-e '107s/aw.setCenter(commonPoints.toArray());/Object[] oa = commonPoints.toArray(); math3d.Point3d[] pa = new math3d.Point3d[oa.length]; for (int i = 0; i < pa.length; i++) pa[i] = (math3d.Point3d)oa[i]; aw.setCenter(pa);/' \
		-e '246s/List<Set>/List/g' \
		-e '302s/for(Integer i : set)/java.util.Iterator iter = set.iterator(); Integer i; while(iter.hasNext() \&\& (i = (Integer)iter.next()) != null) /' \
		-e '184s/\(map.get(\)\(res_pixels\[i\]\))/((Integer)\1new Integer(\2))).intValue()/' \
		-e '251s/\(newset.add(\)\(n_entries\)/\1new Integer(\2)/' \
		-e '271s/classes.get/(Set)&/' \
		-e '278s/contains(n)/contains(new Integer(n))/' \
		-e '288s/\(map.put(\)\(.*\),\(.*\));/\1new Integer(\2), new Integer(\3));/' \
		-e '295s/classes.get/(Set)&/' \
		-e 's/public Options clone/public Object clone/' \
		-e 's/public FileGroup clone/public Object clone/' \
		-e '35s/files.remove(/(File)&/' \
		-e '53s/files.get(/(File)&/'  \
		-e '58s/files.get(i)/((File)&)/' \
		-e '59s/files.get(/(File)&/' \
		-e '85s/files.get(i)/((File)&)/' \
		-e '31s/public ImagePlus execute/public Object execute/' \
		-e 's/messageReceiver.get(i)/((MessageReceiver)&)/' \
		-e '54s/o.clone/(Options)&/' \
		-e '121s/origOptions.clone/(Options)&/' \
		-e '57s/fg.clone/(FileGroup)&/' \
		-e '65s/public VIBImage execute/public Object execute/' \
		-e '70s/ String.class, String.class/ new Class[] { String.class, String.class }/' \
		-e '72s/m.invoke(this, dir, file)/m.invoke(this, new Object[] { dir, file })/' \
		-e '57s/fg.clone/(FileGroup)&/' \
		-e '78s/img, o/new Object[] {&}/' \
		-e '98,99s/points\.get/(Point)&/' \
		-e '117s/points\.get/(Point)&/' \
		-e '78s/renderer\.get/(Renderer)&/' \
		-e '120s/files.get([^)]*)/((File)&)/' \
		-e '157s/files.get([^)]*)/((File)&)/' \
		-e '159s/files.get([^)]*)/((File)&)/' \
		-e '24s/get([^)]*)/((File)&)/' \
		-e '30s/get([^)]*)/((File)&)/' \
		-e '48s/\(get([^)]*)\)\(\.getAbsolute\)/((File)\1)\2/' \
		-e '90s/\(new ColorInterpolator\)/(Interpolator)\1/' \
		-e '91s/\(new BilinearInterpolator\)/(Interpolator)\1/' \
		-e '144s/<Point3f>//g' \
		-e '41s/options.fileGroup.get(i)/((File)&)/' \
		-e '118s/files.get(/(File)&/' \
		-e '156s/files.get(i)/((File)&)/' \
		-e '158s/files.get(i)/((File)&)/' \
		-e '29s/pointsInTemplate.get(/(NamedPoint)&/' \
		-e '56s/public FastMatrixTransform inverse/public Transform inverse/' \
		-e '62s/return result/return (Transform)result/' \
	> $i
	fi
done

javac -classpath "$CP" -source 1.3 -target 1.3 $(find . -name \*.java)

result=$?

for i in $java5s; do
	if [ -e $i.five ]; then
		mv $i.five $i
	fi
done

exit $result
