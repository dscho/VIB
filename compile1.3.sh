CP=../ImageJ/ij.jar:jzlib-1.0.7.jar:imagescience.jar 

case "$(uname)" in
CYGWIN*) CP="$(echo $CP | tr \: \;)";;
esac

java5s="adt/Connectivity2D.java adt/Points.java adt/Sparse3DByteArray.java Affine_FromMarkers.java AutoLabeller.java AutoLabellerNaive.java BatchProcessor_.java events/RoiWatcher.java events/SliceWatcher.java gui/GuiBuilder.java LabelBinaryOps.java LabelInterpolator_.java LabelThresholder_.java Name_Points.java OrderedTransformations.java PCA_Registration.java Segmentator_.java Segmenter_.java Utils.java"
for i in $java5s; do
	classfile=$(echo $i | sed "s/java$/class/")
	if [ ! -e $i.five ]; then
		mv $i $i.five
	fi
	cat $i.five | \
	sed -e 's/<[A-Za-z][]A-Za-z[]*>//g' \
		-e 's/<[A-Za-z]*, *[A-Za-z][]A-Za-z[]*>//g' \
		-e 's/<[A-Za-z]*, *[A-Za-z][]A-Za-z[]*>//g' \
		-e 's/<[A-Za-z]*, *[A-Za-z][]A-Za-z[]*>//g' \
		-e 's/<[A-Za-z]*, *[A-Za-z][]A-Za-z[]*>//g' \
		-e 's/<[A-Za-z]*, *[A-Za-z][]A-Za-z[]*>//g' \
		-e 's/<[A-Za-z]*, *[A-Za-z][]A-Za-z[]*>//g' \
		-e 's/for *(Frame  *\([^ ]*\) *:\(.*\)) *{/Frame[] frames\1 = \2; for(int i\1 = 0; i\1 < frames\1.length; i\1++) { Frame \1 = frames\1[i\1];/' \
		-e "s/for *( *\([^ ]*\) \(.*\):\(.*\)) *{/java.util.Iterator iter\2 = \3.iterator();\
                      while (iter\2.hasNext()) {\
                              \1 \2 = (\1)iter\2.next();/" \
		-e 's/Iterable/java.util.Collection/g' \
		-e 's/^[ 	]*assert \(.*\);/if (!(\1)) throw new RuntimeException("assert failed");/' \
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
	> $i
#	if [ $i = gui/GuiBuilder.java ]; then
#		diff -u $i $i.five
#	fi
done

javac -classpath "$CP" -source 1.3 -target 1.3 $(find . -name \*.java)

for i in $java5s; do
	mv $i.five $i
done

