import ij.gui.GenericDialog;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;
import ij.ImagePlus;
import ij.IJ;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.plugin.filter.PlugInFilter;

// TODO take calibration into account

public class Distance_Transform_3D implements PlugInFilter {

	private static final float SQ1 = 1f;
	private static final float SQ2 = (float)Math.sqrt(2);
	private static final float SQ3 = (float)Math.sqrt(3);

	private final float[] ML = {SQ3, SQ2, SQ3, SQ2, SQ1, SQ2, SQ3, SQ2, SQ3,
		SQ2, SQ1, SQ2, SQ1};
	private final float[] MR = {SQ1, SQ2, SQ1, SQ2,
		SQ3, SQ2, SQ3, SQ2, SQ1, SQ2, SQ3, SQ2, SQ3};

	private ImagePlus image;
	private int w, h, d;
	private byte[][] pixels;
	private float[][] dist;
	private float maxVal;

	public void run(ImageProcessor ip) {
		init();
		forwardPass();
		backwardPass();
		toFloat().show();
	}

	public void init() {
		w = image.getWidth(); h = image.getHeight();
		d = image.getStackSize();
		maxVal = (float)Math.sqrt(w*w + h*h + d*d);
		dist = new float[d][w*h];
		pixels = new byte[d][];
		for(int z = 0; z < d; z++) {
			pixels[z] = (byte[])image.getStack()
					.getProcessor(z+1).getPixels();
			for(int i = 0; i < w*h; i++) {
				if((int)(pixels[z][i]&0xff) == 255)
					dist[z][i] = 0;
				else
					dist[z][i] = maxVal;
			}
		}
	}

	private float dist(int x, int y, int z) {
		if(x < 0 || x >= w || y < 0 || y >= h || z < 0 || z >= d) {
			return  -maxVal;
		} else {
			return dist[z][y*w+x];
		}
	}

	public void forwardPass() {
		float[] D = new float[13];
		for(int z = 0; z < d; z++) {
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					if(dist[z][y*w+x] == 0) {
						continue;
					}
					D[0]  = ML[0]  + dist(x-1, y-1, z-1);
					D[1]  = ML[1]  + dist(x,   y-1, z-1);
					D[2]  = ML[2]  + dist(x+1, y-1, z-1);
					D[3]  = ML[3]  + dist(x-1, y,   z-1);
					D[4]  = ML[4]  + dist(x,   y,   z-1);
					D[5]  = ML[5]  + dist(x+1, y,   z-1);
					D[6]  = ML[6]  + dist(x-1, y+1, z-1);
					D[7]  = ML[7]  + dist(x,   y+1, z-1);
					D[8]  = ML[8]  + dist(x+1, y+1, z-1);
					D[9]  = ML[9]  + dist(x-1, y-1, z);
					D[10] = ML[10] + dist(x,   y-1, z);
					D[11] = ML[11] + dist(x+1, y-1, z);
					D[12] = ML[12] + dist(x-1, y,   z);
					dist[z][y*w+x] = min(D);
				}
			}
		}
	}

	public void backwardPass() {
		float[] D = new float[14];
		for(int z = d-1; z >= 0; z--) {
			for(int y = h-1; y >= 0; y--) {
				for(int x = w-1; x >= 0; x--) {
					if(dist[z][y*w+x] == 0)
						continue;
					D[0]  = MR[0]  + dist(x+1, y,   z);
					D[1]  = MR[1]  + dist(x-1, y+1, z);
					D[2]  = MR[2]  + dist(x,   y+1, z);
					D[3]  = MR[3]  + dist(x+1, y+1, z);
					D[4]  = MR[4]  + dist(x-1, y-1, z+1);
					D[5]  = MR[5]  + dist(x,   y-1, z+1);
					D[6]  = MR[6]  + dist(x+1, y-1, z+1);
					D[7]  = MR[7]  + dist(x-1, y,   z+1);
					D[8]  = MR[8]  + dist(x,   y,   z+1);
					D[9]  = MR[9]  + dist(x+1, y,   z+1);
					D[10] = MR[10] + dist(x-1, y+1, z+1);
					D[11] = MR[11] + dist(x,   y+1, z+1);
					D[12] = MR[12] + dist(x+1, y+1, z+1);
					D[13] = dist[z][y*w+x];
					dist[z][y*w+x] = min(D);
				}
			}
		}
	}

	public float min(float[] data) {
		float min = maxVal;
		for(int i = 0; i < data.length; i++) {
			if(data[i] < min && data[i] >= 0)
				min = data[i];
		}
		return min;
	}

	public ImagePlus toFloat() {
		ImageStack stack = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			stack.addSlice("",new FloatProcessor(w,h,dist[z],null));
		}
		return new ImagePlus("Distance", stack);
	}

	public ImagePlus rebin() {
		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;
		for(int z = 0; z < d; z++) {
			for(int i = 0; i < w*h; i++) {
				if(dist[z][i] < min)
					min = dist[z][i];
				if(dist[z][i] > max)
					max = dist[z][i];
			}
		}
		float delta = (max - min) / 256;
		ImageStack stack = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			byte[] binned = new byte[w*h];
			for(int i = 0; i < w*h; i++)
				binned[i] = (byte)((dist[z][i]-min) / delta);
			stack.addSlice("", new ByteProcessor(w,h,binned,null));
		}
		return new ImagePlus("Distance", stack);
	}

	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_8G;
	}
}
