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

public class Volume implements VolRendConstants {

	private ImagePlus imp;

	public int xDim = 0, yDim = 0, zDim = 0;
	public float xSpace = 0, ySpace = 0, zSpace = 0;
	public int xTexSize, yTexSize, zTexSize;
	public float xTexGenScale, yTexGenScale, zTexGenScale;

	Point3d minCoord = new Point3d();
	Point3d maxCoord = new Point3d();
	Point3d volRefPt = new Point3d();

	public Volume(ImagePlus imp) {
		this.imp = imp;
		ImageStack stack = imp.getStack();
		int d = imp.getStackSize();
		fData = new byte[d][];
		for (int z = 0; z < d; z++)
			fData[z] = (byte[])stack.getPixels(z+1);
	}

	public void update() {
		Calibration c = imp.getCalibration();
		ImageStack stack = imp.getStack();
		xDim = stack.getWidth();
		yDim = stack.getHeight();
		zDim = stack.getSize();
		int type = imp.getType();
		if(type != ImagePlus.GRAY8 && type != ImagePlus.COLOR_256){
			IJ.error("8 bit image required");
		}

		// tex size is next power of two greater than max - min
		// regarding pixels
		xTexSize = powerOfTwo(xDim);
		yTexSize = powerOfTwo(yDim);
		zTexSize = powerOfTwo(zDim);

		xSpace = (float)imp.getCalibration().pixelWidth;
		ySpace = (float)imp.getCalibration().pixelHeight;
		zSpace = (float)imp.getCalibration().pixelDepth;
		
		// real coords
		maxCoord.x = xDim * xSpace;
		maxCoord.y = yDim * ySpace;
		maxCoord.z = zDim * zSpace;

		// xTexSize is the pixel dim of the file in x-dir, e.g. 256
		// xSpace is the normalised length of a pixel
		xTexGenScale = (float)(1.0 / (xSpace * xTexSize));
		yTexGenScale = (float)(1.0 / (ySpace * yTexSize));
		zTexGenScale = (float)(1.0 / (zSpace * zTexSize));

		// the min and max coords are for the usable area of the texture,
		// which is has a half-texel boundary.  Otherwise the boundary
		// gets sampled, leading to artifacts with a texture color table.
// 		minCoord.x = 0.5f * xSpace;
// 		minCoord.y = 0.5f * ySpace;
// 		minCoord.z = 0.5f * zSpace;
// 
// 		maxCoord.x = (xDim - 0.5f) * xSpace;
// 		maxCoord.y = (yDim - 0.5f) * ySpace;
// 		maxCoord.z = (zDim - 0.5f) * zSpace;


		// TODO: how to set here, but not clobber value from restore()?
		// perhaps set in VolRend?
		volRefPt.x = (maxCoord.x + minCoord.x) / 2;
		volRefPt.y = (maxCoord.y + minCoord.y) / 2;
		volRefPt.z = (maxCoord.z + minCoord.z) / 2;
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

	// Note:
	// Java3D "flips" images along the "t" axis, so we load the images into
	// the buffer from the "top" down.  That is, we use (numRows - row - 1)
	// instead of (row).

	// load byteData with Intensity values

	byte[][] fData;
	void loadZ(int zValue, byte[] dst) {
		byte[] src = fData[zValue];
		for (int y=0; y < yDim; y++){
			int offsSrc = y * xDim;
			int offsDst = y * xTexSize;
			System.arraycopy(src, offsSrc, dst, offsDst, xDim);
		}
	}

	// this routine loads values for constant yValue, the texture map is
	// stored in x,z format (x changes fastest)
	void loadY(int yValue, byte[] dst)  {
		for (int z=0; z < zDim; z++){
			byte[] src = fData[z];
			int offsSrc = yValue * xDim;
			int offsDst = z * xTexSize;
			System.arraycopy(src, offsSrc, dst, offsDst, xDim);
		}
	}

	// this routine loads values for constant xValue, into byteData in y,z
	// order (y changes fastest)
	void loadX(int xValue, byte[] dst)  {
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
