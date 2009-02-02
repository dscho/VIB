package textureByRef;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import voltex.VoltexVolume;

public class EditableVolume extends VoltexVolume {
	
	private byte[][] xy;
	private byte[][] xz;
	private byte[][] yz;

	private ImageComponent2D[] xyComp;
	private ImageComponent2D[] xzComp;
	private ImageComponent2D[] yzComp;

	private ColorModel cm;
	private SampleModel sm;
	
	private ImageUpdater updater = new ImageUpdater();
	
	/**
	 * Initializes this Volume with the specified image.
	 * All channels are used.
	 * @param imp
	 */
	public EditableVolume(ImagePlus imp) {
		this(imp, new boolean[] {true, true, true});
	}

	/**
	 * Initializes this Volume with the specified image and channels.
	 * @param imp
	 * @param ch A boolean[] array of length three, which indicates whether
	 * the red, blue and green channel should be read. This has only an
	 * effct when reading color images.
	 */
	public EditableVolume(ImagePlus imp, boolean[] ch) {
		super(makePowerOfTwo(imp), ch);
		xy = new byte[zDim][];
		xz = new byte[yDim][xDim * zDim];
		yz = new byte[xDim][yDim * zDim];
		xyComp = new ImageComponent2D[zDim];
		xzComp = new ImageComponent2D[yDim];
		yzComp = new ImageComponent2D[xDim];
		
		ImageStack stack = imp.getStack();
		for(int z = 0; z < zDim; z++)
			xy[z] = (byte[])stack.getPixels(z+1);
		
		updateData();

		cm = getGreyColorModel();
		WritableRaster wr = cm.createCompatibleWritableRaster(1, 1);
		sm = wr.getSampleModel();
		
		for(int z = 0; z < zDim; z++)
			xyComp[z] = createImageComponent(xy[z], xDim, yDim);
		for(int y = 0; y < yDim; y++)
			xzComp[y] = createImageComponent(xz[y], xDim, zDim);
		for(int x = 0; x < xDim; x++)
			yzComp[x] = createImageComponent(yz[x], yDim, zDim);
	}
	
	public ImageComponent2D getImageComponentZ(int index) {
		return xyComp[index];
	}
	
	public ImageComponent2D getImageComponentY(int index) {
		return xzComp[index];
	}
	
	public ImageComponent2D getImageComponentX(int index) {
		return yzComp[index];
	}
	
	public final void setNoCheck(int x, int y, int z, int v) {
		xy[z][y * xDim + x] = (byte)v;
		xz[y][z * xDim + x] = (byte)v;
		yz[x][z * yDim + y] = (byte)v;	
		xyComp[z].updateData(updater, x, y, 1, 1);
		xzComp[y].updateData(updater, x, z, 1, 1);
		yzComp[x].updateData(updater, y, z, 1, 1);
	}
	
	@Override
	public final void set(int x, int y, int z, int v) {
		if(x >= 0 && x < xDim && y >= 0 && y < yDim && z >= 0 && z < zDim)
			setNoCheck(x, y, z, v);
	}
	
	void updateData() {
		for(int y = 0; y < yDim; y++)
			loadY(y, xz[y]);
		for(int x = 0; x < xDim; x++)
			loadX(x, yz[x]);
	}

	private ImageComponent2D createImageComponent(byte[] pix, int w, int h) {
		DataBufferByte db = new DataBufferByte(pix, w * h, 0);
		SampleModel smod = sm.createCompatibleSampleModel(w, h);
		WritableRaster raster = Raster.createWritableRaster(smod, db, null);

		BufferedImage bImage = new BufferedImage(cm, raster, false, null);
		ImageComponent2D bComp = new ImageComponent2D(
				ImageComponent.FORMAT_CHANNEL8, w, h, true, true);
		bComp.setCapability(ImageComponent.ALLOW_IMAGE_WRITE);
		bComp.set(bImage);
		return bComp;
	}

	private static ImagePlus makePowerOfTwo(ImagePlus image) {
		int w_old = image.getWidth();
		int h_old = image.getHeight();
		int d_old = image.getStackSize();
		
		int w_new = powerOfTwo(w_old);
		int h_new = powerOfTwo(h_old);
		int d_new = powerOfTwo(d_old);
		
		ImageStack oldStack = image.getStack();
		ImageStack newStack = new ImageStack(w_new, h_new);
		ImageProcessor ip_old = null, ip_new = null;
		int z = 0;
		for(z = 0; z < d_old; z++) {
			ip_old = oldStack.getProcessor(z+1);
			ip_new = ip_old.createProcessor(w_new, h_new);
			ip_new.insert(ip_old, 0, 0);
			newStack.addSlice("", ip_new);
		}
		for(; z < d_new; z++) {
			newStack.addSlice("", ip_old.createProcessor(w_new, h_new));
		}
		image.setStack(null, newStack);
		return image;
	}

	private static IndexColorModel getGreyColorModel() {
		byte[] r = new byte[256], g = new byte[256], b = new byte[256];
		for(int i = 0; i < 256; i++) {
			r[i] = (byte)i;
		}
		return new IndexColorModel(8, 256, r, g, b);
	}

	private class ImageUpdater implements ImageComponent2D.Updater {
		public void updateData(ImageComponent2D comp, int x, int y, int w, int h) {
		}
	}
}
