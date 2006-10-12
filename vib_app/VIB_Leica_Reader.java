package vib_app;

import ij.*;
import ij.io.*;
import ij.measure.Calibration;
import ij.plugin.*;
import java.io.*;
import java.util.*;

/**
Opens multi-image 8-bits tiff files created by Leica confocal microscope
systems using each channels own LUT.  Modified by Nico Stuurman June 2000
Modified to set the real dimensions by J. Schindelin 2006
 */
public class VIB_Leica_Reader {

	int nr_channels = 1; 
	private ImagePlus[] images;
	
	FileInfo[] getFileInfo(String directory, String name) throws IOException {
		LeicaTiffDecoder td = new LeicaTiffDecoder(directory, name);
		if (IJ.debugMode) td.enableDebugging();
		FileInfo[] info = td.getTiffInfo();
		nr_channels = td.nr_channels;
		if (info==null)
			throw new IOException("This file does not appear to be in TIFF format.");
		if (IJ.debugMode) // dump tiff tags
			IJ.write(info[0].info);
		return info;
	}
	
	void openStacks(FileInfo[] fi) throws IOException {
		if (fi[0].fileType!=FileInfo.COLOR8)
			throw new IOException("This does not appear to be  a stack of 8-bit color images.");
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
		if(images == null) {
			FileInfo[] fi = getFileInfo(dir, file);
			openStacks(fi);
		}
		return images[channel];
	}

/* This class inherits ImageJ's TiffDecoder and overrides the decodeImageDescription method.
The Leica SP files start their image description with "[GLOBAL]".  The number
of channels in a Leica SP TIFF file is given within the image description as "NumOfVisualisations"=x.

*/ 
public static class LeicaTiffDecoder extends TiffDecoder{

	public int nr_channels = 1;

	public  LeicaTiffDecoder(String directory, String name) {
		super(directory, name);
	}

	public void saveImageDescription(byte[] description, FileInfo fi) {	
		decodeImageDescription(description,fi);
	}
	public void decodeImageDescription(byte[] description, FileInfo fi) {	
		if (new String (description,0,8).equals("[GLOBAL]")) {
			if (debugMode) ij.IJ.write ("Leica file detected..." + "\n");
			String file_specs = new String (description);
			fi.info = file_specs;
			if (debugMode) ij.IJ.write(file_specs);
			StringTokenizer st = new StringTokenizer(file_specs, "\n= ");
			while (st.hasMoreTokens()) {
				String s = st.nextToken();
				if (s.equals ("NumOfVisualisations")) {
					nr_channels = getInt(st);
					if (debugMode)
						ij.IJ.write ("channels detected: " +nr_channels+ "\n");
				} else if (s.equals ("VoxelSizeX"))
					fi.pixelWidth = getDouble(st);
				else if (s.equals ("VoxelSizeY"))
					fi.pixelHeight = getDouble(st);
				else if (s.equals ("VoxelSizeZ"))
					fi.pixelDepth = getDouble(st);
			}         
		}
	}

	public int getInt(StringTokenizer st) {
		String temp = st.nextToken().trim();
		try {
			return Integer.parseInt(temp);
		} catch (NumberFormatException e) {
			throw new RuntimeException("invalid number: "
					+ temp);
		}
	}

	public double getDouble(StringTokenizer st) {
		String temp = st.nextToken().trim();
		try {
			return Double.parseDouble(temp);
		} catch (NumberFormatException e) {
			throw new RuntimeException("invalid number: "
					+ temp);
		}
	}
}
}

