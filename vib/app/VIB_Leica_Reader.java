package vib.app;

import leica.Leica_SP_Reader;
import ij.*;
import ij.io.*;
import ij.measure.Calibration;
import ij.plugin.*;
import java.io.*;
import java.util.*;

/**
 * Extends the leica/Leica_SP_Reader and overrides openStacks(), so that
 * the channels are not shown automatically, but stored in an array. They
 * can be fetched afterwards seperately.
 */
public class VIB_Leica_Reader extends Leica_SP_Reader{

	private ImagePlus[] images;
	
	void openStacks(FileInfo[] fi) throws IOException {
		if (fi[0].fileType!=FileInfo.COLOR8)
			throw new IOException("This does not appear to be  a " + 
					"stack of 8-bit color images.");
		int nr_channels = fi.length;
		int maxStacks = nr_channels;
		ImageStack[] stacks = new ImageStack[maxStacks];
		int width = fi[0].width;
		int height = fi[0].height;
		String name = fi[0].fileName;
		int length_per_channel = fi.length/nr_channels;

		images = new ImagePlus[nr_channels];
		for (int j=0; j<nr_channels; j++){
    			if (stacks[j]==null)
					 stacks[j] = new ImageStack(width,height);
			for (int i=0; i<length_per_channel; i++) {
				int k = i + j * length_per_channel;
				if (fi[k].width!=width || fi[k].height!=height)
					break;
				FileOpener fo = new FileOpener(fi[k]);
				ImagePlus imp = fo.open(false);
				if (imp!=null)
					stacks[j].addSlice("", imp.getProcessor());
				IJ.showProgress(k + 1, nr_channels * length_per_channel);
			}
			if (stacks[j]!=null){
				int l = j+1;
				ImagePlus image =
					new ImagePlus(name+"(channel "+l+")",
							stacks[j]);
				Calibration cal = new Calibration();
				cal.pixelWidth = fi[0].pixelWidth;
				cal.pixelHeight = fi[0].pixelHeight;
				cal.pixelDepth = fi[0].pixelDepth;
				image.setCalibration(cal);
				image.setProperty("Info", fi[0].info);
				// image.show ();
				images[j] = image;
			}
		}
	}

	public ImagePlus getImage(String dir, String file, int channel) 
													throws IOException {
		// this is a hack
		if(images == null) {
			FileInfo[] fi = null;
			try {
				java.lang.reflect.Method m = Leica_SP_Reader.class.
				getDeclaredMethod("getFileInfo", String.class, String.class);
				m.setAccessible(true);
				fi = (FileInfo[])m.invoke(this, dir, file);
				openStacks(fi);
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		return images[channel];
	}
}

