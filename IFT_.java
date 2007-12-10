import vib.PointList;
import vib.BenesNamedPoint;

import java.util.Random;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import ij.measure.Calibration;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.plugin.filter.PlugInFilter;

public class IFT_ implements PlugInFilter {
	
	private ImagePlus image;
	private ImagePlus resultIm;
	private PointList markers;
	private int w, h, d;
	private PriorityQueue queue;
	private byte[][] data;
	private byte[][] result;
	private int[] C;
	private boolean[] flag;

	public void run(ImageProcessor ip) {
		init();
		resultIm = createResult();
		resultIm.show();
		propagate();
	}

	public void init() {
		w = image.getWidth();
		h = image.getHeight();
		d = image.getStackSize();
		markers = PointList.load(image);
		data = new byte[d][];
		for(int z = 0; z < d; z++) {
			data[z] = (byte[])image.getStack()
					.getProcessor(z+1).getPixels();
		}
		C = new int[w*h*d];
		flag = new boolean[w*h*d];
		result = new byte[d][w*h];
		for(int i = 0; i < C.length; i++)
			C[i] = 255;
// 			C[i] = Integer.MAX_VALUE;
		queue = new PriorityQueue();
		byte m = 1;
		for(Iterator it = markers.iterator(); it.hasNext();) {
			BenesNamedPoint p = (BenesNamedPoint)it.next();
			int z = (int)p.z;
			int i = (int)p.y * w + (int)p.x;
			int index = z*w*h + i;
			int cost = 0;
			C[index] = cost;
			m += 10;
			result[z][i] = m;
			queue.add(index, cost);
		}
	}

	public void propagate() {
		int wh = w * h;
		int counter = 0;
		while(!queue.isEmpty()) {
			int v = queue.poll();
			flag[v] = true;
			IntArray nei = getNeighbours(v);
			for(int i = 0; i < nei.size(); i++) {
				int p = nei.get(i);
				int pCost = C[p];
// 				int m = Math.max(C[v], weight(v, p));
 				int m = C[v] + weight(v, p);
				if(m < C[p]) {
					C[p] = m;
					int pz = p / wh, pi = p % wh;
					int vz = v / wh, vi = v % wh;
					result[pz][pi] = result[vz][vi];
					queue.removeFromBucket(p, pCost);
					queue.add(p, C[p]);
				}
			}
			counter++;
			if(counter % 100 == 0)
				resultIm.updateAndDraw();
		}
	}

	public ImagePlus createResult() {
		ImageStack stack = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			stack.addSlice("", 
				new ByteProcessor(w, h, result[z], null));
		}
		return new ImagePlus("Result", stack);
	}

 	public int weight(int n1, int n2) {
		int wh = w * h;
		int z1 = n1 / wh; int i1 = n1 % wh;
		int z2 = n2 / wh; int i2 = n2 % wh;

		return (int)Math.abs((int)(data[z1][i1] & 0xff) - 
			(int)(data[z2][i2] & 0xff));
	}

	public int setup(String arg, ImagePlus image) {
		this.image = image;
		return DOES_8G;
	}

	public IntArray getNeighbours(int index) {
		IntArray l = new IntArray(6);
		int wh = w*h;
		int z = index / (wh);
		int s = index % (wh);
		int x = s % w, y = s / w;
		if(z != 0 && !flag[(z-1) * wh + s])
			l.add((z-1) * wh + s);
		if(z != d-1 && !flag[(z+1) * wh + s])
			l.add((z+1) * wh + s);
		if(x != 0 && !flag[z * wh + s-1])
			l.add(z * wh + s-1);
		if(x != w-1 && !flag[z * wh + s+1])
			l.add(z * wh + s+1);
		if(y != 0 && !flag[z * wh + (y-1)*w+x])
			l.add(z * wh + (y-1)*w+x);
		if(y != h-1 && !flag[z * wh + (y+1)*w+x])
			l.add(z * wh + (y+1)*w+x);
		return l;
	}

	private class PriorityQueue {
		private IntArray[] arr = new IntArray[256];

		public PriorityQueue() {}

		public void add(int value, int cost) {
			if(arr[cost] == null)
				arr[cost] = new IntArray(1000);
			arr[cost].add(value);
		}

		public boolean isEmpty() {
			for(int i = 0; i < arr.length; i++)
				if(arr[i] != null && !arr[i].isEmpty())
					return false;
			return true;
		}

		public int poll() {
			for(int i = 0; i < arr.length; i++) {
				if(arr[i] != null && !arr[i].isEmpty()) {
// 					int r = new Random().nextInt(
// 						arr[i].size());
					int r = arr[i].size() - 1;
					int ret = arr[i].get(r);
					arr[i].removeIndex(r);
					return ret;
				}
			}
			return -1;
		}

		public void removeFromBucket(int value, int bucket) {
			if(arr[bucket] != null)
				arr[bucket].removeValue(value);
		}
	}

	private class IntArray {
		private int[] array;
		private int size = 0;
		private int initCap;

		public IntArray(int cap) {
			initCap = cap;
			array = new int[cap];
		}

		public int size() {
			return size;
		}

		public boolean isEmpty() {
			return size == 0;
		}

		public int get(int i) {
			if(i >= 0 && i < size)
				return array[i];
			return -1;
		}

		public void add(int n) {
			if(size == array.length) {
				int additional = initCap / 5;
				additional = additional == 0 ? 1 : additional;
				int[] tmp = array;
				array = new int[array.length + additional];
				System.arraycopy(tmp, 0, array, 0, tmp.length);
			}
			array[size] = n;
			size++;
		}

		public void removeIndex(int i) {
			if(i < 0 || i >= size)
				return;
			size--;
			for(int k = i; k < size; k++) 
				array[k] = array[k+1];
		}

		public void removeValue(int v) {
			int k = 0;
			for(; k < size && array[k] != v; k++)
				;
			if(k == size)
				return;
			size--;
			for(; k < size; k++) 
				array[k] = array[k+1];
		}

		public void set(int index, int value) {
			if(index >= 0 && index < size)
				array[index] = value;
		}
	}
}
