package octree;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import java.io.File;
import java.io.FileWriter;
import java.util.Properties;
import vib.Resample_;

public class FilePreparer {

	private ImagePlus image;
	private int size;
	private String outdir;
	private Resample_.Averager accu = new Resample_.Averager();

	public FilePreparer(String imagePath, int size, String outdir) {
		this(IJ.openImage(imagePath), size, outdir);
	}

	public FilePreparer(ImagePlus image, int size, String outdir) {
		this.image = image;
		this.size = size;
		this.outdir = outdir;
	}

	public int createFiles() throws Exception {
		System.out.println("create Files");
		int w = nextPow2(image.getWidth());
		int h = nextPow2(image.getHeight());
		int d = nextPow2(image.getStackSize());
		System.out.println("w = " + w);
		System.out.println("h = " + h);
		System.out.println("d = " + d);
		int l = 1;
		Calibration c = image.getCalibration().copy();

		int wOrig = w, hOrig = h, dOrig = d;

		while(true) {
			for(int z = 0; z < d; z += size) {
				byte[][] pixels = new byte[size][];
				for(int zi = 0; zi < size; zi++) {
					if(z + zi < image.getStackSize())
						pixels[zi] = (byte[])image.getStack().getPixels(z + zi + 1); 
					else
						pixels[zi] = new byte[image.getWidth() * image.getHeight()];
				}
				System.out.println("create cubes for z = " + z);

				for(int y = 0; y < h; y += size) {
					for(int x = 0; x < w; x += size) {
						String path = outdir + "/" + (x*l) + "_" + (y*l) + "_" + (z*l) + "_" + l + ".tif";
						if(new File(path).exists())
							continue;
						ImagePlus im = createSubvolume(pixels, x, y);
						saveCube(im, path);
					}
				}
			}
			int factorX = w > size ? 2 : 1;
			int factorY = h > size ? 2 : 1;
			int factorZ = d > size ? 2 : 1;
			if(factorX == 1 && factorY == 1 && factorZ == 1)
				break;
			image = resample(image, factorX, factorY, factorZ, accu);
			image.show();
			w = image.getWidth();
			h = image.getHeight();
			d = image.getStackSize();
			l = l << 1;
		}
		writeProperties(wOrig, hOrig, dOrig, l, c);
		System.out.println("Finished create files");
		return l;
	}

	private void saveCube(ImagePlus image, String path) throws Exception {
		byte[][] data = new byte[size][];
		ImageStack stack = image.getStack();
		Calibration cal = image.getCalibration();
		for(int z = 0; z < size; z++)
			data[z] = (byte[])stack.getPixels(z+1);
		CubeData.writeZData(path, data, 
			(float)cal.pixelWidth,
			(float)cal.pixelHeight,
			(float)cal.pixelDepth);
	}

	private void writeProperties(int w, int h, int d, int l, Calibration c) throws Exception {
		Properties props = new Properties();
		props.setProperty("width", Integer.toString(w));
		props.setProperty("height", Integer.toString(h));
		props.setProperty("depth", Integer.toString(d));
		props.setProperty("level", Integer.toString(l));
		props.setProperty("pixelWidth", Float.toString((float)c.pixelWidth));
		props.setProperty("pixelHeight", Float.toString((float)c.pixelHeight));
		props.setProperty("pixelDepth", Float.toString((float)c.pixelDepth));
		FileWriter fw = new FileWriter(new File(outdir, "props.txt"));
		props.store(fw, "octree");
	}

	private ImagePlus createSubvolume(byte[][] pixels, int x, int y) {
		int w = image.getWidth(), h = image.getHeight();
		int d = image.getStackSize();

		ImageStack newStack = new ImageStack(size, size);

		for(int zi = 0; zi < size; zi++) {
			byte[] p_old = pixels[zi];
			byte[] p_new = new byte[size * size];
			for(int yi = 0; yi < size; yi++) {
				for(int xi = 0; xi < size; xi++) {
					if(x + xi >= w || y + yi >= h)
						p_new[yi * size + xi] = (byte)0;
					else
						p_new[yi * size + xi] = p_old[(yi + y) * w + (xi + x)];
				}
			}
			newStack.addSlice("", p_new);
		}
		ImagePlus ret = new ImagePlus("", newStack);
		ret.setCalibration(image.getCalibration());
		return ret;
	}
		
	private final int nextPow2(int n) {
		int retval = 2;
		while (retval < n) {
			retval = retval << 1;
		}
		return retval;
	}

	public ImagePlus resample(ImagePlus image, int factorX, int factorY,
			int factorZ, Resample_.Averager accu) {

		int type = image.getType();
		if(type != ImagePlus.GRAY8)
			throw new RuntimeException("Only 8bit images supported at the moment");
		ImageStack stack = image.getStack();
		int w = image.getWidth(), h = image.getHeight(), d = stack.getSize();
		int ws = nextPow2(w), hs = nextPow2(h), ds = nextPow2(d);
		int wn = ws / 2, hn = hs / 2, dn = ds / 2;

		ImageStack result = new ImageStack(wn, hn);

		for(int z = 0; z < ds; z += factorZ) {

			byte[][] slices = new byte[factorZ][];
			for(int k = 0; k < factorZ; k++) {
				if(z + k < d)
					slices[k] = (byte[])stack.getPixels(z + k + 1);
				else
					slices[k] = new byte[w * h];
			}

			byte[] newSlice = new byte[wn * hn];

			for(int y = 0; y < hs; y += factorY) {
				for(int x = 0; x < ws; x += factorX) {
					accu.reset();
					for(int i = 0; i < factorX; i++) {
						int ix = x + i;
						for(int j = 0; j < factorY; j++) {
							int iy = y + j;
							for(int k = 0; k < factorZ; k++) {
								if(ix >= w || iy >= h) accu.add(0);
								else accu.add((int)(slices[k][iy * w + ix]&0xff));
							}
						}
					}
					newSlice[(x / factorX) + wn * (y / factorY)] = (byte)accu.get();
				}
				IJ.showProgress(z * hs + y + 1, hs*ds);
			}
			result.addSlice(null,newSlice);
			
		}

		ImagePlus res = new ImagePlus(image.getTitle()+" resampled",
				result);

		Calibration cal = image.getCalibration().copy();
		cal.pixelWidth  *= factorX;
		cal.pixelHeight *= factorY;
		cal.pixelDepth  *= factorZ;
		res.setCalibration(cal);

		return res;
	}
}
