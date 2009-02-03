package voltex;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij3d.Volume;
import ij.IJ;

import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
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
	public final Point3d volRefPt = new Point3d();

	/** The particular loader which loads the actual data into the arrays */
	protected Loader loader;

	/** The ColorModel used for 8-bit textures */
	protected static final ColorModel greyCM = createGreyColorModel();

	/** The ColorModel used for RGB textures */
	protected static final ColorModel rgbCM = createRGBColorModel();

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

	public ImageComponent2D getImageComponentZ(int index) {
		return loader.getComponentZ(index);
	}
	
	public ImageComponent2D getImageComponentY(int index) {
		return loader.getComponentY(index);
	}
	
	public ImageComponent2D getImageComponentX(int index) {
		return loader.getComponentX(index);
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
	protected static int powerOfTwo(int value) {
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
	
	protected ImageComponent2D createGreyComponent(byte[] pix, int w, int h) {
		DataBufferByte db = new DataBufferByte(pix, w * h, 0);
		SampleModel smod = greyCM.createCompatibleSampleModel(w, h);
		WritableRaster raster = Raster.createWritableRaster(smod, db, null);

		BufferedImage bImage = new BufferedImage(
				greyCM, raster, false, null);
		ImageComponent2D bComp = new ImageComponent2D(
				ImageComponent.FORMAT_CHANNEL8, w, h, false, true);
		bComp.setCapability(ImageComponent.ALLOW_IMAGE_WRITE);
		bComp.set(bImage);
		return bComp;
	}

	protected ImageComponent2D createColorComponent(byte[] pix, int w, int h) {
		int[] bandOffset = { 0, 1, 2, 3 };

		DataBufferByte db = new DataBufferByte(pix, w * h * 4, 0);
		WritableRaster raster = Raster.createInterleavedRaster(
						db, w, h, w * 4, 4, bandOffset, null);

		BufferedImage bImage =  new BufferedImage(
				rgbCM, raster, false, null);
		ImageComponent2D bComp = new ImageComponent2D(
				ImageComponent.FORMAT_RGBA, w, h, false, true);
		bComp.setCapability(ImageComponent.ALLOW_IMAGE_WRITE);
		bComp.set(bImage);
		return bComp;
	}

	private static final ColorModel createGreyColorModel() {
		byte[] r = new byte[256], g = new byte[256], b = new byte[256];
		for(int i = 0; i < 256; i++)
			r[i] = (byte)i;
		return new IndexColorModel(8, 256, r, g, b);
	}

	private static final ColorModel createRGBColorModel() {
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
		int[] nBits = { 8, 8, 8, 8 };
		return new ComponentColorModel(
				cs, nBits, true, false, BufferedImage.TRANSLUCENT,
				DataBuffer.TYPE_BYTE);
	}

	/**
	 * Abstract interface for the loader classes.
	 */
	protected interface Loader extends Volume.Loader {
		void loadZ(int z, Object dst);
		void loadY(int y, Object dst);
		void loadX(int x, Object dst);
		ImageComponent2D getComponentZ(int i);
		ImageComponent2D getComponentY(int i);
		ImageComponent2D getComponentX(int i);
	}

	/*
	 * This class loads bytes from byte data.
	 */
	private final class ByteLoader extends Volume.ByteLoader implements Loader {
		ByteLoader(ImagePlus imp) {
			super(imp);
		}

		public ImageComponent2D getComponentZ(int i) {
			byte[] data = new byte[xTexSize * yTexSize];
			loadZ(i, data);
			return createGreyComponent(data, xTexSize, yTexSize);
		}

		public ImageComponent2D getComponentY(int i) {
			byte[] data = new byte[xTexSize * zTexSize];
			loadY(i, data);
			return createGreyComponent(data, xTexSize, zTexSize);
		}

		public ImageComponent2D getComponentX(int i) {
			byte[] data = new byte[yTexSize * zTexSize];
			loadX(i, data);
			return createGreyComponent(data, yTexSize, zTexSize);
		}

		public void loadZ(int zValue, Object arr) {
			byte[] dst = (byte[])arr;
			byte[] src = fData[zValue];
			for (int y=0; y < yDim; y++){
				int offsSrc = y * xDim;
				int offsDst = y * xTexSize;
				System.arraycopy(src, offsSrc, dst, offsDst, xDim);
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
				System.arraycopy(src, offsSrc, dst, offsDst, xDim);
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

		public ImageComponent2D getComponentZ(int i) {
			byte[] data = new byte[xTexSize * yTexSize * 4];
			loadZ(i, data);
			return createColorComponent(data, xTexSize, yTexSize);
		}

		public ImageComponent2D getComponentY(int i) {
			byte[] data = new byte[xTexSize * zTexSize * 4];
			loadY(i, data);
			return createColorComponent(data, xTexSize, zTexSize);
		}

		public ImageComponent2D getComponentX(int i) {
			byte[] data = new byte[yTexSize * zTexSize * 4];
			loadX(i, data);
			return createColorComponent(data, yTexSize, zTexSize);
		}

		public void loadZ(int zValue, Object arr) {
			byte[] dst = (byte[])arr;
			int[] src = fData[zValue];
			for (int y=0; y < yDim; y++){
				int offsSrc = y * xDim;
				int offsDst = y * xTexSize * 4;
				for(int x = 0; x < xDim; x++) {
					int c = src[offsSrc + x];
					int r = (c & 0xff0000) >> 16;
					int g = (c & 0xff00) >> 8;
					int b = c & 0xff;
					int a = Math.min(255, r + g + b);
					dst[offsDst++] = (byte)r;
					dst[offsDst++] = (byte)g;
					dst[offsDst++] = (byte)b;
					dst[offsDst++] = (byte)a;
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
				int[] src = fData[z];
				int offsSrc = yValue * xDim;
				int offsDst = z * xTexSize * 4;
				for(int x = 0; x < xDim; x++) {
					int c = src[offsSrc + x];
					int r = (c & 0xff0000) >> 16;
					int g = (c & 0xff00) >> 8;
					int b = c & 0xff;
					int a = Math.min(255, r + g + b);
					dst[offsDst++] = (byte)r;
					dst[offsDst++] = (byte)g;
					dst[offsDst++] = (byte)b;
					dst[offsDst++] = (byte)a;
				}
			}
		}

		/* 
		 * this routine loads values for constant xValue, into 
		 * byteData in y,z order (y changes fastest)
		 */
		public void loadX(int xValue, Object arr)  {
			byte[] dst = (byte[])arr;
			for (int z=0; z < zDim; z++){
				int offsDst = z * yTexSize * 4;
				for (int y=0; y < yDim; y++){
					int offsSrc = y * xDim + xValue;
					int c = fData[z][offsSrc];
					int r = (c & 0xff0000) >> 16;
					int g = (c & 0xff00) >> 8;
					int b = c & 0xff;
					int a = Math.min(255, r + g + b);
					dst[offsDst++] = (byte)r;
					dst[offsDst++] = (byte)g;
					dst[offsDst++] = (byte)b;
					dst[offsDst++] = (byte)a;
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

		public ImageComponent2D getComponentZ(int i) {
			byte[] data = new byte[xTexSize * yTexSize * 4];
			loadZ(i, data);
			return createColorComponent(data, xTexSize, yTexSize);
		}

		public ImageComponent2D getComponentY(int i) {
			byte[] data = new byte[xTexSize * zTexSize * 4];
			loadY(i, data);
			return createColorComponent(data, xTexSize, zTexSize);
		}

		public ImageComponent2D getComponentX(int i) {
			byte[] data = new byte[yTexSize * zTexSize * 4];
			loadX(i, data);
			return createColorComponent(data, yTexSize, zTexSize);
		}

		public void loadZ(int zValue, Object arr) {
			byte[] dst = (byte[])arr;
			int[] src = fData[zValue];
			for (int y=0; y < yDim; y++){
				int offsSrc = y * xDim;
				int offsDst = y * xTexSize * 4;
				for (int x=0; x < xDim; x++){
					int c = src[offsSrc + x];
					int a = (c & 0xff000000) >> 24;
					int r = (c & 0xff0000) >> 16;
					int g = (c & 0xff00) >> 8;
					int b = c & 0xff;
					dst[offsDst++] = ch[0] ? (byte)r : 0;
					dst[offsDst++] = ch[1] ? (byte)g : 0;
					dst[offsDst++] = ch[2] ? (byte)b : 0;
					dst[offsDst++] = (byte)a;
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
				int[] src = fData[z];
				int offsSrc = yValue * xDim;
				int offsDst = z * xTexSize * 4;
				for (int x=0; x < xDim; x++){
					int c = src[offsSrc + x];
					int a = (c & 0xff000000) >> 24;
					int r = (c & 0xff0000) >> 16;
					int g = (c & 0xff00) >> 8;
					int b = c & 0xff;
					dst[offsDst++] = ch[0] ? (byte)r : 0;
					dst[offsDst++] = ch[1] ? (byte)g : 0;
					dst[offsDst++] = ch[2] ? (byte)b : 0;
					dst[offsDst++] = (byte)a;
				}
			}
		}

		/* 
		 * this routine loads values for constant xValue, into 
		 * byteData in y,z order (y changes fastest)
		 */
		public void loadX(int xValue, Object arr)  {
			byte[] dst = (byte[])arr;
			for (int z=0; z < zDim; z++){
				int offsDst = z * yTexSize * 4;
				for (int y=0; y < yDim; y++){
					int offsSrc = y * xDim + xValue;
					int c = fData[z][offsSrc];
					int a = (c & 0xff000000) >> 24;
					int r = (c & 0xff0000) >> 16;
					int g = (c & 0xff00) >> 8;
					int b = c & 0xff;
					dst[offsDst++] = ch[0] ? (byte)r : 0;
					dst[offsDst++] = ch[1] ? (byte)g : 0;
					dst[offsDst++] = ch[2] ? (byte)b : 0;
					dst[offsDst++] = (byte)a;
				}
			}
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

		public ImageComponent2D getComponentZ(int i) {
			byte[] data = new byte[xTexSize * yTexSize];
			loadZ(i, data);
			return createGreyComponent(data, xTexSize, yTexSize);
		}

		public ImageComponent2D getComponentY(int i) {
			byte[] data = new byte[xTexSize * zTexSize];
			loadY(i, data);
			return createGreyComponent(data, xTexSize, zTexSize);
		}

		public ImageComponent2D getComponentX(int i) {
			byte[] data = new byte[yTexSize * zTexSize];
			loadX(i, data);
			return createGreyComponent(data, yTexSize, zTexSize);
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
