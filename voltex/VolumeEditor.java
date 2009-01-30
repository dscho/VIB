package voltex;

import javax.media.j3d.ImageComponent;

import ij.ImagePlus;

public abstract class VolumeEditor implements VolRendConstants {
	
	VoltexVolume volume;
	
	ImagePlus image;
	
	ImageComponent[] xy;
	
	ImageComponent[] xz;
	
	ImageComponent[] yz;
	
	private VolumeEditor() {}
	
	VolumeEditor(VoltexVolume volume) {
		this.volume = volume;
	}
	
	// package protected
	abstract void createImageComponents();
	
	ImageComponent getImageComponentXY(int axis, int i) {
		switch(axis) {
			case Z_AXIS: return xy[i];
			case Y_AXIS: return xz[i];
			case X_AXIS: return yz[i];
		}
		return null;
	}
	
	// public
	public abstract void put(int x, int y, int z, int v);
	
	public abstract void putVoxel(int x, int y, int z, int v);
	
	public abstract void setSlice(int z, Object data);
	
	public ImagePlus getImage() {
		return image;
	}
}

