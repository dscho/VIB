package voltex;

import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import com.sun.j3d.utils.behaviors.mouse.*;
import java.net.*;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.IJ;

public class Volume {

	public static final int INT_DATA = 0;
	public static final int BYTE_DATA = 1;
	
	public static final int TRANSLUCENT = 2;
	public static final int OPAQUE = 3;

	private ImagePlus imp;
	private Loader loader;

	private int dataType;
	private int transparencyType = TRANSLUCENT;
	private boolean average = false;

	private boolean[] channels = new boolean[] {true, true, true};

	public final int xDim, yDim, zDim;
	public final double pw, ph, pd;

	public final float xSpace, ySpace, zSpace;
	public final int xTexSize, yTexSize, zTexSize;
	public final float xTexGenScale, yTexGenScale, zTexGenScale;

	final Point3d minCoord = new Point3d();
	final Point3d maxCoord = new Point3d();
	final Point3d volRefPt = new Point3d();

	public Volume(ImagePlus imp) {
		this(imp, OPAQUE);
	}

	public Volume(ImagePlus imp, int transpType) {
		this(imp, transpType, new boolean[] {true, true, true});
	}

	public Volume(ImagePlus imp, int transpType, boolean[] ch) {
		this.channels = ch;
		this.imp = imp;
		this.transparencyType = transpType;
		xDim = imp.getWidth();
		yDim = imp.getHeight();
		zDim = imp.getStackSize();
		Calibration c = imp.getCalibration();
		pw = c.pixelWidth;
		ph = c.pixelHeight;
		pd = c.pixelDepth;
		
		// tex size is next power of two greater than max - min
		// regarding pixels
		xTexSize = powerOfTwo(xDim);
		yTexSize = powerOfTwo(yDim);
		zTexSize = powerOfTwo(zDim);

		xSpace = (float)pw;
		ySpace = (float)ph;
		zSpace = (float)pd;

		// real coords
		minCoord.x = c.xOrigin * xSpace;
		minCoord.y = c.yOrigin * ySpace;
		minCoord.z = c.zOrigin * zSpace;

		maxCoord.x = minCoord.x + xDim * xSpace;
		maxCoord.y = minCoord.y + yDim * ySpace;
		maxCoord.z = minCoord.z + zDim * zSpace;

		// xTexSize is the pixel dim of the file in x-dir, e.g. 256
		// xSpace is the normalised length of a pixel
		xTexGenScale = (float)(1.0 / (xSpace * xTexSize));
		yTexGenScale = (float)(1.0 / (ySpace * yTexSize));
		zTexGenScale = (float)(1.0 / (zSpace * zTexSize));

		// the min and max coords are for the usable area of the texture,
		volRefPt.x = (maxCoord.x + minCoord.x) / 2;
		volRefPt.y = (maxCoord.y + minCoord.y) / 2;
		volRefPt.z = (maxCoord.z + minCoord.z) / 2;

		initLoader();
	}

	public int getDataType() {
		return dataType;
	}

	public int getTransparenyType() {
		return transparencyType;
	}

	public void setTransparencyType(int t) {
		transparencyType = t;
	}

	/*
	 * Returns a flag which indicates whether the textures should
	 * be reloaded.
	 */
	public boolean setAverage(boolean a) {
		if(average != a) {
			this.average = a;
			initLoader();
			return true;
		}
		return false;
	}

	public boolean isAverage() {
		return average;
	}

	/*
	 * Returns true if data has to be reloaded.
	 */
	public boolean setChannels(boolean[] ch) {
		if(ch[0] == channels[0] && 
			ch[1] == channels[1] && 
			ch[2] == channels[2])
			return false;
		channels = ch;
		initLoader();
		return true;
	}

