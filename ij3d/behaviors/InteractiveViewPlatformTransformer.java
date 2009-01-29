package ij3d.behaviors;

import ij3d.DefaultUniverse;
import ij3d.Image3DUniverse;
import ij3d.UniverseSettings;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.vecmath.Point3d;

public class InteractiveViewPlatformTransformer extends ViewPlatformTransformer {

	private static final double ONE_RAD = 2 * Math.PI / 360;
	private int xLast, yLast;

	public InteractiveViewPlatformTransformer(DefaultUniverse univ, BehaviorCallback callback) {
		super(univ, callback);
	}

	public void init(MouseEvent e) {
		this.xLast = e.getX();
		this.yLast = e.getY();
	}

	public void translate(MouseEvent e) {
		int dx = xLast - e.getX();
		int dy = yLast - e.getY();
		translateXY(dx, dy);
		xLast = e.getX();
		yLast = e.getY();
	}

	private final Point3d rotCenter = new Point3d();
	public void rotate(MouseEvent e) {
		int dx = xLast - e.getX();
		int dy = yLast - e.getY();
		if(UniverseSettings.globalRotationCenter == 
				UniverseSettings.ROTATION_AROUND_CENTER)
			((Image3DUniverse)univ).getGlobalCenterPoint(rotCenter);
		else
			rotCenter.set(0, 0, 0);
		rotateXY(rotCenter, dy * ONE_RAD, dx * ONE_RAD);
		xLast = e.getX();
		yLast = e.getY();
	}

	public void wheel_zoom(MouseEvent e) {
		MouseWheelEvent we = (MouseWheelEvent)e;
		int units = 0;
		if(we.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
			units = we.getUnitsToScroll();
		zoom(units);
	}

	public void zoom(MouseEvent e) {
		int y = e.getY();
		int dy = y - yLast;
		zoom(dy);
		xLast = e.getX();
		yLast = y;
	}
}
