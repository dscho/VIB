package ij3d;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.io.FileInfo;
import ij.io.OpenDialog;

import vib.BenesNamedPoint;

import java.awt.image.IndexColorModel;

import java.util.BitSet;

import isosurface.IsoShape;
import javax.media.j3d.*;
import javax.vecmath.Color3f;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;

import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3d;

public abstract class ContentNode extends BranchGroup {

	protected Point3f min, max, center;

	public ContentNode() {
		super();
		setCapability(BranchGroup.ALLOW_DETACH);
	}

	public abstract void channelsUpdated();

	public abstract void thresholdUpdated();

	public abstract void colorUpdated();

	public abstract void transparencyUpdated();

	public abstract void shadeUpdated();

	public abstract void eyePtChanged(View view);

	public abstract void calculateMinMaxCenterPoint();

	public abstract float getVolume();
}

