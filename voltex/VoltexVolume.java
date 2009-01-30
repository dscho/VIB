package voltex;

import ij.ImagePlus;
import ij3d.Volume;
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
public class VoltexVolume extends Volume {

	/** The textures' size. These are powers of two. */
	public final int xTexSize, yTexSize, zTexSize;

	/** The texGenScale */
	public final float xTexGenScale, yTexGenScale, zTexGenScale;

	/** The mid point in the data */
	final Point3d volRefPt = new Point3d();
	
	/** The particular loader which loads the actual data into the arrays */
	protected Loader loader;

	/**
	 * Initializes this Volume with the specified image.
	 * All channels are used.
	 * @param imp
	 */
	public VoltexVolume(ImagePlus imp) {
		this(imp, new boolean[] {true, true, true});
	}

	/**
	 * Initializes this Volume with the specified image and channels.
	 * @param imp
	 * @param ch A boolean[] array of length three, which indicates whether
	 * the red, blue and green channel should be read. This has only an
	 * effct when reading color images.
	 */
	public VoltexVolume(ImagePlus imp, boolean[] ch) {

		super(imp, ch);
		// tex size is next power of two greater than max - min
		// regarding pixels
		xTexSize = powerOfTwo(xDim);
		yTexSize = powerOfTwo(yDim);
		zTexSize = powerOfTwo(zDim);

		float xSpace = (float)pw;
		float ySpace = (float)ph;
		float zSpace = (float)pd;

		// xTexSize is the pixel dim of the file in x-dir, e.g. 256
		// xSpace is the normalised length of a pixel
		xTexGenScale = (float)(1.0 / (xSpace * xTexSize));
		yTexGenScale = (float)(1.0 / (ySpace * yTexSize));
		zTexGenScale = (float)(1.0 / (zSpace * zTexSize));

		// the min and max coords are for the usable area of the texture,
		volRefPt.x = (maxCoord.x + minCoord.x) / 2;
		volRefPt.y = (maxCoord.y + minCoord.y) / 2;
		volRefPt.z = (maxCoord.z + minCoord.z) / 2;
	}

	/*
	 * Initializes the specific loader which is used for the current
	 * settings. The choice depends on the specific values of channels,
	 * average and data type.
	 */
	@Override
	protected void initLoader() {

		boolean[] c = channels;
		int usedCh = 0;
		for(int i = 0; i < 3; i++)
			if(channels[i]) usedCh++;
		switch(imp.getType()) {
			case ImagePlus.GRAY8:
				loader = new ByteLoader(imp);
				dataType = BYTE_DATA;
				break;
			case ImagePlus.COLOR_RGB:
				if(usedCh == 1) {
					loader = new ByteFromIntLoader(imp, c);
					dataType = BYTE_DATA;
				} else if(usedCh == 2) {
					if(average) {
						loader = new ByteFromIntLoader(imp, c);
						dataType = BYTE_DATA;
					} else {
						loader = new IntFromIntLoader(imp, c);
						dataType = INT_DATA;
					}
				} else {
					if(average) {
						loader = new ByteFromIntLoader(imp, c);
						dataType = BYTE_DATA;
					} else {
						loader = new IntLoader(imp);
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
	protected interface Loader extends Volume.Loader {
		void loadZ(int z, Object dst);
		void loadY(int y, Object dst);
		void loadX(int x, Object dst);
	}

	/*
	 * This class loads bytes from byte data.
	 */
	private final class ByteLoader extends Volume.ByteLoader implements Loader {
		ByteLoader(ImagePlus imp) {
			super(imp);
		}

		public void loadZ(int zValue, Object arr) {
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
		public void loadY(int yValue, Object arr)  {
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
		public void loadX(int xValue, Object arr)  {
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
	private final class IntLoader extends Volume.IntLoader implements Loader {
		IntLoader(ImagePlus imp) {
			super(imp);
		}

		public void loadZ(int zValue, Object arr) {
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
		public void loadY(int yValue, Object arr)  {
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
		public void loadX(int xValue, Object arr)  {
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
	private final class IntFromIntLoader extends Volume.IntFromIntLoader implements Loader {
		IntFromIntLoader(ImagePlus imp, boolean[] channels) {
			super(imp, channels);
		}

		public void loadZ(int zValue, Object arr) {
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
		public void loadY(int yValue, Object arr)  {
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
		public void loadX(int xValue, Object arr)  {
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
	private final class ByteFromIntLoader extends Volume.ByteFromIntLoader implements Loader {
		ByteFromIntLoader(ImagePlus imp, boolean[] channels) {
			super(imp, channels);
		}

		public void loadZ(int zValue, Object arr) {
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
		public void loadY(int yValue, Object arr)  {
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
		public void loadX(int xValue, Object arr)  {
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
