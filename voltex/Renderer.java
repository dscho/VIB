package voltex;

import ij.ImagePlus;
import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import java.text.NumberFormat;
import com.sun.j3d.utils.behaviors.mouse.*;

abstract public class Renderer implements VolRendConstants {

	protected ImagePlus image;
	protected Volume volume;
	protected int threshold = 0;

	public Renderer(ImagePlus image) {
		this.image = image;
		this.volume = new Volume(image);
	}

	public int  getThreshold() {
		return threshold;
	}

	abstract public void setThreshold(int d);

	/** 
	 * Retrieve the branchgroup of this renderer
	 */
	abstract public BranchGroup getVolumeNode();

	/**
	 * Called to make changes to the renderer state
	 */
	abstract public void fullReload();

	/**
	 * Called when the view position relative to the renderer changes
	 */
	abstract public void eyePtChanged(View view);

	abstract public void setTransparency(float transparency);

	abstract public void setColorModel(IndexColorModel cmodel);

	abstract public void setColor(Color3f color);


	/** 
	 * return the eye's position in <node>'s coordinate space
	 */
	Point3d getViewPosInLocal(View view, Node node) {

		Point3d viewPosition = new Point3d();
		Vector3d translate = new Vector3d();
		if (node == null ){
			return null;
		}
		if (!node.isLive()) {
			return null;
		}
		//  get viewplatforms's location in virutal world
		Canvas3D canvas = (Canvas3D)view.getCanvas3D(0);
		canvas.getCenterEyeInImagePlate(viewPosition);
		Transform3D t = new Transform3D();
		canvas.getImagePlateToVworld(t);
		t.transform(viewPosition);

		// get parent transform
		Transform3D parentInv = new Transform3D();
		node.getLocalToVworld(parentInv);
		parentInv.invert();

		// transform the eye position into the parent's coordinate system
		parentInv.transform(viewPosition);

		return viewPosition;
	}
}
