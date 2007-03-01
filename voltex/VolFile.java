package voltex;

import ij.ImageStack;
import ij.ImagePlus;
import ij.measure.Calibration;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.event.*;
import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.Box;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.WritableRaster;
import java.awt.image.DataBufferByte;
import java.awt.color.ColorSpace;
import java.awt.Transparency;
import java.io.*;
import java.net.*;


/**
 * A VolFile is object holds the volume data from a file.
 */
public class VolFile {

    int xDim = 0, yDim = 0, zDim = 0;
	float xSpace = 0, ySpace = 0, zSpace = 0;
    byte[][][] fileData;
    

	public VolFile(ImagePlus imp) {
		ImageStack stack = imp.getStack();
		Calibration c = imp.getCalibration();
		xSpace = (float)c.pixelWidth;
		ySpace = (float)c.pixelHeight;
		zSpace = (float)c.pixelDepth;
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
	}
}
