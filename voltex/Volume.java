package voltex;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.IJ;
import javax.vecmath.Point3d;

/**
 * This class encapsulates an image stack and provides various methods for
 * retrieving data. It is possible to control the loaded color channels of
 * RGB images, and to specify whether or not to average several channels
 * (and merge them in this way into one byte per pixel).
 * 
 * Depending on these settings, and on the type of image given at construction
 * time, the returned data type is one of INT_DATA or BYTE_DATA.
 * 
 * @author Benjamin Schmid
 */
public class Volume {

	/** Data is read as int data */
	public static final int INT_DATA = 0;
	/** Data is read as byte data */
	public static final int BYTE_DATA = 1;
	
	/** The image holding the data */
	private final ImagePlus imp;

	/** The loader, initialized depending on the data type */
	private Loader loader;

	/** 
	 * Indicates in which format the data is loaded. This depends on
	 * the image type and on the number of selected channels.
	 * May be one of INT_DATA or BYTE_DATA
	 */
	private int dataType;

	/** Flag indicating that the channels should be averaged */
	private boolean average = false;

	/** Channels in RGB images which should be loaded */
	private boolean[] channels = new boolean[] {true, true, true};

	/** The dimensions of the data */
	public final int xDim, yDim, zDim;

	/** The calibration of the data */
	public final double pw, ph, pd;

	/** The textures' size. These are powers of two. */
	public final int xTexSize, yTexSize, zTexSize;

	/** The texGenScale */
	public final float xTexGenScale, yTexGenScale, zTexGenScale;

	/** The minimum coordinate of the data */
	final Point3d minCoord = new Point3d();

	/** The maximum coordinate of the data */
	final Point3d maxCoord = new Point3d();

	/** The mid point in the data */
	final Point3d volRefPt = new Point3d();

	/**
	 * Initializes this Volume with the specified image.
	 * All channels are used.
	 * @param imp
	 */
	public Volume(ImagePlus imp) {
		this(imp, new boolean[] {true, true, true});
	}

	/**
	 * Initializes this Volume with the specified image and channels.
	 * @param imp
	 * @param ch A boolean[] array of length three, which indicates whether
	 * the red, blue and green channel should be read. This has only an
	 * effct when reading color images.
	 */
	public Volume(ImagePlus imp, boolean[] ch) {
		this.channels = ch;
		this.imp = imp;
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

		float xSpace = (float)pw;
		float ySpace = (float)ph;
		float zSpace = (float)pd;

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

	/**
	 * Get the current set data type. This is one of BYTE_DATA or INT_DATA.
	 * The data type specifies in which format the data is read:
	 * This method returns INT_DATA, if for example the image is of type
	 * RGB and more than one channels should be read.
	 * If only one channels is read, or if the type of the image is 8-bit,
	 * it will return BYTE_DATA.
	 * @return The type of the returned data.
	 */
	public int getDataType() {
		return dataType;
	}

	/**
	 * If true, build an average byte from the specified channels 
	 * (for each pixel).
	 * @return true if the value for 'average' has changed.
	 */
	public boolean setAverage(boolean a) {
		if(average != a) {
			this.average = a;
			initLoader();
			return true;
		}
		return false;
	}

	/**
	 * Returns true if specified channels are being averaged when
	 * reading the image data.
	 * @return
	 */
	public boolean isAverage() {
		return average;
	}

	/**
	 * Specify the channels which should be read from the image.
	 * This only affects RGB images.
	 * @return true if the channels settings has changed.
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
	 * Initializes the specific loader which is used for the current
	 * settings. The choice depends on the specific values of channels,
	 * average and data type.
	 */
	private void initLoader() {

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

	/**
	 * Calculate the next power of two to the given value.
	 * @param value
	 * @return
	 */
	private int powerOfTwo(int value) {
		int retval = 16;
		while (retval < value) {
			retval *= 2;
		}
		return retval;
	}

	public void set(int x, int y, int z, int v) {
		loader.set(x, y, z, v);
	}

	/**
	 * Load the value at the specified position
	 * @param x
	 * @param y
	 * @param z
	 * @return value. Casted to int if it was a byte value before.
	 */
	public int load(int x, int y, int z) {
		return loader.load(x, y, z);
	}

	/**
	 * Loads a xy-slice at the given z position.
	 * @param z
	 * @param dst must be an int[] or byte[] array (depending on what
	 * getDataType() says) of correct length.
	 */
	public void loadZ(int z, Object dst) {
		loader.loadZ(z, dst);
	}

	/**
	 * Loads a xz-slice at the given y position.
	 * @param y
	 * @param dst must be an int[] or byte[] array (depending on what
	 * getDataType() says) of correct length.
	 */
	public void loadY(int y, Object dst) {
		loader.loadY(y, dst);
	}

	/**
	 * Loads a yz-slice at the given x position.
	 * @param x
	 * @param dst must be an int[] or byte[] array (depending on what
	 * getDataType() says) of correct length.
	 */
	public void loadX(int x, Object dst) {
		loader.loadX(x, dst);
	}

	/**
	 * Abstract interface for the loader classes.
	 */
	private abstract class Loader {
		abstract int load(int x, int y, int z);
		abstract void set(int x, int y, int z, int v);

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
		}

		int load(int x, int y, int z) {
			return (int)fData[z][y * w + x] & 0xff;
		}

		void set(int x, int y, int z, int v) {
			fData[z][y * w + x] = (byte)v;
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

		void set(int x, int y, int z, int v) {
			fData[z][y * w + x] = v;
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

		void set(int x, int y, int z, int v) {
			fData[z][y * w + x] = v;
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

		void set(int x, int y, int z, int v) {
			fdata[z][y * w + x] = v;
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
