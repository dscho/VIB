package orthoslice;

import java.awt.Polygon;
import java.awt.image.IndexColorModel;

import java.util.List;
import java.util.Vector;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Point2d;
import javax.vecmath.Point3f;
import javax.vecmath.Point3d;
import javax.media.j3d.View;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.BranchGroup;
import com.sun.j3d.utils.behaviors.picking.PickingCallback;

import ij.ImagePlus;
import ij.IJ;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;

import ij3d.Content;
import ij3d.ContentNode;
import ij3d.ImageCanvas3D;
import ij3d.Image3DUniverse;
import ij3d.ColorTable;

import vib.Resample_;
import voltex.*;

public class OrthoGroup extends ContentNode {

	private Renderer renderer;
	private int[] slices;
	private float volume;
	private Color3f oldColor;
	private Content c;

	public OrthoGroup(Content c) {
		super();
		this.c = c;
		float scale = c.getImage().getWidth() * 
			(float)c.getImage().getCalibration().pixelWidth;

		IndexColorModel cmodel = c.getColor() == null ? 
			ColorTable.getOpaqueIndexedColorModel(
				c.getImage(), c.getChannels()) :
			ColorTable.getOpaqueAverageGrayColorModel(
				c.getImage(), c.getChannels());
		ImagePlus imp = c.getResamplingFactor() == 1 ? c.getImage() 
			: Resample_.resample(c.getImage(), c.getResamplingFactor());
		renderer = new Orthoslice(imp, cmodel, 
				c.getColor(), c.getTransparency());
		slices = new int[] {imp.getWidth()/2, imp.getHeight()/2,
					imp.getStackSize()/2};
		renderer.fullReload();
		oldColor = c.getColor();
		addChild(renderer.getVolumeNode());
		calculateMinMaxCenterPoint();
	}

	public void thresholdUpdated() {
		renderer.setThreshold(c.getThreshold());
	}

	public void calculateMinMaxCenterPoint() {
		ImagePlus imp = c.getImage();
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		Calibration c = imp.getCalibration();
		min = new Point3f();
		max = new Point3f();
		center = new Point3f();
		min.x = w * (float)c.pixelHeight;
		min.y = h * (float)c.pixelHeight;
		min.z = d * (float)c.pixelDepth;
		max.x = 0;
		max.y = 0;
		max.z = 0;

		long vol = 0;
		for(int zi = 0; zi < d; zi++) {
			float z = zi * (float)c.pixelDepth;
			byte[] p = (byte[])imp.getStack().getPixels(zi+1);
			for(int i = 0; i < p.length; i++) {
				if(p[i] == 0) continue;
				vol += (p[i] & 0xff);
				float x = (i % w) * (float)c.pixelWidth;
				float y = (i / w) * (float)c.pixelHeight;
				if(x < min.x) min.x = x;
				if(y < min.y) min.y = y;
				if(z < min.z) min.z = z;
				if(x > max.x) max.x = x;
				if(y > max.y) max.y = y;
				if(z > max.z) max.z = z;
				center.x += (p[i] & 0xff) * x;
				center.y += (p[i] & 0xff) * y;
				center.z += (p[i] & 0xff) * z;
			}
		}
		center.x /= vol;
		center.y /= vol;
		center.z /= vol;

		volume = (float)(vol * c.pixelWidth
				* c.pixelHeight
				* c.pixelDepth);
		
	}

	public float getVolume() {
		return volume;
	}
		
	public void eyePtChanged(View view) {
		renderer.eyePtChanged(view);
	}

	public void channelsUpdated() {
		IndexColorModel cmodel = c.getColor() == null ?
			ColorTable.getOpaqueIndexedColorModel(
				c.getImage(), c.getChannels()) :
			ColorTable.getOpaqueAverageGrayColorModel(
				c.getImage(), c.getChannels());
		renderer.setColorModel(cmodel);
	}

	public void colorUpdated() {
		// color model only needs update if there is a switch
		// between null and non-null color
		if(oldColor == null && c.getColor() != null || 
			oldColor != null && c.getColor() == null) {

			IndexColorModel cmodel = c.getColor() == null ?
				ColorTable.getOpaqueIndexedColorModel(
					c.getImage(), c.getChannels()) :
				ColorTable.getOpaqueAverageGrayColorModel(
					c.getImage(), c.getChannels());
			renderer.setColorModel(cmodel);
		}
		oldColor = c.getColor();
		renderer.setColor(c.getColor());
	}

	public void transparencyUpdated() {
		renderer.setTransparency(c.getTransparency());
	}

	public void setXSlice(int x) {
		((Orthoslice)renderer).setXSlice(x);
	}

	public void setYSlice(int y) {
		((Orthoslice)renderer).setYSlice(y);
	}

	public void setZSlice(int z) {
		((Orthoslice)renderer).setZSlice(z);
	}

	public void setSlices(int x, int y, int z) {
		((Orthoslice)renderer).setSlices(x, y, z);
	}

	public int[] getSlices() {
		return slices;
	}
}