	/*
	 * OK, this code is not nice. I will clean it later, as soon as it
	 * works.
	 */
	public void initLoader() {

		boolean[] c = channels;
		int usedCh = 0;
		for(int i = 0; i < 3; i++)
			if(channels[i]) usedCh++;
		switch(imp.getType()) {
			case ImagePlus.GRAY8:
				loader = new ByteLoader();
				dataType = BYTE_DATA;
				break;
			case ImagePlus.COLOR_RGB:
				if(usedCh == 1) {
					loader = new ByteFromIntLoader(c);
					dataType = BYTE_DATA;
				} else if(usedCh == 2) {
					if(average) {
						loader = new ByteFromIntLoader(c);
						dataType = BYTE_DATA;
					} else {
						loader = new IntFromIntLoader(c);
						dataType = INT_DATA;
					}
				} else {
					if(average) {
						loader = new ByteFromIntLoader(c);
						dataType = BYTE_DATA;
					} else {
						loader = new IntLoader();
						dataType = INT_DATA;
					}
				}
				break;
			default: 
				IJ.error("image format not supported");
				break;
		}
	}

	public boolean hasData() {
		return (imp != null);
	}

	private int powerOfTwo(int value) {
		int retval = 16;
		while (retval < value) {
			retval *= 2;
		}
		return retval;
	}

	public int load(int x, int y, int z) {
		return loader.load(x, y, z);
	}

	public void loadZ(int z, Object dst) {
		loader.loadZ(z, dst);
	}

	public void loadY(int y, Object dst) {
		loader.loadY(y, dst);
	}

	public void loadX(int x, Object dst) {
		loader.loadX(x, dst);
	}

	private abstract class Loader {
		abstract int load(int x, int y, int z);

		abstract void loadZ(int z, Object dst);
		abstract void loadY(int y, Object dst);
		abstract void loadX(int x, Object dst);
	}

	/*
	 * This class loads bytes from byte data.
	 */
	private final class ByteLoader extends Loader {
		byte[][] fData;
		int w;

		ByteLoader() {
			ImageStack stack = imp.getStack();
			int d = imp.getStackSize();
			w = imp.getWidth();
			fData = new byte[d][];
			for (int z = 0; z < d; z++)
				fData[z] = (byte[])stack.getPixels(z+1);
System.out.println("ByteLoader");
		}

		int load(int x, int y, int z) {
			return (int)fData[z][y * w + x] & 0xff;
		}

		void loadZ(int zValue, Object arr) {
			byte[] dst = (byte[])arr;
			byte[] src = fData[zValue];
			for (int y=0; y < yDim; y++){
				int offsSrc = y * xDim;
				int offsDst = y * xTexSize;
				System.arraycopy(
					src, offsSrc, dst, offsDst, xDim);
			}
		}

		/* 
		 * this routine loads values for constant yValue, the 
		 * texture map is stored in x,z format (x changes fastest)
		 */
		void loadY(int yValue, Object arr)  {
			byte[] dst = (byte[])arr;
			for (int z=0; z < zDim; z++){
				byte[] src = fData[z];
				int offsSrc = yValue * xDim;
				int offsDst = z * xTexSize;
				System.arraycopy(
					src, offsSrc, dst, offsDst, xDim);
			}
		}

		/* 
		 * this routine loads values for constant xValue, into 
		 * byteData in y,z order (y changes fastest)
		 */
		void loadX(int xValue, Object arr)  {
			byte[] dst = (byte[])arr;
			for (int z=0; z < zDim; z++){
				byte[] src = fData[z];
				int offsDst = z * yTexSize;
				for (int y=0; y < yDim; y++){
					int offsSrc = y * xDim + xValue;
					dst[offsDst + y] = fData[z][offsSrc];
				}
			}
		}
	}

	/*
	 * This class loads all channels from int data and returns
	 * it as int array.
	 */
	private final class IntLoader extends Loader {
		int[][] fData;
		int w;

		IntLoader() {
System.out.println("IntLoader");
			ImageStack stack = imp.getStack();
			int d = imp.getStackSize();
			w = imp.getWidth();
			fData = new int[d][];
			for (int z = 0; z < d; z++)
				fData[z] = (int[])stack.getPixels(z+1);
			adjustAlphaChannel();
		}

