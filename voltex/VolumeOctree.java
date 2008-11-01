package voltex;

import vib.Resample_;

import java.io.File;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;

import ij.io.FileSaver;

import ij.measure.Calibration;

public class VolumeOctree {

	public static final int SIZE = 32;

	private String outdir;
	private ImagePlus imp;

	private Cube root;

	public VolumeOctree(ImagePlus imp) {
		this.imp = imp;
		String basename = imp.getTitle();
		if(basename.toLowerCase().endsWith(".tif"))
			basename = basename.substring(0, basename.length() - 4);
		File dir = new File(System.getProperty("java.io.tmpdir"), basename);
		dir.mkdir();
		outdir = dir.getAbsolutePath();

	}

	public Cube getRoot() {
		return root;
	}

	public void create() {
		int l = createFiles();
		root = new Cube(outdir, 0, 0, 0, l);
		root.createChildren();
	}

	public int createFiles() {
		makePowerOfTwo();
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		int l = 1;

		while(true) {
			for(int z = 0; z < d; z += SIZE) {
				for(int y = 0; y < h; y += SIZE) {
					for(int x = 0; x < w; x += SIZE) {
						ImagePlus tmp = createSubvolume(x, y, z, l);
						new FileSaver(tmp).saveAsTiffStack(outdir + "/" + tmp.getTitle() + ".tif");
					}
				}
			}
			int factorX = w > 32 ? 2 : 1;
			int factorY = h > 32 ? 2 : 1;
			int factorZ = d > 32 ? 2 : 1;
			if(factorX == 1 && factorY == 1 && factorZ == 1)
				break;
			imp = Resample_.resample(imp, factorX, factorY, factorZ);
			w = imp.getWidth();
			h = imp.getHeight();
			d = imp.getStackSize();
			l = l << 1;
		}
		return l;
	}

	private ImagePlus createSubvolume(int x, int y, int z, int l) {
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();

		if(x == w-1 || y == h-1 || z == d-1)
			return null;
		ImageStack newStack = new ImageStack(SIZE, SIZE);
		ImageStack oldStack = imp.getStack();

		for(int zi = 0; zi < SIZE; zi++) {
			byte[] p_old = (byte[])oldStack.getPixels(z + zi + 1);
			byte[] p_new = new byte[SIZE * SIZE];
			for(int yi = 0; yi < SIZE; yi++) {
				System.arraycopy(p_old, (yi + y) * w + x, p_new, yi * SIZE, SIZE);
			}
			newStack.addSlice("", p_new);
		}
		ImagePlus ret = new ImagePlus("" + (x*l) + "_" + (y*l) + "_" + (z*l) + "_" + l, newStack);
		Calibration cal = imp.getCalibration().copy();
		cal.xOrigin = x;
		cal.yOrigin = y;
		cal.zOrigin = z;
		ret.setCalibration(cal);
		return ret;
	}
		

	private void makePowerOfTwo() {
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();

		int wn = nextPow2(w);
		int hn = nextPow2(h);
		int dn = nextPow2(d);

		ImageStack newStack = new ImageStack(w, h);
		ImageStack oldStack = imp.getStack();

		int z = 0;
		for(z = 0; z < d; z++) {
			byte[] p_old = (byte[])oldStack.getPixels(z+1);
			byte[] p_new = new byte[wn * hn];
			for(int y = 0; y < h; y++) {
				System.arraycopy(p_old, y * w, p_new, y * wn, w);
			}
			newStack.addSlice("", p_new);
		}

		// add empty slices
		for(; z < dn; z++) {
			newStack.addSlice("", new ByteProcessor(wn, hn));
		}
		imp.setStack(null,newStack);
	}

	private final int nextPow2(int n) {
		int retval = 2;
		while (retval < n) {
			retval = retval << 1;
		}
		return retval;
	}
}

