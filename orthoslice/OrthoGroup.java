package orthoslice;

import java.awt.Polygon;
import java.awt.image.ColorModel;
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
import ij.process.ImageProcessor;

import ij3d.Content;
import ij3d.ContentNode;
import ij3d.ImageCanvas3D;
import ij3d.Image3DUniverse;
import ij3d.ColorTable;

import voltex.VolRendConstants;

import vib.Resample_;
import voltex.*;

public class OrthoGroup extends ContentNode {

	private Renderer renderer;
	private Orthoslice ortho;
	private float volume;
	private Color3f oldColor;
	private Content c;

	public OrthoGroup(Content c) {
		super();
		this.c = c;
		float scale = c.getImage().getWidth() * 
			(float)c.getImage().getCalibration().pixelWidth;

		ImagePlus imp = c.getResamplingFactor() == 1 ? c.getImage() 
			: Resample_.resample(c.getImage(), c.getResamplingFactor());
		renderer = new Orthoslice(imp, c.getColor(), 
				c.getTransparency(), c.getChannels());
		ortho = (Orthoslice)renderer;
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
			ImageProcessor ip = imp.getStack().getProcessor(zi+1);
			int wh = w * h;
			for(int i = 0; i < wh; i++) {
				int v = ip.get(i);
				if(v == 0) continue;
				vol += v;
				float x = (i % w) * (float)c.pixelWidth;
				float y = (i / w) * (float)c.pixelHeight;
				if(x < min.x) min.x = x;
				if(y < min.y) min.y = y;
				if(z < min.z) min.z = z;
				if(x > max.x) max.x = x;
				if(y > max.y) max.y = y;
				if(z > max.z) max.z = z;
				center.x += v * x;
				center.y += v * y;
				center.z += v * z;
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
		renderer.setChannels(c.getChannels());
	}

	public void shadeUpdated() {
		// do nothing
	}

	public void colorUpdated() {
		renderer.setColor(c.getColor());
	}

	public void transparencyUpdated() {
		renderer.setTransparency(c.getTransparency());
	}

	public void setSlice(int axis, int v) {
		ortho.setSlice(axis, v);
	}

	public void decrease(int axis) {
		ortho.decrease(axis);
	}

	public void increase(int axis) {
		ortho.increase(axis);
	}

	public void setSlices(int[] v) {
		ortho.setSlices(v);
	}

	public int[] getSlices() {
		return ortho.getSlices();
	}

	public boolean[] getVisible() {
		return ortho.getVisible();
	}

	public boolean isVisible(int i) {
		return ortho.isVisible(i);
	}

	public void setVisible(boolean[] b) {
		ortho.setVisible(b);
	}

	public void setVisible(int axis, boolean b) {
		ortho.setVisible(axis, b);
	}
}

