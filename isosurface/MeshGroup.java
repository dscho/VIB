package isosurface;

import java.util.List;
import java.awt.Color;
import ij.IJ;

import ij3d.Content;
import ij3d.ContentNode;

import marchingcubes.MCTriangulator;

import javax.media.j3d.View;
import javax.vecmath.Point3f;
import javax.vecmath.Color3f;

public class MeshGroup extends ContentNode {

	public static final int TRIANGLES = 0;
	public static final int LINES = 1;
	public static final int LINE_STRIPS = 2;

	IsoShape shape; 
	Triangulator triangulator = new MCTriangulator();
	Content c;

	public MeshGroup (Content c) {
		super();
		this.c = c;
		Color3f color = c.getColor();
		List mesh = triangulator.getTriangles(c.getImage(), 
			c.getThreshold(), c.getChannels(), 
			c.getResamplingFactor());
		if(color == null) {
			int value = c.getImage().getProcessor().
				getColorModel().getRGB(c.getThreshold());
			color = new Color3f(new Color(value));
		}
		shape = new IsoShape(mesh, color, 
			c.getThreshold(), c.getTransparency());
		calculateMinMaxCenterPoint();
		addChild(shape);
	}

	public MeshGroup(Content c, List mesh) {
		this(c, mesh, TRIANGLES);
	}

	public MeshGroup(Content c, List mesh, int mode) {
		super();
		this.c = c;
		Color3f color = c.getColor();
		int thresh = c.getThreshold();
		if(color == null) {
			color= new Color3f(
				thresh/255f, thresh/255f, thresh/255f);
		}
		shape = new IsoShape(mesh, color, thresh, c.getTransparency(), mode);
		calculateMinMaxCenterPoint();
		addChild(shape);
	}

	public void eyePtChanged(View view) {
		// do nothing
	}

	public void thresholdUpdated() {
		if(c.getImage() == null) {
			IJ.error("Mesh was not calculated of a grayscale " +
				"image. Can't change threshold");
			return;
		}
		List mesh = triangulator.getTriangles(c.getImage(), 
			c.getThreshold(), c.getChannels(), 
			c.getResamplingFactor());
		shape.mesh = mesh;
		shape.update();
	}

	public void channelsUpdated() {
		if(c.getImage() == null) {
			IJ.error("Mesh was not calculated of a grayscale " +
				"image. Can't change channels");
			return;
		}
		List mesh = triangulator.getTriangles(c.getImage(), 
			c.getThreshold(), c.getChannels(), 
			c.getResamplingFactor());
		shape.mesh = mesh;
		shape.update();
	}

	public void calculateMinMaxCenterPoint() {
		min = new Point3f(); max = new Point3f();
		center = new Point3f();
		if(shape != null) {
			shape.calculateMinMaxCenterPoint(min, max, center);
		}
	}

	public float getVolume() {
		if(shape == null)
			return -1;
		return shape.getVolume();
	}

	public void shadeUpdated() {
		if(shape.isShaded() != c.isShaded())
			shape.setShaded(c.isShaded());
	}

	public void colorUpdated() {
		Color3f newColor = c.getColor();
		if(newColor == null){
			int val = c.getImage().getProcessor().
				getColorModel().getRGB(c.getThreshold());
			newColor = new Color3f(new Color(val));
		}
		shape.setColor(newColor);	
	}

	public void transparencyUpdated() {
		shape.setTransparency(c.getTransparency());
	}
}

