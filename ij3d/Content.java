package ij3d;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;

import java.awt.image.IndexColorModel;

import isosurface.IsoShape;
import javax.media.j3d.*;
import javax.vecmath.Color3f;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;

public abstract class Content extends BranchGroup {

	String name;
	Color3f color;
	ImagePlus image;
	boolean[] channels = new boolean[]{true, true, true};
	float transparency = 0f;
	int resamplingF = 1;
	Transform3D initialTransform;
	protected boolean selected;
	
	protected TransformGroup initialTG;
	protected TransformGroup pickTG;

	public Content() {
		// create BranchGroup for this image
		setCapability(BranchGroup.ALLOW_DETACH);
		setCapability(BranchGroup.ENABLE_PICK_REPORTING);

		this.name = name;

		// create transformation for pickeing
		pickTG = new TransformGroup();
		pickTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		pickTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		addChild(pickTG);
	}

	public Content(String name, Color3f color, Transform3D initial) {
		this();
		this.name = name;
		this.color = color;
		this.initialTransform = initial;
		initialTG = new TransformGroup(initial);
		pickTG.addChild(initialTG);
	}

	public Content(String name, Color3f color, ImagePlus image, boolean[] 
		channels, int resamplingF, Transform3D initialTransform) {
		
		this(name, color, initialTransform);
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

	public void setTransform(Transform3D transform) {
		pickTG.setTransform(transform);
	}

	public void setChannels(boolean[] channels) {
		boolean channelsChanged = channels[0] != this.channels[0] || 
				channels[1] != this.channels[1] || 
				channels[2] != this.channels[2];
		if(!channelsChanged)
			return;
		this.channels = channels;
		channelsUpdated(channels);
	}
		

	public void setColor(Color3f color) {
		boolean colorChanged = !(this.color == null && color == null)
			|| (this.color == null && color != null)
			|| (color == null && this.color != null) 
			|| !(this.color.equals(color));
		if(!colorChanged)
			return;
		Color3f oldColor = this.color;
		this.color = color;
		colorUpdated(oldColor, color);
	}

	public void setTransparency(float transparency) {
		transparency = transparency < 0 ? 0 : transparency;
		transparency = transparency > 1 ? 1 : transparency;
		if(Math.abs(transparency - this.transparency) < 0.01)
			return;
		this.transparency = transparency;
		transparencyUpdated(transparency);
	}

	public void resetView() {
		pickTG.setTransform(new Transform3D());
	}

	public String getName() {
		return name;
	}

	public ImagePlus getImage() {
		return image;
	}

	public boolean[] getChannels() {
		return channels;
	}

	public Color3f getColor() {
		return color;
	}

	public float getTransparency() {
		return transparency;
	}

	public int getResamplingFactor() {
		return resamplingF;
	}

	public TransformGroup getPickTG() {
		return pickTG;
	}

	public TransformGroup getInitialTG() {
		return initialTG;
	}

	public Transform3D getInitialTransform() {
		return initialTransform;
	}

	public abstract void eyePtChanged(View view);
	public abstract void colorUpdated(Color3f oldColor, Color3f newColor);
	public abstract void channelsUpdated(boolean[] channels);
	public abstract void transparencyUpdated(float transparency);
}


