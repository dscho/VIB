import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.ImageStack;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class Reslice_Z implements PlugInFilter {

	private ImagePlus image;
	private Calibration cal;
	private int w, h;

	public void run(ImageProcessor ip) {
		w = image.getWidth();
		h = image.getHeight();
		cal = image.getCalibration();
		GenericDialog gd = new GenericDialog("Reslice_Z");
		gd.addNumericField("New pixel depth", cal.pixelDepth, 3);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		double pixelDepth = gd.getNextNumber();
		reslice(pixelDepth).show();
	}

	public ImagePlus reslice(double pixelDepth) {

		ImageStack stack = image.getStack();
		int numSlices = (int)(image.getStackSize() * cal.pixelDepth / 
					pixelDepth);
		
		// Create a new Stack
		ImageStack newStack = new ImageStack(w, h);
		for(int z = 0; z < numSlices-1; z++) {
			ByteProcessor bp = new ByteProcessor(w, h);
			byte[] pixels = (byte[])bp.getPixels();
			// getSliceBefore
			double currentPosition = z * pixelDepth;
			int ind_p = (int)Math.floor(
					currentPosition / cal.pixelDepth);
			int ind_n = ind_p + 1;
			double d_p = currentPosition - ind_p*cal.pixelDepth;
			double d_n = ind_n*cal.pixelDepth - currentPosition;

			byte[] before = (byte[])stack.
					getProcessor(ind_p+1).getPixels();
			byte[] after = (byte[])stack.
					getProcessor(ind_n+1).getPixels();
			
			for(int i = 0; i < pixels.length; i++) {
				pixels[i] = (byte) ((((int)before[i]&0xff)*d_n
					+ ((int)after[i]&0xff)*d_p) / 
						cal.pixelDepth);
			}
			
			newStack.addSlice("", bp);
		}
		ImagePlus result = new ImagePlus("Resliced", newStack);
		cal = cal.copy();
		cal.pixelDepth = pixelDepth;
		result.setCalibration(cal);
		return result;
	}

	public int setup(String arg, ImagePlus img) {
		this.image = img;
		return DOES_8G;
	}
}
