package ij3d;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.View;
import javax.vecmath.Tuple3d;

/**
 * Abstract class which has to be extended by classes that want to define
 * a new type of Content.
 * 
 * @author Benjamin Schmid
 */
public abstract class ContentNode extends BranchGroup {

	/**
	 * Default constructor. Ensures that this ContentNode
	 * is detachable.
	 */
	public ContentNode() {
		super();
		setCapability(BranchGroup.ALLOW_DETACH);
	}

	/**
	 * Returns the volume of this ContentNode.
	 * @return
	 */
	public abstract float getVolume();

	/**
	 * Stores the minimum coordinate of this ContentNode in
	 * the specified Tuple3d
	 * @param min
	 */
	public abstract void getMin(Tuple3d min);

	/**
	 * Stores the maximum coordinate of this ContentNode in
	 * the specified Tuple3d
	 * @param min
	 */
	public abstract void getMax(Tuple3d max);

	/**
	 * Stores the center coordinate of this ContentNode in
	 * the specified Tuple3d
	 * @param min
	 */
	public abstract void getCenter(Tuple3d center);

	/**
	 * This method is called when the channels of the Content
	 * have changed.
	 */
	public abstract void channelsUpdated();

	/**
	 * This method is called when the threshold of the Content
	 * has changed.
	 */
	public abstract void thresholdUpdated();

	/**
	 * This method is called when the color of the Content has changed.
	 */
	public abstract void colorUpdated();

	/**
	 * This method is called when the transparency of the Content
	 * has changed.
	 */
	public abstract void transparencyUpdated();

	/**
	 * This method is called when the 'shade' attribute of the
	 * Content has changed.
	 */
	public abstract void shadeUpdated();

	/**
	 * This method is called when the view direction has changed.
	 * @param view
	 */
	public abstract void eyePtChanged(View view);
}

