package vis3d;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;

import java.awt.image.IndexColorModel;

import isosurface.IsoShape;
import javax.media.j3d.*;
import javax.vecmath.Color3f;

public abstract class Content extends BranchGroup {

	String name;
	String color;
	ImagePlus image;
	boolean[] channels = new boolean[]{true, true, true};
	int resamplingF = 1;;
	protected boolean selected;
	protected boolean channelsChanged;
	
	protected TransformGroup pickTr;

	public Content() {
		// create BranchGroup for this image
		setCapability(BranchGroup.ALLOW_DETACH);
		setCapability(BranchGroup.ENABLE_PICK_REPORTING);

		this.name = name;

		// create transformation for pickeing
		pickTr = new TransformGroup();
		pickTr.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		pickTr.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		pickTr.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		addChild(pickTr);
	}

	public Content(String name, String color) {
		this();
		this.name = name;
		this.color = color;
	}

	public Content(String name, String color, 
			ImagePlus image, boolean[] channels, int resamplingF) {
		this(name, color);
		this.image = image;
		this.channels = channels;
		this.resamplingF = resamplingF;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public void setColor(String color, boolean[] channels) {
		channelsChanged = channels[0] != this.channels[0] || 
							channels[1] != this.channels[1] || 
							channels[2] != this.channels[2];
		boolean colorChanged = !color.equals(this.color);
		if(!colorChanged && !channelsChanged)
			return;
		this.color = color;
		this.channels = channels;
		colorUpdated(color, channels);
	}

	public ImagePlus getImage() {
		return image;
	}

	public boolean[] getChannels() {
		return channels;
	}

	public int getResamplingFactor() {
		return resamplingF;
	}

	public abstract void eyePtChanged(View view);
	public abstract void colorUpdated(String color, boolean[] channels);
}


