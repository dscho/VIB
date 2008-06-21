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

public class OrthoGroup extends VoltexGroup {

	public OrthoGroup(Content c) {
		super();
		this.c = c;
		ImagePlus imp = c.getResamplingFactor() == 1 ? c.getImage() 
			: Resample_.resample(c.getImage(),
				c.getResamplingFactor());
		renderer = new Orthoslice(imp, c.getColor(), 
				c.getTransparency(), c.getChannels());
		renderer.fullReload();
		oldColor = c.getColor();
		addChild(renderer.getVolumeNode());
		calculateMinMaxCenterPoint();
	}

	public void setSlice(int axis, int v) {
		((Orthoslice)renderer).setSlice(axis, v);
	}

	public void decrease(int axis) {
		((Orthoslice)renderer).decrease(axis);
	}

	public void increase(int axis) {
		((Orthoslice)renderer).increase(axis);
	}

	public void setSlices(int[] v) {
		((Orthoslice)renderer).setSlices(v);
	}

	public int[] getSlices() {
		return ((Orthoslice)renderer).getSlices();
	}

	public boolean[] getVisible() {
		return ((Orthoslice)renderer).getVisible();
	}

	public boolean isVisible(int i) {
		return ((Orthoslice)renderer).isVisible(i);
	}

	public void setVisible(boolean[] b) {
		((Orthoslice)renderer).setVisible(b);
	}

	public void setVisible(int axis, boolean b) {
		((Orthoslice)renderer).setVisible(axis, b);
	}
}

