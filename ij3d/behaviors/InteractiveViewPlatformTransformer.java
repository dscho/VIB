package ij3d.behaviors;

import ij3d.DefaultUniverse;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

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

	public void rotate(MouseEvent e) {
		int dx = xLast - e.getX();
		int dy = yLast - e.getY();
		Point3f cen = new Point3f();  // ((Image3DUniverse)univ).getGlobalCenterPoint();
		rotateXY(new Point3d(cen), dy * ONE_RAD, dx * ONE_RAD);
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