		void adjustAlphaChannel() {
			for(int z = 0; z < fData.length; z++) {
				for(int i = 0; i < fData[z].length; i++) {
					int v = fData[z][i];
					int r = (v&0xff0000)>>16;
					int g = (v&0xff00)>>8;
					int b = (v&0xff);
					int a = ((r + g + b) / 3) << 24;
					fData[z][i] = (v & 0xffffff) + a;
				}
			}
		}

		int load(int x, int y, int z) {
			return fData[z][y * w + x];
		}

		void loadZ(int zValue, Object arr) {
			int[] dst = (int[])arr;
			int[] src = fData[zValue];
			for (int y=0; y < yDim; y++){
				int offsSrc = y * xDim;
				int offsDst = y * xTexSize;
				System.arraycopy(src,offsSrc,dst,offsDst,xDim);
			}
		}

		/* 
		 * this routine loads values for constant yValue, the 
		 * texture map is stored in x,z format (x changes fastest)
		 */
		void loadY(int yValue, Object arr)  {
			int[] dst = (int[])arr;
			for (int z=0; z < zDim; z++){
				int[] src = fData[z];
				int offsSrc = yValue * xDim;
				int offsDst = z * xTexSize;
				System.arraycopy(src,offsSrc,dst,offsDst,xDim);
			}
		}

		/* 
		 * this routine loads values for constant xValue, into 
		 * byteData in y,z order (y changes fastest)
		 */
		void loadX(int xValue, Object arr)  {
			int[] dst = (int[])arr;
			for (int z=0; z < zDim; z++){
				int[] src = fData[z];
				int offsDst = z * yTexSize;
				for (int y=0; y < yDim; y++){
					int offsSrc = y * xDim + xValue;
					dst[offsDst + y] = fData[z][offsSrc];
				}
			}
		}
	}

	/*
	 * Loads the specified channels from int data
	 * This class should only be used if not all channels are
	 * used. Otherwise, it's faster to use the IntLoader.
	 */
	private final class IntFromIntLoader extends Loader {
		int[][] fData;
		int w;
		int mask = 0xffffff;
		boolean[] ch = new boolean[] {true, true, true};
		int usedCh = 3;

		IntFromIntLoader(boolean[] channels) {
System.out.println("IntFromIntLoader");
			ImageStack stack = imp.getStack();
			int d = imp.getStackSize();
			fData = new int[d][];
			for (int z = 0; z < d; z++)
				fData[z] = (int[])stack.getPixels(z+1);

			ch = channels;
			usedCh = 0;
			mask = 0xff000000;
			if(ch[0]) { usedCh++; mask |= 0xff0000; }
			if(ch[1]) { usedCh++; mask |= 0xff00; }
			if(ch[2]) { usedCh++; mask |= 0xff; }
			adjustAlphaChannel();
		}

		void adjustAlphaChannel() {
			for(int z = 0; z < fData.length; z++) {
				for(int i = 0; i < fData[z].length; i++) {
					int v = fData[z][i];
					int n = 0;
					if(ch[0]) n += (v & 0xff0000) >> 16;
					if(ch[1]) n += (v & 0xff00) >> 8;
					if(ch[2]) n += (v & 0xff);
					int a = (n / usedCh) << 24;
					fData[z][i] = (v & 0xffffff) + a;
				}
			}
		}

		int load(int x, int y, int z) {
			return fData[z][y * w + x] & mask;
		}

		void loadZ(int zValue, Object arr) {
			int[] dst = (int[])arr;
			int[] src = fData[zValue];
			for (int y=0; y < yDim; y++){
				int offsSrc = y * xDim;
				int offsDst = y * xTexSize;
				System.arraycopy(src,offsSrc,dst,offsDst,xDim);
			}
			for(int i = 0; i < dst.length; i++)
				dst[i] &= mask;
		}

		/* 
		 * this routine loads values for constant yValue, the 
		 * texture map is stored in x,z format (x changes fastest)
		 */
		void loadY(int yValue, Object arr)  {
			int[] dst = (int[])arr;
			for (int z=0; z < zDim; z++){
				int[] src = fData[z];
				int offsSrc = yValue * xDim;
				int offsDst = z * xTexSize;
				System.arraycopy(src,offsSrc,dst,offsDst,xDim);
			}
			for(int i = 0; i < dst.length; i++)
				dst[i] &= mask;
		}

