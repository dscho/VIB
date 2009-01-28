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

import customnode.CustomTriangleMesh;

public class MeshGroup extends ContentNode {

	private CustomTriangleMesh mesh;
	private Triangulator triangulator = new MCTriangulator();
	private Content c;

	public MeshGroup (Content c) {
		super();
		this.c = c;
		Color3f color = c.getColor();
		List tri = triangulator.getTriangles(c.getImage(),
			c.getThreshold(), c.getChannels(),
			c.getResamplingFactor());
		if(color == null) {
			int value = c.getImage().getProcessor().
				getColorModel().getRGB(c.getThreshold());
			color = new Color3f(new Color(value));
		}
		mesh = new CustomTriangleMesh(tri, color, c.getTransparency());
		calculateMinMaxCenterPoint();
		addChild(mesh);
	}

	public CustomTriangleMesh getMesh() {
		return mesh;
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
		List tri = triangulator.getTriangles(c.getImage(),
				c.getThreshold(), c.getChannels(),
				c.getResamplingFactor());
		mesh.setMesh(tri);
	}

	public void channelsUpdated() {
		if(c.getImage() == null) {
			IJ.error("Mesh was not calculated of a grayscale " +
				"image. Can't change channels");
			return;
		}
		List tri = triangulator.getTriangles(c.getImage(),
			c.getThreshold(), c.getChannels(),
			c.getResamplingFactor());
		mesh.setMesh(tri);
	}

	public void calculateMinMaxCenterPoint() {
		min = new Point3f(); max = new Point3f();
		center = new Point3f();
		if(mesh != null) {
			mesh.calculateMinMaxCenterPoint(min, max, center);
		}
	}

	public float getVolume() {
		if(mesh == null)
			return -1;
		return mesh.getVolume();
	}

	public void shadeUpdated() {
		mesh.setShaded(c.isShaded());
	}

	public void colorUpdated() {
		Color3f newColor = c.getColor();
		if(newColor == null){
			int val = c.getImage().getProcessor().
				getColorModel().getRGB(c.getThreshold());
			newColor = new Color3f(new Color(val));
		}
		mesh.setColor(newColor);
	}

	public void transparencyUpdated() {
		mesh.setTransparency(c.getTransparency());
	}
}

