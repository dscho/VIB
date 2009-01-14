package voltex;

import ij.ImagePlus;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

abstract public class Renderer implements VolRendConstants {

	protected ImagePlus image;
	protected int threshold = 0;
	protected Volume volume;

	public Renderer(ImagePlus image) {
		this.image = image;
		volume = new Volume(image);
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

	abstract public void setChannels(boolean[] channels);

	abstract public void setColor(Color3f color);

	/** 
	 * return the eye's position in <node>'s coordinate space
	 */
	private static Transform3D parentInv = new Transform3D();
	private static Point3d viewPosition = new Point3d();
	private static Transform3D t = new Transform3D();

	public static Point3d getViewPosInLocal(View view, Node node) {
		if (node == null )
			return null;
		if (!node.isLive()) 
			return null;
		//  get viewplatforms's location in virutal world
		Canvas3D canvas = (Canvas3D)view.getCanvas3D(0);
		canvas.getCenterEyeInImagePlate(viewPosition);
		canvas.getImagePlateToVworld(t);
		t.transform(viewPosition);

		// get parent transform
		node.getLocalToVworld(parentInv);
		parentInv.invert();

		// transform the eye position into the parent's coordinate system
		parentInv.transform(viewPosition);

		return viewPosition;
	}
}
