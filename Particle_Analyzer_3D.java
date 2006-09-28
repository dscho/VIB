import ij.ImageStack;
import ij.ImagePlus;
import ij.IJ;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class Particle_Analyzer_3D {

	private ImagePlus image;
	private ImagePlus result;
	private int[] classes;
	private int[] sizes;


	private int w,h,z;
	private byte FG = (byte)255;
	private byte BG = (byte)0;
	private boolean showStatus = true;

	/**
	 * Constructor
	 */
	public Particle_Analyzer_3D(ImagePlus image, byte fg, byte bg, boolean sS){
		this.image = image;
		this.w = image.getWidth();
		this.h = image.getHeight();
		this.z = image.getStackSize();
		this.result = classify(image);
		this.showStatus = sS;
	}

	public Particle_Analyzer_3D(ImagePlus image, boolean showStatus){
		this(image, (byte)255, (byte)0, showStatus);
	}

	/**
	 * Returns an array of ints, where each 
	 * int is one class label.
	 * @return classes
	 */
	public int[] getClasses(){
		return classes;
	}
	
	/**
	 * Returns an array which contains the number of 
	 * pixels for each class. The order in the array 
	 * is the same as in the array returned by getClasses().
	 * @return size of the classes
	 */
	public int[] sizes(){
		return sizes;
	}

	/**
	 * Returns the number of classes (excluding the 
	 * 'background' class)
	 * @return number of classes
	 */
	public int classesCount(){
		return classes.length;
	}

	/**
	 * Returns an image whose pixel values are the class
	 * labels.
	 * @return class image
	 */
	public ImagePlus classesAsImage(){
		return result;
	}

	/**
	 * Returns the part of the original picture which
	 * belongs to the specified class
	 */
	public ImagePlus imageForClass(int classlabel, String label){
		ImageStack stack = new ImageStack(w,h);
		for(int d=1;d<=z;d++){
			int[] result_pixels = (int[])result.getStack().getProcessor(d).
				getPixels();
			byte[] new_pixels = new byte[result_pixels.length];
			for(int i=0;i<result_pixels.length;i++){
				new_pixels[i] = result_pixels[i] == classlabel ? FG : BG;
			}
			stack.addSlice("", new ByteProcessor(w,h,new_pixels,null));
		}
		if(label == null || label.trim().equals(""))
			label = "Class " + classlabel;
		return new ImagePlus(label, stack);
	}

	/**
	 * Returns the size of the specified class
	 */
	public int getSize(int classlabel){
		for(int i=0;i<classes.length;i++){
			if(classes[i] == classlabel)
				return sizes[i];
		}
		return -1;
	}

	/**
	 * Returns the label of the largest class
	 */
	public int getLargestClass(){
		int index = -1, maxSize = -1;
		for(int i=0;i<classes.length;i++){
			if(sizes[i] > maxSize){
				maxSize = sizes[i];
				index = i;
			}
		}
		return classes[index];
	}

	/**
	 * Returns the image of the largest class
	 */
	public ImagePlus imageOfLargestClass(){
		return imageForClass(getLargestClass(), "Largest object");
	}
	
	/**
	 * Creates a new ImagePlus which contains at each pixel location 
	 * a byte value which indicates to which class this location 
	 * belongs to. Background class has integer -1. The other classes start 
	 * with integer 0 and continue in ascending order (0, 1, 2, ...)
	 * 
	 * @param image ImagePlus binary image
	 * @return result classesAsImage
	 */
	private ImagePlus classify(ImagePlus image){
		if(showStatus)
			IJ.showStatus("classify...");
		MergedClasses mergedClasses = new MergedClasses();
		ImageStack resStack = new ImageStack(w,h);
		for(int d=1;d<=z;d++){
			byte[] pixels = 
				(byte[])image.getStack().getProcessor(d).getPixels();
			int[] classes = new int[pixels.length];
			int[] classesBefore = d > 1 ? 
				(int[])resStack.getProcessor(d-1).getPixels() : null;
			for(int i=0;i<h;i++){
				for(int j=0;j<w;j++){
					int index = i*w+j;
					byte current = pixels[index];
					int upper_c   = i > 0 ? classes[index-w] : -1;
					int left_c    = j > 0 ? classes[index-1] : -1;
					int before_c  = d > 1 ? classesBefore[index] : -1;
					classes[index] = classifyPixel(mergedClasses,
							current, upper_c, left_c, before_c);
				}
			}
			if(showStatus)
				IJ.showProgress(d,z);
			resStack.addSlice("",new ColorProcessor(w,h,classes));
		}
		correctMergedClasses(resStack,mergedClasses);
		calculateSizes(resStack);
		return new ImagePlus("Classified", resStack);
	}
		
	private void correctMergedClasses(ImageStack resStack, 
			MergedClasses mergedClasses){

		if(showStatus)
			IJ.showStatus("correct merged classes...");
		Map<Integer,Integer> map = mergedClasses.mapToRealClasses();
		for(int d=1;d<=z;d++){
			int[] res_pixels = (int[])resStack.getProcessor(d).getPixels();
			for(int i=0;i<res_pixels.length;i++){
				if(res_pixels[i] != -1){
					int realClass = map.get(res_pixels[i]);
					res_pixels[i] = realClass;
				}
			}
			if(showStatus)
				IJ.showProgress(d,z);
		}
		int n_classes = mergedClasses.n_entries;
		classes = new int[n_classes];
		for(int i=0;i<n_classes;i++)
			classes[i] = i;
	}

	private void calculateSizes(ImageStack resStack){
		if(showStatus)
			IJ.showStatus("calculate class sizes...");
		sizes = new int[classes.length];
		for(int d=1;d<=z;d++){
			int[] classPixels = (int[])resStack.getProcessor(d).getPixels();
			for(int i=0;i<h;i++){
				for(int j=0;j<w;j++){
					int index = i*w + j;
					if(classPixels[index] != -1)
						sizes[classPixels[index]]++;
				}
			}
			if(showStatus)
				IJ.showProgress(d,z);
		}
	}

	private int max(int[] array){
		int max = array[0];
		for(int i=1;i<array.length;i++)
			if(array[i] > max)
				max = array[i];
		return max;
	}
			
	private int classifyPixel(MergedClasses mergedClasses, byte cur, 
			int upper_c, int left_c, int before_c){
		if(cur != BG && cur != FG)
			IJ.error("Image is not binary. Abort.");
		if(cur == BG)
			return -1;
		boolean connected = (upper_c != -1 || left_c != -1 || before_c != -1);
		int classl = -1;
		if(connected){
			classl = Math.max( Math.max(upper_c,left_c),before_c);
			if(upper_c != classl && upper_c != -1)
				mergedClasses.mergeIfNecessary(upper_c,classl);
			if(left_c != classl && left_c != -1)
				mergedClasses.mergeIfNecessary(left_c,classl);
			if(before_c != classl && before_c != -1)
				mergedClasses.mergeIfNecessary(before_c,classl);
		} else {
			classl = mergedClasses.addNewClass();
		}
		return classl;
	}

	private class MergedClasses{
		private List<Set<Integer>> classes = new ArrayList<Set<Integer>>();
		private int n_entries = 0;
		
		int addNewClass(){
			Set<Integer> newset = new HashSet<Integer>();
			newset.add(n_entries);
			classes.add(newset);
			n_entries++;
			return n_entries-1;
		}

		void mergeIfNecessary(int a, int b){
			Set<Integer> aSet = getClassWhichContains(a);
			Set<Integer> bSet = getClassWhichContains(b);
			if(aSet == null || bSet == null)
				IJ.error("Expected that both classes a and b already exist");
			if(aSet == bSet)
				return;
			aSet.addAll(bSet);
			classes.remove(bSet);
		}

		Set<Integer> getClassWhichContains(int n){
			int index = getClassIndexWhichContains(n);
			if(index != -1)
				return classes.get(index);
			return null;
		}

		int getClassIndexWhichContains(int n){
			int i = 0;
			for(Set<Integer> set : classes){
				if(set.contains(n))
					return i;
				i++;
			}
			return -1;
		}

		Map<Integer,Integer> mapToRealClasses(){
			Map<Integer,Integer> map = new HashMap<Integer,Integer>();
			for(int i=0;i<n_entries;i++){
				map.put(i,getClassIndexWhichContains(i));
			}
			return map;
		}

		void print(){
			for(int i=0;i<classes.size();i++){
				Set<Integer> set = classes.get(i);
				System.out.println(i + " --> [" + asString(set) + "]");
			}
		}

		String asString(Set<Integer> set){
			StringBuffer buf = new StringBuffer();
			for(Integer i : set)
				buf.append(i + "  ");
			return buf.toString();
		}
	}
}