		/* 
		 * this routine loads values for constant xValue, into 
		 * byteData in y,z order (y changes fastest)
		 */
		void loadX(int xValue, Object arr)  {
			int[] dst = (int[])arr;
			for (int z=0; z < zDim; z++){
				int[] src = fData[z];
				int offsDst = z * yTexSize;
				for (int y=0; y < yDim; y++){
					int offsSrc = y * xDim + xValue;
					dst[offsDst + y] = fData[z][offsSrc];
				}
			}
			for(int i = 0; i < dst.length; i++)
				dst[i] &= mask;
		}
	}

	/*
	 * Loads from the specified channels an average byte from int
	 * data.
	 */
	private final class ByteFromIntLoader extends Loader {
		int[][] fdata;
		int w;
		boolean[] channels = new boolean[] {true, true, true};
		int usedCh = 3;

		ByteFromIntLoader(boolean[] channels) {
System.out.println("ByteFromIntLoader");
			this.channels = channels;
			ImageStack stack = imp.getStack();
			int d = imp.getStackSize();
			w = imp.getWidth();
			fdata = new int[d][];
			for (int z = 0; z < d; z++)
				fdata[z] = (int[])stack.getPixels(z+1);
			usedCh = 0;
			for(int i = 0; i < 3; i++)
				if(channels[i]) usedCh++;
		}

		int load(int x, int y, int z) {
			int v = fdata[z][y*w + x], n = 0;
			if(channels[0]) n += (v & 0xff0000) >> 16;
			if(channels[1]) n += (v & 0xff00) >> 8;
			if(channels[2]) n += (v & 0xff);
			return (n /= usedCh);
		}

		void loadZ(int zValue, Object arr) {
			byte[] dst = (byte[])arr;
			int[] src = fdata[zValue];
			for (int y=0; y < yDim; y++){
				int offssrc = y * xDim;
				int offsdst = y * xTexSize;
				for(int x = 0; x < xDim; x++) {
					int v = src[offssrc + x];
					int n = 0;
					if(channels[0]) n += (v&0xff0000)>>16;
					if(channels[1]) n += (v&0xff00)>>8;
					if(channels[2]) n += (v&0xff);
					n /= usedCh;
					dst[offsdst + x] = (byte)n;
				}
			}
		}

		/* 
		 * this routine loads values for constant yValue, the 
		 * texture map is stored in x,z format (x changes fastest)
		 */
		void loadY(int yValue, Object arr)  {
			byte[] dst = (byte[])arr;
			for (int z=0; z < zDim; z++){
				int[] src = fdata[z];
				int offssrc = yValue * xDim;
				int offsdst = z * xTexSize;
				for(int x = 0; x < xDim; x++) {
					int v = src[offssrc + x];
					int n = 0;
					if(channels[0]) n += (v&0xff0000)>>16;
					if(channels[1]) n += (v&0xff00)>>8;
					if(channels[2]) n += (v&0xff);
					n /= usedCh;
					dst[offsdst + x] = (byte)n;
				}
			}
		}

		/* 
		 * this routine loads values for constant xValue, into 
		 * bytedata in y,z order (y changes fastest)
		 */
		void loadX(int xValue, Object arr)  {
			byte[] dst = (byte[])arr;
			for (int z=0; z < zDim; z++){
				int[] src = fdata[z];
				int offsdst = z * yTexSize;
				for (int y=0; y < yDim; y++){
					int offssrc = y * xDim + xValue;
					int v = src[offssrc];
					int n = 0;
					if(channels[0]) n += (v&0xff0000)>>16;
					if(channels[1]) n += (v&0xff00)>>8;
					if(channels[2]) n += (v&0xff);
					n /= usedCh;
					dst[offsdst + y] = (byte)n;
				}
			}
		}
	}
}
