package surfaceplot;

import java.util.List;
import java.util.Vector;

import java.awt.Color;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;

import ij.measure.Calibration;

import ij3d.Content;
import ij3d.ContentNode;
import ij3d.Image3DUniverse;
import ij3d.ColorTable;

import vib.Resample_;

import javax.media.j3d.Node;
import javax.media.j3d.View;
import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3f;
import javax.vecmath.Color3f;

public class SurfacePlotGroup extends ContentNode {

	SurfacePlot surfacep;
	Content c;

	public SurfacePlotGroup (Content c) {
		super();
		this.c = c;
		surfacep = new SurfacePlot(c.getImage(), c.getColor(),
			c.getTransparency(), c.getResamplingFactor());
		calculateMinMaxCenterPoint();
		addChild(surfacep);
	}

	public void eyePtChanged(View view) {
		// do nothing
	}

	public void thresholdUpdated() {
		// TODO
	}

	public void channelsUpdated() {
		// TODO
	}

	public void calculateMinMaxCenterPoint() {
		min = new Point3f(); max = new Point3f();
		center = new Point3f();
		if(surfacep != null) {
			surfacep.calculateMinMaxCenterPoint(min, max, center);
		}
	}

	public float getVolume() {
		if(surfacep == null)
			return -1;
		// TODO
		return 0f;
	}

	public void colorUpdated() {
		if(c.getColor() != surfacep.getColor())
			surfacep.setColor(c.getColor());
	}

	public void transparencyUpdated() {
		if(c.getTransparency() != surfacep.getTransparency())
			surfacep.setTransparency(c.getTransparency());
	}

	public void shadeUpdated() {
		if(c.isShaded() != surfacep.isShaded())
			surfacep.setShaded(c.isShaded());
	}
}

