package marchingcubes;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import java.util.List;

import vib.Resample_;

import ij3d.ColorTable;

import isosurface.Triangulator;

public class MCTriangulator implements Triangulator {

	public List getTriangles(ImagePlus image, int threshold, 
					boolean[] channels, int resamplingF) {

		image = ColorTable.adjustChannels(image, channels);
		if(resamplingF != 1)
			image = Resample_.resample(image, resamplingF);
		// zero-padding the resampled mesh
		ImageStack stack = zeroPad(image.getStack());
		ImagePlus padded = new ImagePlus(image.getTitle(), stack);
		padded.setCalibration(image.getCalibration().copy());
		List l = MCCube.getTriangles(padded, threshold);
		return l;
	}

	static public ImageStack zeroPad(final ImageStack stack) {
		int w = stack.getWidth();
		int h = stack.getHeight();
		// enlarge all processors
		ImageStack st = new ImageStack(w+2, h+2);
		for (int i=1; i<=stack.getSize(); i++) {
			ImageProcessor ip = new ByteProcessor(w+2, h+2);
			ip.insert(stack.getProcessor(i), 1, 1);
			st.addSlice(Integer.toString(i), ip);
		}
		ByteProcessor bp = new ByteProcessor(w+2, h+2);
		// insert slice at 0
		st.addSlice("0", bp, 0);
		// append slice at the end
		st.addSlice(Integer.toString(stack.getSize()+1), bp);

		return st;
	}
}
