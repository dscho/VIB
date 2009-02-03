package textureByRef;

import javax.media.j3d.ImageComponent2D;

import ij.ImagePlus;
import voltex.VoltexVolume;

public class EditableVolume extends VoltexVolume {
	
	private byte[][] xy;
	private byte[][] xz;
	private byte[][] yz;

	private ImageComponent2D[] xyComp;
	private ImageComponent2D[] xzComp;
	private ImageComponent2D[] yzComp;

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
		super(imp, ch);
		xy = new byte[zDim][xTexSize * yTexSize];
		xz = new byte[yDim][xTexSize * zTexSize];
		yz = new byte[xDim][yTexSize * zTexSize];
		xyComp = new ImageComponent2D[zDim];
		xzComp = new ImageComponent2D[yDim];
		yzComp = new ImageComponent2D[xDim];

		updateData();
		for(int z = 0; z < zDim; z++)
			xyComp[z] = createGreyComponent(xy[z], xTexSize, yTexSize);
		for(int y = 0; y < yDim; y++)
			xzComp[y] = createGreyComponent(xz[y], xTexSize, zTexSize);
		for(int x = 0; x < xDim; x++)
			yzComp[x] = createGreyComponent(yz[x], yTexSize, zTexSize);
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
		xy[z][y * xTexSize + x] = (byte)v;
		xz[y][z * xTexSize + x] = (byte)v;
		yz[x][z * yTexSize + y] = (byte)v;	
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
		for(int z = 0; z < zDim; z++)
			loadZ(z, xy[z]);
		for(int y = 0; y < yDim; y++)
			loadY(y, xz[y]);
		for(int x = 0; x < xDim; x++)
			loadX(x, yz[x]);
	}

	private class ImageUpdater implements ImageComponent2D.Updater {
		public void updateData(ImageComponent2D comp, int x, int y, int w, int h) {
		}
	}
}
