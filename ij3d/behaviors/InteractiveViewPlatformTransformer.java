package ij3d.behaviors;

import ij3d.DefaultUniverse;
import ij3d.Image3DUniverse;
import ij3d.UniverseSettings;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.vecmath.Point3d;

/**
 * This class extends ViewPlatformTransformer, to transform mouse events into
 * real world transformations.
 * 
 * @author Benjamin Schmid
 */
public class InteractiveViewPlatformTransformer extends ViewPlatformTransformer {

	private static final double ONE_RAD = 2 * Math.PI / 360;
	private int xLast, yLast;
	private Point3d rotCenter;

	/**
	 * Initializes a new InteractiveViewPlatformTransformer.
	 * @param univ
	 * @param callback
	 */
	public InteractiveViewPlatformTransformer(DefaultUniverse univ, BehaviorCallback callback) {
		super(univ, callback);
		// Set the initial rotation center to whatever is set in UniverseSettings
		if(UniverseSettings.globalRotationCenter == 
				UniverseSettings.ROTATION_AROUND_CENTER)
			rotCenter = ((Image3DUniverse)univ).getGlobalCenterPoint();
		else
			rotCenter = new Point3d();
	}

	/**
	 * Returns a reference to the rotation center.
	 * Attention: Changing the returned point results in unspecified
	 * behavior.
	 */
	public Point3d getRotationCenter() {
		return rotCenter;
	}

	/**
	 * Sets the rotation center to the specified point.
	 * Attention: No copy is made.
	 */
	public void setRotationCenter(Point3d rotCenter) {
		this.rotCenter = rotCenter;
	}

	/**
	 * This method should be called when a new transformation is started
	 * (i.e. when the mouse is pressed before dragging in order to rotate
	 * or translate).
	 * @param e
	 */
	public void init(MouseEvent e) {
		this.xLast = e.getX();
		this.yLast = e.getY();
	}

	/**
	 * This method should be called during the mouse is dragged, if
	 * the mouse event should result in a translation.
	 * @param e
	 */
	public void translate(MouseEvent e) {
		int dx = xLast - e.getX();
		int dy = yLast - e.getY();
		translateXY(dx, dy);
		xLast = e.getX();
		yLast = e.getY();
	}

	/**
	 * This method should be called during the mouse is dragged, if
	 * the mouse event should result in a rotation.
	 * @param e
	 */
	public void rotate(MouseEvent e) {
		int dx = xLast - e.getX();
		int dy = yLast - e.getY();
		rotateXY(rotCenter, dy * ONE_RAD, dx * ONE_RAD);
		xLast = e.getX();
		yLast = e.getY();
	}

	/**
	 * This method should be called, if the specified MouseEvent should
	 * affect zooming based on wheel movement.
	 * @param e
	 */
	public void wheel_zoom(MouseEvent e) {
		MouseWheelEvent we = (MouseWheelEvent)e;
		int units = 0;
		if(we.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
			units = we.getUnitsToScroll();
		zoom(units);
	}

	/**
	 * This method should be called, if the specified MouseEvent should
	 * affect zooming based on vertical mouse dragging.
	 * @param e
	 */
	public void zoom(MouseEvent e) {
		int y = e.getY();
		int dy = y - yLast;
		zoom(dy);
		xLast = e.getX();
		yLast = y;
	}
}
