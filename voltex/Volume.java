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
	private boolean	tFlip = false;
	private byte[][][] fileData;
	private Point3d[]	voiPts = new Point3d[8];

	int xDim = 0, yDim = 0, zDim = 0;
	float xSpace = 0, ySpace = 0, zSpace = 0;
	int xTexSize, yTexSize, zTexSize;
	float	xTexGenScale, yTexGenScale, zTexGenScale;
	boolean is8C = true;
	IndexColorModel cmodel = null;

	Point3d minCoord = new Point3d();
	Point3d maxCoord = new Point3d();
	Point3d[][] facePoints = new Point3d[6][];

	Point3d volRefPt = new Point3d();

	public Volume(ImagePlus imp) {
		this.imp = imp;
		ImageStack stack = imp.getStack();
		Calibration c = imp.getCalibration();
		xDim = stack.getWidth();
		yDim = stack.getHeight();
		zDim = stack.getSize();
		fileData = new byte[zDim][yDim][xDim];

		for (int z = 0; z < zDim; z++) {
			byte[] slice = (byte[])stack.getPixels(z+1);
			int offset = 0;
			for(int y = 0; y < yDim; y++) {
				byte[] datarow = fileData[z][y];
				System.arraycopy(slice, offset, datarow, 0, xDim);
				offset += xDim;
			}
		}
		int type = imp.getType();
		if(type != ImagePlus.GRAY8 && type != ImagePlus.COLOR_256){
			IJ.error("8 bit image required");
		}
		is8C = type == ImagePlus.COLOR_256;
		if(is8C) {
			adjustColorModel();
		}
		for (int i = 0; i < 8; i++) {
		   voiPts[i] = new Point3d();
		}
		for (int i = 0; i < 6; i++) {
			facePoints[i] = new Point3d[4];
		}
		
		// the rectangle at PLUS_X is the right side
		// of the VOI:
		//	____________	 PLUS_X:
		//   /3		  /|7	  ______
		//  /___________/ |	  |6	 |7
		//  |2		 6| |4	 |______|
		//  |___________|/		5	  4
		//  1		   5
		facePoints[PLUS_X][0] =  voiPts[5];
		facePoints[PLUS_X][1] =  voiPts[4];
		facePoints[PLUS_X][2] =  voiPts[7];
		facePoints[PLUS_X][3] =  voiPts[6];

		facePoints[PLUS_Y][0] =  voiPts[2];
		facePoints[PLUS_Y][1] =  voiPts[3];
		facePoints[PLUS_Y][2] =  voiPts[7];
		facePoints[PLUS_Y][3] =  voiPts[6];

		facePoints[PLUS_Z][0] =  voiPts[1];
		facePoints[PLUS_Z][1] =  voiPts[2];
		facePoints[PLUS_Z][2] =  voiPts[6];
		facePoints[PLUS_Z][3] =  voiPts[5];

		facePoints[MINUS_X][0] =  voiPts[0];
		facePoints[MINUS_X][1] =  voiPts[1];
		facePoints[MINUS_X][2] =  voiPts[2];
		facePoints[MINUS_X][3] =  voiPts[3];

		facePoints[MINUS_Y][0] =  voiPts[0];
		facePoints[MINUS_Y][1] =  voiPts[4];
		facePoints[MINUS_Y][2] =  voiPts[5];
		facePoints[MINUS_Y][3] =  voiPts[1];

		facePoints[MINUS_Z][0] =  voiPts[0];
		facePoints[MINUS_Z][1] =  voiPts[3];
		facePoints[MINUS_Z][2] =  voiPts[7];
		facePoints[MINUS_Z][3] =  voiPts[4];
	}

	public void adjustColorModel() {
		byte[] r = new byte[256];
		byte[] g = new byte[256];
		byte[] b = new byte[256];
		byte[] a = new byte[256];
		cmodel = (IndexColorModel)imp.getProcessor().getColorModel();
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		adjustAlpha(r, g, b, a);
		cmodel = new IndexColorModel(8, 256, r, g, b, a); 
	}

	float weightr = 1.0f;
	float weightg = 1.0f;
	float weightb = 1.0f;

	public void adjustAlpha(byte[]r, byte[]g, byte[]b, byte[]a) {
		for(int i=0; i<256; i++) {
			int red = (int)(r[i] & 0xff);
			int green = (int)(g[i] & 0xff);
			int blue = (int)(b[i] & 0xff);
			
			int meanInt = (int)Math.round(
				(weightr * red + weightg * green + weightb * blue)/3.0);
			
			a[i] = (byte)(meanInt);
		}
	}

	public void update() {

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
		minCoord.x = 0.5f * xSpace;
		minCoord.y = 0.5f * ySpace;
		minCoord.z = 0.5f * zSpace;

		maxCoord.x = (xDim - 0.5f) * xSpace;
		maxCoord.y = (yDim - 0.5f) * ySpace;
		maxCoord.z = (zDim - 0.5f) * zSpace;

		// setup the VOI box points
		voiPts[0].x = voiPts[1].x = voiPts[2].x = voiPts[3].x = minCoord.x;
		voiPts[4].x = voiPts[5].x = voiPts[6].x = voiPts[7].x = maxCoord.x;
		voiPts[0].y = voiPts[1].y = voiPts[4].y = voiPts[5].y = minCoord.y;
		voiPts[2].y = voiPts[3].y = voiPts[6].y = voiPts[7].y = maxCoord.y;
		voiPts[0].z = voiPts[3].z = voiPts[4].z = voiPts[7].z = minCoord.z;
		voiPts[1].z = voiPts[2].z = voiPts[5].z = voiPts[6].z = maxCoord.z;


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
	void loadZ(int zValue, byte[] byteData) {
		loadZ(zValue, byteData, 0);
	}

	void loadZ(int zValue, byte[] byteData, int byteOffset) {
		for (int y=0; y < yDim; y++){
			byte[] vRow = fileData[zValue][y];
			int rowIndex = 0;
			if (tFlip) {
				rowIndex = (yTexSize - y - 1) * xTexSize;
			} else {
				rowIndex = y * xTexSize;
			}
			System.arraycopy(vRow,0,byteData,byteOffset + rowIndex,xDim);
		}
	}

	// this routine loads values for constant yValue, the texture map is
	// stored in x,z format (x changes fastest)
	private byte[] emptyByteRow = new byte[1024];
	void loadY(int yValue, byte[] byteData)  {
		//boolean tFlip = true;
		for (int z=0; z < zDim; z++){
			int rowIndex;
			if (tFlip) {
				rowIndex = (zTexSize - z - 1) * xTexSize;
			} else {
				rowIndex = z * xTexSize;
			}
			byte[] vRow;
			if (z < zDim) {
				vRow = fileData[z][yValue];
			} else {
				vRow = emptyByteRow;
			}
			System.arraycopy(vRow, 0, byteData, rowIndex, xDim);
		}
	}

	// this routine loads values for constant xValue, into byteData in y,z
	// order (y changes fastest)
	void loadX(int xValue, byte[] byteData)  {
		for (int z=0; z < zDim; z++){
			int rowIndex;
			if (tFlip) {
				rowIndex = (zTexSize - z - 1) * yTexSize;
			} else {
				rowIndex = z * yTexSize;
			}
			for (int y=0; y < yDim; y++){
				byte value;
				value = fileData[z][y][xValue];
				int tIndex = rowIndex + y;
				try {
					byteData[tIndex] = value;
				} catch (ArrayIndexOutOfBoundsException e) {
					System.out.println("tIndex = " + tIndex +
					" byteData.length = " + byteData.length);
					System.out.println("rowIndex =  " + rowIndex);
					System.out.println("zTexSize =  " + zTexSize);
					System.out.println("xDim =  " + xDim);
					System.out.println("z =  " + z + " y = " + y);
					System.exit(0);
				}
			}
		}
	}
}
