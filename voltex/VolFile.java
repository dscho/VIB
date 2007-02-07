package voltex;

import ij.ImageStack;
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
    
    private int dataOffset = 0;
    private int bytesPerVoxel = 0;
    private byte[] id = new byte[64];
    private int pos = 0;
    private DataInputStream file = null;


	public VolFile(ImageStack stack) {
		xSpace = 1.0f;
		ySpace = 1.0f;
		zSpace = 2.0f;
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


    /**
     * Creates a VolFile from a URL.
     * @param voldat  The URL for a .vol file holding the volume
     */
	/*
    public VolFile(URL voldat) throws java.io.IOException {
		try {
			 file = new DataInputStream(voldat.openStream());
		} catch(FileNotFoundException fnf) {
			 System.out.println(fnf.getMessage()); 
		}
		byte[] magicBuffer = new byte[4];
		file.read(magicBuffer, 0, 4);
		pos += 4;
		String magic = new String(magicBuffer, 0, 4); 
		if (!magic.equals("vol3")) {
			System.out.println("file specified is not a .vol file" +
			" header begins with " + magic);
			System.exit(0);
		}
		xDim = file.readInt();
		pos += 4;
		yDim = file.readInt();
		pos += 4;
		zDim = file.readInt();
		pos += 4;
		xSpace = file.readFloat();
		pos += 4;
		ySpace = file.readFloat();
		pos += 4;
		zSpace = file.readFloat();
		pos += 4;
		dataOffset = file.readInt();
		pos += 4;
		minVal = (int)file.readShort();
		pos += 2;
		maxVal = (int)file.readShort();
		pos += 2;
		bytesPerVoxel = (int)file.readByte();
		pos++ ;
		System.out.println("xDim = " + xDim);
		System.out.println("yDim = " + yDim);
		System.out.println("zDim = " + zDim);
		System.out.println("xSpace = " + xSpace);
		System.out.println("ySpace = " + ySpace);
		System.out.println("zSpace = " + zSpace);
		System.out.println();
		System.out.println();
		dummybyte = file.readByte();
		pos ++ ;
		for (int i = 0; i < 13; i++) {
			dummyshort = file.readShort();
		pos += 2;
		}
		file.read(id, 0, 64);
		pos += 64;

		fileData = new byte[zDim][yDim][xDim];
		if (pos != dataOffset) System.out.print("VolFile: Pointer Mismatch");
		if (bytesPerVoxel == 1) {
			System.out.print("Reading data...");
			for (int z = 0; z < zDim; z++) {
			for (int y = 0; y < yDim; y++) {
				int vIndex = (z * xDim * yDim + y * xDim);
				byte[] dataRow = fileData[z][y];
				file.readFully(dataRow, 0, xDim);
			}
			}
			System.out.println("done");
		}
    }*/

    /**
     * Returns the ID string for the volume file
     */
    private String getId() {
		return new String(id);
    }
}
