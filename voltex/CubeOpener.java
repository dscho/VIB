package voltex;

import ij.ImagePlus;
import ij.ImageStack;
import ij.LookUpTable;
import ij.io.FileInfo;
import ij.io.TiffDecoder;
import ij.measure.Calibration;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CubeOpener {
	
	public static ImagePlus openCube(String dir, String name) {
		TiffDecoder td = new TiffDecoder(dir, name);
		FileInfo[] info=null;
		try {
			info = td.getTiffInfo();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		if (info==null)
			return null;

		FileInfo fi = info[0];
		ColorModel cm = createColorModel(fi);
		long skip = fi.longOffset > 0 ? fi.longOffset : fi.offset;

		ImageStack stack = new ImageStack(fi.width, fi.height);
		int nPixels = fi.width * fi.height;
		try {
			InputStream is = createInputStream(fi);
			if (is == null)
				return null;
			// skip the specified amount of bytes
			if (skip > 0) {
				long bytesSkipped = 0;
				while (bytesSkipped < skip) {
					bytesSkipped += is.skip(skip - bytesSkipped);
				}
			}
			for (int i = 1; i <= fi.nImages; i++) {
				byte[] pixels = new byte[nPixels];
				// assume contiguous strips
				int read = 0;
				while (read < nPixels) {
					read += is.read(pixels, read, nPixels-read);
				}
				stack.addSlice(null, pixels);
				skip = fi.gapBetweenImages;
			}
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (stack.getSize()==0)
			return null;

		ImagePlus imp = new ImagePlus(fi.fileName, stack);
		if (fi.info!=null)
			imp.setProperty("Info", fi.info);

		imp.setFileInfo(fi);
		setCalibration(fi, imp);
		return imp;
	}

	/** Returns an IndexColorModel for the image specified by this FileInfo. */
	public static ColorModel createColorModel(FileInfo fi) {
		if (fi.fileType==FileInfo.COLOR8 && fi.lutSize>0)
			return new IndexColorModel(8, fi.lutSize, fi.reds, fi.greens, fi.blues);
		else
			return LookUpTable.createGrayscaleColorModel(fi.whiteIsZero);
	}

	/** Returns an InputStream for the image described by this FileInfo. */
	public static InputStream createInputStream(FileInfo fi) throws IOException {
		InputStream is = null;
		if (fi.inputStream!=null)
			is = fi.inputStream;
		else {
			File f = new File(fi.directory, fi.fileName);
			if (f==null || f.isDirectory() || !validateFileInfo(f, fi))
				is = null;
			else
				is = new FileInputStream(f);
		}
		return is;
	}

	static boolean validateFileInfo(File f, FileInfo fi) {
		long offset = fi.longOffset>0?fi.longOffset:fi.offset;
		long length = 0;
		if (fi.width<=0 || fi.height<0) {
		   return false;
		}
		if (offset>=0 && offset<1000)
			 return true;
		if (offset<0) {
		   return false;
		}
		if (fi.fileType==FileInfo.BITMAP || fi.compression!=FileInfo.COMPRESSION_NONE)
			return true;
		length = f.length();
		long size = fi.width*fi.height*fi.getBytesPerPixel();
		size = fi.nImages>1?size:size/4;
		if (fi.height==1) size = 0; // allows plugins to read info of unknown length at end of file
		if (offset+size>length) {
		   return false;
		}
		return true;
	}

	static void setCalibration(FileInfo fi, ImagePlus imp) {
		
		Properties props = decodeDescriptionString(fi);
		Calibration cal = imp.getCalibration();
		boolean calibrated = false;
		if (fi.pixelWidth>0.0 && fi.unit!=null) {
			cal.pixelWidth = fi.pixelWidth;
			cal.pixelHeight = fi.pixelHeight;
			cal.pixelDepth = fi.pixelDepth;
			cal.setUnit(fi.unit);
			calibrated = true;
		}
		
		if (fi.frameInterval!=0.0)
			cal.frameInterval = fi.frameInterval;
		
		if (props==null)
			return;
		
		cal.xOrigin = getDouble(props,"xorigin");
		cal.yOrigin = getDouble(props,"yorigin");
		cal.zOrigin = getDouble(props,"zorigin");
		cal.info = props.getProperty("info");
		
		cal.fps = getDouble(props,"fps");
		cal.loop = getBoolean(props, "loop");
		cal.frameInterval = getDouble(props,"finterval");
		cal.setTimeUnit(props.getProperty("tunit", "sec"));
		
		int stackSize = imp.getStackSize();
		if (stackSize>1) {
			int channels = (int)getDouble(props,"channels");
			int slices = (int)getDouble(props,"slices");
			int frames = (int)getDouble(props,"frames");
			if (channels==0) channels = 1;
			if (slices==0) slices = 1;
			if (frames==0) frames = 1;
			//IJ.log("setCalibration: "+channels+"  "+slices+"  "+frames);
			if (channels*slices*frames==stackSize) {
				imp.setDimensions(channels, slices, frames);
				if (getBoolean(props, "hyperstack"))
					imp.setOpenAsHyperStack(true);
			}
		}
	}

	public static Properties decodeDescriptionString(FileInfo fi) {
		if (fi.description==null || fi.description.length()<7)
			return null;
		if (!fi.description.startsWith("ImageJ"))
			return null;
		Properties props = new Properties();
		InputStream is = new ByteArrayInputStream(fi.description.getBytes());
		try {props.load(is); is.close();}
		catch (IOException e) {return null;}
		fi.unit = props.getProperty("unit","");
		Double n = getNumber(props,"cf");
		if (n!=null) fi.calibrationFunction = n.intValue();
		double c[] = new double[5];
		int count = 0;
		for (int i=0; i<5; i++) {
			n = getNumber(props,"c"+i);
			if (n==null) break;
			c[i] = n.doubleValue();
			count++;
		}
		if (count>=2) {
			fi.coefficients = new double[count];
			for (int i=0; i<count; i++)
				fi.coefficients[i] = c[i];			
		}
		fi.valueUnit = props.getProperty("vunit");
		n = getNumber(props,"images");
		if (n!=null && n.doubleValue()>1.0)
			fi.nImages = (int)n.doubleValue();
		if (fi.nImages>1) {
			double spacing = getDouble(props,"spacing");
			if (spacing!=0.0)
				fi.pixelDepth = spacing;
		}
		return props;
	}

	private static Double getNumber(Properties props, String key) {
		String s = props.getProperty(key);
		if (s!=null) {
			try {
				return Double.valueOf(s);
			} catch (NumberFormatException e) {}
		}	
		return null;
	}
	
	private static double getDouble(Properties props, String key) {
		Double n = getNumber(props, key);
		return n!=null?n.doubleValue():0.0;
	}
	
	private static boolean getBoolean(Properties props, String key) {
		String s = props.getProperty(key);
		return s!=null&&s.equals("true")?true:false;
	}
}
