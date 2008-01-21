import java.io.File;

import ij.gui.GenericDialog;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;

import vib.app.FileGroup;
import vib.app.gui.FileGroupDialog;

/**
 * Rohlfing, Maurer (2007): Shape-based averaging
 */
public class Rohlfing_SBA implements PlugIn {

	private FileGroup fg;
	private ImagePlus D_min;
	private ImagePlus output;

	private ImagePlus image;
	private int w, h, d, L, K;

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Rohlfing");
		fg = new FileGroup("files");
		FileGroupDialog fgd = new FileGroupDialog(fg, false);
		gd.addPanel(fgd);
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		init();
		doit();
	}

	private void doit() {
		ImagePlus D = null;
		for(int l = 0; l < L; l++) {
			if(l != 0 && l != 85 && l != 120 
				&& l != 132 && l != 153 && l != 170) {
				continue;
			}
			// Sum up the distance maps of all input images
			for(int k = 0; k < K; k++) {
				if(D == null)
					D = d_kl(l, k);
				else
					new ImageCalculator().calculate(
						"add 32 stack", D, d_kl(l, k));
			}
			for(int z = 0; z < d; z++) {
				// Devide it by the number of input images
				D.getStack().getProcessor(z+1).multiply(1.0/K);
				float[] D_p = (float[])D.getStack()
						.getProcessor(z+1).getPixels();
				float[] D_minp = (float[])D_min.getStack()
						.getProcessor(z+1).getPixels();
				byte[] output_p = (byte[])output.getStack()
						.getProcessor(z+1).getPixels();
				// if average distance is smaller than 
				// min distance, output is l
				for(int i = 0; i < w*h; i++) {
					if(D_p[i] < D_minp[i]) {
						output_p[i] = (byte)l;
						D_minp[i] = D_p[i];
					}
				}
			}
		}
		output.show();
	}

	private ImagePlus d_kl(int l, int k) {
		File file = fg.get(k);
		ImagePlus image = new Opener().openImage(
			file.getParentFile().getAbsolutePath(),
			file.getName());
		// Remember: need signed dist transform
		// Outside EDT
		ImagePlus im1 = new Distance_Transform_3D()
			.getTransformed(createBinary(image, l), 0);
		// Inside EDT
		ImagePlus im2 = new Distance_Transform_3D()
			.getTransformed(createBinary(image, l), 255);
		// Subtract Inside EDT from Outside EDT
		new ImageCalculator().calculate(
			"sub 32 stack", im1, im2);
		return im1;
	}

	private ImagePlus createBinary(ImagePlus image, int value) {
		int w = image.getWidth(), h = image.getWidth();
		int d = image.getStackSize();
		ImageStack stack = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			byte[] f = new byte[w*h];
			byte[] p = (byte[])image.getStack()
					.getProcessor(z+1).getPixels();
			for(int i = 0; i < w*h; i++)
				f[i] = (int)(p[i]&0xff) == value ? (byte)255 : 0;
			stack.addSlice("", new ByteProcessor(w, h, f, null));	
		}
		return new ImagePlus("Label_" + (int)(value&0xff), stack);
	}

	private void init() {
		File file = fg.get(0);
		image = new Opener().openImage(
			file.getParentFile().getAbsolutePath(),
			file.getName());
		w = image.getWidth();
		h = image.getHeight();
		d = image.getStackSize();
		L = 255;
		K = fg.size();

		// output value initialized to 0
		ImageStack stack = new ImageStack(w, h);
		for(int z = 0; z < d; z++)
			stack.addSlice("", new ByteProcessor(w, h));
		output = new ImagePlus("Output", stack);
		// D_min initialized to Infinity
		stack = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			float[] f = new float[w*h];
			for(int i = 0; i < w*h; i++)
				f[i] = Float.MAX_VALUE;
			stack.addSlice("", new FloatProcessor(w, h, f, null));
		}
		D_min = new ImagePlus("D_min", stack);
	}
}
